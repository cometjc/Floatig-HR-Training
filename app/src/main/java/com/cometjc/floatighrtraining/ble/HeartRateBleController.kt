package com.cometjc.floatighrtraining.ble

import android.content.Context
import com.cometjc.floatighrtraining.cadence.AndroidStepCadenceMonitor
import com.cometjc.floatighrtraining.cadence.CadenceMonitor
import com.cometjc.floatighrtraining.cadence.NoopCadenceMonitor
import com.cometjc.floatighrtraining.model.DiscoveredHeartRateDevice
import com.cometjc.floatighrtraining.telemetry.SentryTelemetry
import com.cometjc.floatighrtraining.model.TrainingPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HeartRateBleUiState(
    val devices: List<DiscoveredHeartRateDevice> = emptyList(),
    val selectedDeviceAddress: String? = null,
    val connectedDeviceName: String? = null,
    val heartRate: Int = 0,
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false
)

class HeartRateBleController(
    private val monitor: HeartRateBleMonitor,
    private val cadenceMonitor: CadenceMonitor = NoopCadenceMonitor,
    private val workoutSessionCoordinator: WorkoutSessionCoordinator = ForegroundWorkoutSessionCoordinator,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val tickerDelayMillis: Long = 1_000L
) {
    private val selectedDeviceAddress = MutableStateFlow<String?>(null)
    private val isScanning = MutableStateFlow(false)
    private val isConnecting = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(buildUiState())
    val uiState: StateFlow<HeartRateBleUiState> = _uiState.asStateFlow()

    private var workoutTickerJob: Job? = null

    init {
        scope.launch {
            monitor.devices.collect { publishUiState() }
        }
        scope.launch {
            monitor.heartRate.collect { publishUiState() }
        }
        scope.launch {
            cadenceMonitor.cadenceSpm.collect { publishUiState() }
        }
        scope.launch {
            monitor.connectedDeviceName.collect { connectedDeviceName ->
                if (connectedDeviceName != null) {
                    isConnecting.value = false
                    isScanning.value = false
                }
                publishUiState()
            }
        }
        scope.launch { selectedDeviceAddress.collect { publishUiState() } }
        scope.launch { isScanning.collect { publishUiState() } }
        scope.launch { isConnecting.collect { publishUiState() } }
    }

    fun startScan() {
        SentryTelemetry.instance.runBleOperationSpan("ble.ui.scan") {
            isScanning.value = true
            monitor.startScan()
            publishUiState()
        }
    }

    fun stopScan() {
        SentryTelemetry.instance.runBleOperationSpan("ble.ui.scan_stop") {
            isScanning.value = false
            monitor.stopScan()
            publishUiState()
        }
    }

    fun selectDevice(address: String) {
        selectedDeviceAddress.value = address
        publishUiState()
    }

    fun connectSelectedDevice() {
        val address = selectedDeviceAddress.value ?: return
        SentryTelemetry.instance.runBleOperationSpan("ble.ui.connect") {
            isConnecting.value = true
            monitor.connect(address)
            publishUiState()
        }
    }

    fun disconnect() {
        SentryTelemetry.instance.runBleOperationSpan("ble.ui.disconnect") {
            isConnecting.value = false
            monitor.disconnect()
            publishUiState()
        }
    }

    fun startWorkout(training: TrainingPlan) {
        SentryTelemetry.instance.runWorkoutLifecycleSpan("workout.start") {
            cadenceMonitor.startTracking()
            workoutSessionCoordinator.startSession(training, monitor.connectedDeviceName.value)
            resumeWorkout()
        }
    }

    fun pauseWorkout() {
        workoutTickerJob?.cancel()
        workoutTickerJob = null
        publishUiState()
    }

    fun resumeWorkout() {
        if (workoutTickerJob?.isActive == true) return
        workoutTickerJob = scope.launch {
            while (true) {
                delay(tickerDelayMillis)
                val current = workoutSessionCoordinator.state.value
                if (!current.isRunning) break

                val bpm = uiState.value.heartRate.takeIf { it > 0 } ?: current.bpm
                val cadence = cadenceMonitor.cadenceSpm.value ?: current.cadence
                workoutSessionCoordinator.recordTelemetry(
                    bpm = bpm,
                    cadence = cadence,
                    elapsedSeconds = current.elapsedSeconds + 1
                )
            }
        }
    }

    fun stopWorkout() {
        SentryTelemetry.instance.runWorkoutLifecycleSpan("workout.stop") {
            pauseWorkout()
            cadenceMonitor.stopTracking()
            workoutSessionCoordinator.stopSession()
            publishUiState()
        }
    }

    fun close() {
        pauseWorkout()
        cadenceMonitor.close()
        monitor.close()
        scope.cancel()
    }

    private fun publishUiState() {
        _uiState.value = buildUiState()
    }

    private fun buildUiState(): HeartRateBleUiState {
        return HeartRateBleUiState(
            devices = monitor.devices.value,
            selectedDeviceAddress = selectedDeviceAddress.value,
            connectedDeviceName = monitor.connectedDeviceName.value,
            heartRate = monitor.heartRate.value,
            isScanning = isScanning.value,
            isConnecting = isConnecting.value
        )
    }

    companion object {
        @Volatile
        private var instance: HeartRateBleController? = null

        fun getInstance(context: Context): HeartRateBleController {
            return instance ?: synchronized(this) {
                instance ?: HeartRateBleController(
                    monitor = HeartRateBleClient(context.applicationContext),
                    cadenceMonitor = AndroidStepCadenceMonitor(context.applicationContext)
                ).also {
                    instance = it
                }
            }
        }
    }
}
