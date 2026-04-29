package com.cometjc.floatighrtraining.ble

import android.content.Context
import com.cometjc.floatighrtraining.model.DiscoveredHeartRateDevice
import com.polar.androidcommunications.api.ble.model.DisInfo
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHealthThermometerData
import com.polar.sdk.api.model.PolarHrData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.cometjc.floatighrtraining.telemetry.SentryTelemetry
import java.util.UUID

class HeartRateBleClient(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : HeartRateBleMonitor {
    private val api = PolarBleApiDefaultImpl.defaultImplementation(
        context.applicationContext,
        setOf(
            PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
            PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
            PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO
        )
    )
    private var scanJob: Job? = null
    private var connectedIdentifier: String? = null

    private val _devices = MutableStateFlow<List<DiscoveredHeartRateDevice>>(emptyList())
    override val devices: StateFlow<List<DiscoveredHeartRateDevice>> = _devices.asStateFlow()

    private val _heartRate = MutableStateFlow(0)
    override val heartRate: StateFlow<Int> = _heartRate.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    override val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    init {
        api.setPolarFilter(true)
        api.setAutomaticReconnection(true)
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) = Unit
            override fun disInformationReceived(identifier: String, disInfo: DisInfo) = Unit
            override fun htsNotificationReceived(
                identifier: String,
                data: PolarHealthThermometerData
            ) = Unit

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                connectedIdentifier = deviceIdentifier(polarDeviceInfo)
                _connectedDeviceName.value = displayName(polarDeviceInfo)
                upsertDevice(polarDeviceInfo)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                if (deviceIdentifier(polarDeviceInfo) == connectedIdentifier) {
                    connectedIdentifier = null
                    _connectedDeviceName.value = null
                    _heartRate.value = 0
                }
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData.PolarHrSample) {
                if (connectedIdentifier == null || connectedIdentifier == identifier) {
                    _heartRate.value = data.hr
                }
            }
        })
    }

    override fun startScan() {
        if (scanJob?.isActive == true) return
        SentryTelemetry.instance.bleScanStarted()
        scanJob = scope.launch {
            try {
                api.searchForDevice("Polar H10").collect { deviceInfo ->
                    upsertDevice(deviceInfo)
                }
            } catch (e: Throwable) {
                SentryTelemetry.instance.captureBleError(e, "ble.scan")
            }
        }
    }

    override fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        SentryTelemetry.instance.bleScanStopped()
    }

    override fun connect(address: String) {
        SentryTelemetry.instance.bleConnectAttempt()
        try {
            stopScan()
            connectedIdentifier = address
            api.connectToDevice(address)
        } catch (e: Throwable) {
            SentryTelemetry.instance.captureBleError(e, "ble.connect")
        }
    }

    override fun disconnect() {
        try {
            connectedIdentifier?.let(api::disconnectFromDevice)
        } catch (e: Throwable) {
            SentryTelemetry.instance.captureBleError(e, "ble.disconnect")
        } finally {
            connectedIdentifier = null
            _connectedDeviceName.value = null
            _heartRate.value = 0
            SentryTelemetry.instance.bleDisconnected()
        }
    }

    override fun close() {
        stopScan()
        api.shutDown()
        scope.cancel()
    }

    private fun upsertDevice(deviceInfo: PolarDeviceInfo) {
        val item = DiscoveredHeartRateDevice(
            address = deviceInfo.address,
            name = displayName(deviceInfo),
            rssi = deviceInfo.rssi
        )
        _devices.value = (_devices.value.filterNot { it.address == item.address } + item)
            .sortedByDescending { it.rssi }
    }

    private fun deviceIdentifier(deviceInfo: PolarDeviceInfo): String {
        return deviceInfo.address.ifBlank { deviceInfo.deviceId }
    }

    private fun displayName(deviceInfo: PolarDeviceInfo): String {
        return deviceInfo.name.ifBlank { "Polar H10 ${deviceInfo.deviceId}" }
    }
}
