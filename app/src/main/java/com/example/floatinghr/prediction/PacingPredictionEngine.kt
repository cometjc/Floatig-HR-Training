package com.example.floatinghr.prediction

import kotlin.math.abs
import kotlin.math.sqrt

data class TrainingTelemetrySample(
    val elapsedSeconds: Long,
    val heartRateBpm: Int,
    val cadenceSpm: Int? = null
)

enum class PacingDecision {
    SpeedUp,
    Maintain,
    SlowDownSoon,
    SlowDownNow
}

data class HeartRateDelayEstimate(
    val seconds: Int,
    val confidence: Float
)

data class PacingPrediction(
    val decision: PacingDecision,
    val message: String,
    val projectedBpm: Double,
    val secondsToUpperBound: Int?,
    val heartRateSlopeBpmPerMinute: Double,
    val delayEstimate: HeartRateDelayEstimate
)

data class PacingPredictionConfig(
    val analysisWindowSeconds: Int = 45,
    val fallbackDelaySeconds: Int = 30,
    val minSamples: Int = 4,
    val upperSafetyMarginBpm: Int = 2,
    val minActionableSlopeBpmPerMinute: Double = 1.0
)

class PacingPredictionEngine(
    private val config: PacingPredictionConfig = PacingPredictionConfig()
) {
    fun predict(
        samples: List<TrainingTelemetrySample>,
        targetMinBpm: Int,
        targetMaxBpm: Int
    ): PacingPrediction {
        val orderedSamples = samples.sortedBy { it.elapsedSeconds }
        val latest = orderedSamples.lastOrNull()
        val delay = estimateDelay(orderedSamples)
        if (latest == null || orderedSamples.size < config.minSamples) {
            return PacingPrediction(
                decision = PacingDecision.Maintain,
                message = "收集中",
                projectedBpm = latest?.heartRateBpm?.toDouble() ?: 0.0,
                secondsToUpperBound = null,
                heartRateSlopeBpmPerMinute = 0.0,
                delayEstimate = delay
            )
        }

        val windowStart = latest.elapsedSeconds - config.analysisWindowSeconds
        val window = orderedSamples.filter { it.elapsedSeconds >= windowStart }
        val slopePerSecond = heartRateSlopePerSecond(window)
        val slopePerMinute = slopePerSecond * 60.0
        val projectedBpm = latest.heartRateBpm + slopePerSecond * delay.seconds
        val secondsToUpperBound = if (slopePerSecond > 0) {
            ((targetMaxBpm - latest.heartRateBpm) / slopePerSecond).toInt().coerceAtLeast(0)
        } else {
            null
        }

        val decision = when {
            latest.heartRateBpm >= targetMaxBpm -> PacingDecision.SlowDownNow
            projectedBpm >= targetMaxBpm - config.upperSafetyMarginBpm &&
                slopePerMinute >= config.minActionableSlopeBpmPerMinute -> PacingDecision.SlowDownSoon
            latest.heartRateBpm < targetMinBpm && slopePerMinute <= 3.0 -> PacingDecision.SpeedUp
            else -> PacingDecision.Maintain
        }

        return PacingPrediction(
            decision = decision,
            message = decision.toMessage(secondsToUpperBound),
            projectedBpm = projectedBpm,
            secondsToUpperBound = secondsToUpperBound,
            heartRateSlopeBpmPerMinute = slopePerMinute,
            delayEstimate = delay
        )
    }

    fun estimateDelay(samples: List<TrainingTelemetrySample>): HeartRateDelayEstimate {
        val usable = samples.sortedBy { it.elapsedSeconds }.filter { it.cadenceSpm != null }
        if (usable.size < 8) {
            return HeartRateDelayEstimate(config.fallbackDelaySeconds, confidence = 0f)
        }

        val lagCandidates = listOf(10, 15, 20, 30, 45, 60, 75, 90)
        val scored = lagCandidates.mapNotNull { lag ->
            val pairs = usable.mapNotNull { current ->
                val previousCadence = nearestAtOrBefore(usable, current.elapsedSeconds - lag.toLong())
                val previousHeartRate = nearestAtOrBefore(usable, current.elapsedSeconds - 10)
                if (previousCadence == null || previousHeartRate == null) return@mapNotNull null
                val cadenceDelta = current.cadenceSpm!! - previousCadence.cadenceSpm!!
                val heartRateDelta = current.heartRateBpm - previousHeartRate.heartRateBpm
                cadenceDelta.toDouble() to heartRateDelta.toDouble()
            }
            if (pairs.size < 5) null else lag to abs(correlation(pairs))
        }

        val best = scored.maxByOrNull { it.second }
            ?: return HeartRateDelayEstimate(config.fallbackDelaySeconds, confidence = 0f)
        val confidence = best.second.coerceIn(0.05, 1.0).toFloat()
        return HeartRateDelayEstimate(best.first, confidence)
    }

    private fun heartRateSlopePerSecond(samples: List<TrainingTelemetrySample>): Double {
        if (samples.size < 2) return 0.0
        val firstTime = samples.first().elapsedSeconds
        val points = samples.map { (it.elapsedSeconds - firstTime).toDouble() to it.heartRateBpm.toDouble() }
        val meanX = points.sumOf { it.first } / points.size
        val meanY = points.sumOf { it.second } / points.size
        val denominator = points.sumOf { (it.first - meanX) * (it.first - meanX) }
        if (denominator == 0.0) return 0.0
        return points.sumOf { (it.first - meanX) * (it.second - meanY) } / denominator
    }

    private fun nearestAtOrBefore(
        samples: List<TrainingTelemetrySample>,
        targetSecond: Long
    ): TrainingTelemetrySample? {
        return samples.lastOrNull { it.elapsedSeconds <= targetSecond }
    }

    private fun correlation(pairs: List<Pair<Double, Double>>): Double {
        val meanX = pairs.sumOf { it.first } / pairs.size
        val meanY = pairs.sumOf { it.second } / pairs.size
        val covariance = pairs.sumOf { (it.first - meanX) * (it.second - meanY) }
        val varianceX = pairs.sumOf { (it.first - meanX) * (it.first - meanX) }
        val varianceY = pairs.sumOf { (it.second - meanY) * (it.second - meanY) }
        val denominator = sqrt(varianceX * varianceY)
        return if (denominator == 0.0) 0.0 else covariance / denominator
    }

    private fun PacingDecision.toMessage(secondsToUpperBound: Int?): String = when (this) {
        PacingDecision.SpeedUp -> "加快步頻"
        PacingDecision.Maintain -> "維持節奏"
        PacingDecision.SlowDownSoon -> secondsToUpperBound
            ?.let { "預測 ${it}s 後接近上限，現在放慢" }
            ?: "即將接近上限，現在放慢"
        PacingDecision.SlowDownNow -> "已到上限，立即放慢"
    }
}
