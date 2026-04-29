package com.cometjc.floatighrtraining.ble

import com.cometjc.floatighrtraining.model.DiscoveredHeartRateDevice
import com.cometjc.floatighrtraining.model.TrainingPlan
import com.cometjc.floatighrtraining.model.sampleTrainingPlan
import com.cometjc.floatighrtraining.workout.WorkoutSessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeartRateBleControllerTest {
    @Test
    fun connectSelectedDevice_startsScanAndConnection() = runTest {
        val monitor = FakeHeartRateBleMonitor()
        val coordinator = FakeWorkoutSessionCoordinator()
        val controller = HeartRateBleController(
            monitor = monitor,
            workoutSessionCoordinator = coordinator,
            scope = backgroundScope,
            tickerDelayMillis = 50L
        )
        val device = DiscoveredHeartRateDevice(
            address = "AA:BB:CC:DD:EE:FF",
            name = "Polar H10 12345678",
            rssi = -44
        )

        monitor.devicesFlow.value = listOf(device)
        controller.startScan()
        controller.selectDevice(device.address)
        controller.connectSelectedDevice()
        advanceUntilIdle()

        assertTrue(controller.uiState.value.isScanning)
        assertTrue(controller.uiState.value.isConnecting)
        assertEquals(listOf(device.address), monitor.connectRequests)
        controller.close()
    }

    @Test
    fun startWorkout_recordsLiveHeartRateSamples() = runTest {
        val monitor = FakeHeartRateBleMonitor()
        val coordinator = FakeWorkoutSessionCoordinator()
        val controller = HeartRateBleController(
            monitor = monitor,
            workoutSessionCoordinator = coordinator,
            scope = backgroundScope,
            tickerDelayMillis = 50L
        )
        val training = sampleTrainingPlan()

        monitor.connectedDeviceNameFlow.value = "Polar H10 12345678"
        monitor.heartRateFlow.value = 148
        advanceUntilIdle()

        controller.startWorkout(training)
        advanceTimeBy(60L)
        advanceUntilIdle()

        assertEquals("Polar H10 12345678", coordinator.startedWithDeviceName)
        assertEquals(148, coordinator.state.value.bpm)
        assertEquals(1, coordinator.state.value.elapsedSeconds)
        assertEquals(1, coordinator.recordedTelemetry.size)
        controller.close()
    }
}

private class FakeHeartRateBleMonitor : HeartRateBleMonitor {
    val devicesFlow = MutableStateFlow<List<DiscoveredHeartRateDevice>>(emptyList())
    val heartRateFlow = MutableStateFlow(0)
    val connectedDeviceNameFlow = MutableStateFlow<String?>(null)
    val connectRequests = mutableListOf<String>()

    override val devices: StateFlow<List<DiscoveredHeartRateDevice>> = devicesFlow
    override val heartRate: StateFlow<Int> = heartRateFlow
    override val connectedDeviceName: StateFlow<String?> = connectedDeviceNameFlow

    override fun startScan() = Unit

    override fun stopScan() = Unit

    override fun connect(address: String) {
        connectRequests += address
    }

    override fun disconnect() = Unit

    override fun close() = Unit
}

private class FakeWorkoutSessionCoordinator : WorkoutSessionCoordinator {
    private val stateFlow = MutableStateFlow(WorkoutSessionState())
    val recordedTelemetry = mutableListOf<Triple<Int, Int, Int>>()
    var startedWithDeviceName: String? = null

    override val state: StateFlow<WorkoutSessionState> = stateFlow

    override fun startSession(training: TrainingPlan, connectedDeviceName: String?) {
        startedWithDeviceName = connectedDeviceName
        stateFlow.value = WorkoutSessionState(
            isRunning = true,
            training = training,
            connectedDeviceName = connectedDeviceName
        )
    }

    override fun recordTelemetry(bpm: Int, cadence: Int, elapsedSeconds: Int) {
        recordedTelemetry += Triple(bpm, cadence, elapsedSeconds)
        stateFlow.value = stateFlow.value.copy(
            bpm = bpm,
            cadence = cadence,
            elapsedSeconds = elapsedSeconds
        )
    }

    override fun stopSession() {
        stateFlow.value = WorkoutSessionState()
    }
}
