package com.cometjc.floatighrtraining.ble

import com.cometjc.floatighrtraining.model.TrainingPlan
import com.cometjc.floatighrtraining.service.HeartRateForegroundService
import com.cometjc.floatighrtraining.workout.WorkoutSessionState
import kotlinx.coroutines.flow.StateFlow

interface WorkoutSessionCoordinator {
    val state: StateFlow<WorkoutSessionState>

    fun startSession(training: TrainingPlan, connectedDeviceName: String? = null)
    fun recordTelemetry(bpm: Int, cadence: Int, elapsedSeconds: Int)
    fun stopSession()
}

object ForegroundWorkoutSessionCoordinator : WorkoutSessionCoordinator {
    override val state: StateFlow<WorkoutSessionState> = HeartRateForegroundService.workoutSessionState

    override fun startSession(training: TrainingPlan, connectedDeviceName: String?) {
        HeartRateForegroundService.startSession(training, connectedDeviceName)
    }

    override fun recordTelemetry(bpm: Int, cadence: Int, elapsedSeconds: Int) {
        HeartRateForegroundService.recordTelemetry(bpm, cadence, elapsedSeconds)
    }

    override fun stopSession() {
        HeartRateForegroundService.stopSession()
    }
}
