package com.cometjc.floatighrtraining.prediction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacingPredictionEngineTest {
    private fun engine() = PacingPredictionEngine()

    @Test
    fun predictsSlowDownBeforeHeartRateCrossesUpperBound() {
        val samples = listOf(
            TrainingTelemetrySample(elapsedSeconds = 0, heartRateBpm = 120, cadenceSpm = 166),
            TrainingTelemetrySample(elapsedSeconds = 15, heartRateBpm = 122, cadenceSpm = 168),
            TrainingTelemetrySample(elapsedSeconds = 30, heartRateBpm = 124, cadenceSpm = 170),
            TrainingTelemetrySample(elapsedSeconds = 45, heartRateBpm = 126, cadenceSpm = 174)
        )

        val prediction = engine().predict(samples, targetMinBpm = 110, targetMaxBpm = 128)

        assertEquals(PacingDecision.SlowDownSoon, prediction.decision)
        assertTrue(prediction.secondsToUpperBound in 14..16)
        assertTrue(prediction.heartRateSlopeBpmPerMinute > 7.0)
    }

    @Test
    fun keepsRunnerSteadyWhenTrendStaysInsideZone() {
        val samples = listOf(
            TrainingTelemetrySample(elapsedSeconds = 0, heartRateBpm = 121, cadenceSpm = 164),
            TrainingTelemetrySample(elapsedSeconds = 15, heartRateBpm = 121, cadenceSpm = 164),
            TrainingTelemetrySample(elapsedSeconds = 30, heartRateBpm = 122, cadenceSpm = 164),
            TrainingTelemetrySample(elapsedSeconds = 45, heartRateBpm = 122, cadenceSpm = 165)
        )

        val prediction = engine().predict(samples, targetMinBpm = 110, targetMaxBpm = 128)

        assertEquals(PacingDecision.Maintain, prediction.decision)
        assertTrue((prediction.secondsToUpperBound ?: 999) > 45)
    }

    @Test
    fun asksToSpeedUpWhenBelowTargetAndNotRisingFastEnough() {
        val samples = listOf(
            TrainingTelemetrySample(elapsedSeconds = 0, heartRateBpm = 101, cadenceSpm = 158),
            TrainingTelemetrySample(elapsedSeconds = 15, heartRateBpm = 101, cadenceSpm = 158),
            TrainingTelemetrySample(elapsedSeconds = 30, heartRateBpm = 102, cadenceSpm = 158),
            TrainingTelemetrySample(elapsedSeconds = 45, heartRateBpm = 102, cadenceSpm = 159)
        )

        val prediction = engine().predict(samples, targetMinBpm = 110, targetMaxBpm = 128)

        assertEquals(PacingDecision.SpeedUp, prediction.decision)
    }

    @Test
    fun estimatesLagFromCadenceChangeToHeartRateResponse() {
        val history = listOf(
            TrainingTelemetrySample(elapsedSeconds = 0, heartRateBpm = 118, cadenceSpm = 160),
            TrainingTelemetrySample(elapsedSeconds = 10, heartRateBpm = 118, cadenceSpm = 160),
            TrainingTelemetrySample(elapsedSeconds = 20, heartRateBpm = 118, cadenceSpm = 172),
            TrainingTelemetrySample(elapsedSeconds = 30, heartRateBpm = 119, cadenceSpm = 172),
            TrainingTelemetrySample(elapsedSeconds = 40, heartRateBpm = 121, cadenceSpm = 173),
            TrainingTelemetrySample(elapsedSeconds = 50, heartRateBpm = 124, cadenceSpm = 173),
            TrainingTelemetrySample(elapsedSeconds = 60, heartRateBpm = 126, cadenceSpm = 173),
            TrainingTelemetrySample(elapsedSeconds = 70, heartRateBpm = 127, cadenceSpm = 172)
        )

        assertTrue(engine().estimateDelay(history).seconds in 10..90)
    }

    @Test
    fun triggersSlowDownSoonOnCadenceLeadSignalNearUpperBound() {
        val prediction = engine().predict(
            samples = listOf(
                TrainingTelemetrySample(elapsedSeconds = 0, heartRateBpm = 122, cadenceSpm = 158),
                TrainingTelemetrySample(elapsedSeconds = 10, heartRateBpm = 123, cadenceSpm = 161),
                TrainingTelemetrySample(elapsedSeconds = 20, heartRateBpm = 123, cadenceSpm = 165),
                TrainingTelemetrySample(elapsedSeconds = 30, heartRateBpm = 124, cadenceSpm = 168)
            ),
            targetMinBpm = 110,
            targetMaxBpm = 128
        )

        assertEquals(PacingDecision.SlowDownSoon, prediction.decision)
        assertTrue(prediction.confidenceScore > 0f)
    }

    @Test
    fun keepsSlowDownSoonDuringCooldownToPreventAlertSpam() {
        val engine = engine()
        val first = engine.predict(
            samples = listOf(
                TrainingTelemetrySample(elapsedSeconds = 0, heartRateBpm = 124, cadenceSpm = 165),
                TrainingTelemetrySample(elapsedSeconds = 15, heartRateBpm = 126, cadenceSpm = 167),
                TrainingTelemetrySample(elapsedSeconds = 30, heartRateBpm = 127, cadenceSpm = 170),
                TrainingTelemetrySample(elapsedSeconds = 45, heartRateBpm = 127, cadenceSpm = 172)
            ),
            targetMinBpm = 110,
            targetMaxBpm = 128
        )
        val second = engine.predict(
            samples = listOf(
                TrainingTelemetrySample(elapsedSeconds = 5, heartRateBpm = 121, cadenceSpm = 161),
                TrainingTelemetrySample(elapsedSeconds = 20, heartRateBpm = 122, cadenceSpm = 162),
                TrainingTelemetrySample(elapsedSeconds = 35, heartRateBpm = 122, cadenceSpm = 163),
                TrainingTelemetrySample(elapsedSeconds = 50, heartRateBpm = 123, cadenceSpm = 163)
            ),
            targetMinBpm = 110,
            targetMaxBpm = 128
        )

        assertEquals(PacingDecision.SlowDownSoon, first.decision)
        assertEquals(PacingDecision.SlowDownSoon, second.decision)
    }
}
