package com.cometjc.floatighrtraining.workout

import com.cometjc.floatighrtraining.model.HeartRateZone
import com.cometjc.floatighrtraining.model.TrainingPlan
import com.cometjc.floatighrtraining.model.TrainingSegment
import com.cometjc.floatighrtraining.model.defaultZones
import com.cometjc.floatighrtraining.prediction.PacingDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionStoreTest {
    private val zones = defaultZones()
    private val z2 = zones.first { it.id == "Z2" }
    private val z3 = zones.first { it.id == "Z3" }

    @Test
    fun startSeedsSharedWorkoutStateFromTrainingPlan() {
        val store = WorkoutSessionStore()
        val training = trainingPlan(
            TrainingSegment(zone = z2, durationSeconds = 60),
            TrainingSegment(zone = z3, durationSeconds = 30)
        )

        store.start(training = training, connectedDeviceName = "Polar H10")

        val state = store.state.value
        assertTrue(state.isRunning)
        assertEquals("Polar H10", state.connectedDeviceName)
        assertEquals(training, state.training)
        assertEquals(0, state.currentSegmentIndex)
        assertEquals(2, state.totalSegments)
        assertEquals(z2, state.currentSegment.zone)
        assertEquals(0, state.elapsedSeconds)
        assertEquals(90, state.remainingSeconds)
        assertEquals(127, state.bpm)
        assertEquals(166, state.cadence)
    }

    @Test
    fun recordTelemetryAdvancesSegmentAndRecomputesPrediction() {
        val store = WorkoutSessionStore()
        val training = trainingPlan(
            TrainingSegment(zone = z2, durationSeconds = 60),
            TrainingSegment(zone = z3, durationSeconds = 30)
        )
        store.start(training = training)

        store.recordTelemetry(bpm = 124, cadence = 170, elapsedSeconds = 70)

        val state = store.state.value
        assertEquals(124, state.bpm)
        assertEquals(170, state.cadence)
        assertEquals(70, state.elapsedSeconds)
        assertEquals(20, state.remainingSeconds)
        assertEquals(1, state.currentSegmentIndex)
        assertEquals(z3, state.currentSegment.zone)
        assertEquals(PacingDecision.Maintain, state.prediction.decision)
        assertFalse(state.samples.isEmpty())
    }

    private fun trainingPlan(vararg segments: TrainingSegment): TrainingPlan {
        return TrainingPlan(
            name = "Intervals",
            repeats = 1,
            segments = segments.toList()
        )
    }
}
