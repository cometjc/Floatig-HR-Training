package com.cometjc.floatighrtraining.ble

import com.cometjc.floatighrtraining.model.DiscoveredHeartRateDevice
import kotlinx.coroutines.flow.StateFlow

interface HeartRateBleMonitor {
    val devices: StateFlow<List<DiscoveredHeartRateDevice>>
    val heartRate: StateFlow<Int>
    val connectedDeviceName: StateFlow<String?>

    fun startScan()
    fun stopScan()
    fun connect(address: String)
    fun disconnect()
    fun close()
}
