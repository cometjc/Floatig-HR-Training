package com.cometjc.floatighrtraining.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import com.cometjc.floatighrtraining.model.DiscoveredHeartRateDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class HeartRateBleClient(private val context: Context) {
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner get() = adapter?.bluetoothLeScanner
    private var gatt: BluetoothGatt? = null

    private val _devices = MutableStateFlow<List<DiscoveredHeartRateDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredHeartRateDevice>> = _devices

    private val _heartRate = MutableStateFlow(0)
    val heartRate: StateFlow<Int> = _heartRate

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = if (hasConnectPermission()) device.getName() else null
            val item = DiscoveredHeartRateDevice(
                address = device.address,
                name = name?.takeIf { it.isNotBlank() } ?: "Heart Rate Sensor",
                rssi = result.rssi
            )
            _devices.value = (_devices.value.filterNot { it.address == item.address } + item)
                .sortedByDescending { it.rssi }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectedDeviceName.value = if (hasConnectPermission()) gatt.device.getName() else null
                if (hasConnectPermission()) gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectedDeviceName.value = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(HEART_RATE_SERVICE_UUID) ?: return
            val characteristic = service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID) ?: return
            if (!hasConnectPermission()) return
            gatt.setCharacteristicNotification(characteristic, true)
            characteristic.getDescriptor(CLIENT_CONFIG_UUID)?.let { descriptor ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                _heartRate.value = parseHeartRate(characteristic.value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                _heartRate.value = parseHeartRate(value)
            }
        }
    }

    fun startScan() {
        if (!hasScanPermission()) return
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    fun stopScan() {
        if (hasScanPermission()) scanner?.stopScan(scanCallback)
    }

    fun connect(address: String) {
        if (!hasConnectPermission()) return
        stopScan()
        val device: BluetoothDevice = adapter?.getRemoteDevice(address) ?: return
        gatt?.close()
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        if (hasConnectPermission()) {
            gatt?.disconnect()
            gatt?.close()
        }
        gatt = null
        _connectedDeviceName.value = null
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
        val HEART_RATE_MEASUREMENT_UUID: UUID =
            UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
        private val CLIENT_CONFIG_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

        fun findHeartRateService(services: List<BluetoothGattService>): BluetoothGattService? {
            return services.firstOrNull { it.uuid == HEART_RATE_SERVICE_UUID }
        }

        fun parseHeartRate(value: ByteArray): Int {
            if (value.size < 2) return 0
            val is16Bit = value[0].toInt() and 0x01 == 0x01
            return if (is16Bit && value.size >= 3) {
                (value[1].toInt() and 0xFF) or ((value[2].toInt() and 0xFF) shl 8)
            } else {
                value[1].toInt() and 0xFF
            }
        }
    }
}
