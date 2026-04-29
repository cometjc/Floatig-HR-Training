package com.cometjc.floatighrtraining.workout

import com.cometjc.floatighrtraining.model.AlertState
import com.cometjc.floatighrtraining.model.HeartRateZone
import com.cometjc.floatighrtraining.model.TrainingPlan
import com.cometjc.floatighrtraining.model.TrainingSegment
import com.cometjc.floatighrtraining.model.defaultZones
import com.cometjc.floatighrtraining.prediction.HeartRateDelayEstimate
import com.cometjc.floatighrtraining.prediction.PacingDecision
import com.cometjc.floatighrtraining.prediction.PacingPrediction
import com.cometjc.floatighrtraining.prediction.PacingPredictionEngine
import com.cometjc.floatighrtraining.prediction.TrainingTelemetrySample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val DEFAULT_BPM = 127
private const val DEFAULT_CADENCE = 166
private const val MAX_SAMPLE_HISTORY = 24

private val DEFAULT_ZONE: HeartRateZone = defaultZones().first { it.id == "Z2" }
private val DEFAULT_SEGMENT = TrainingSegment(zone = DEFAULT_ZONE, durationSeconds = 60)
private fun defaultPrediction(bpm: Int): PacingPrediction {
    return PacingPrediction(
        decision = PacingDecision.Maintain,
        message = "收集中",
        projectedBpm = bpm.toDouble(),
        secondsToUpperBound = null,
        heartRateSlopeBpmPerMinute = 0.0,
        delayEstimate = HeartRateDelayEstimate(seconds = 30, confidence = 0f),
        confidenceScore = 0f
    )
}

data class WorkoutSessionState(
    val isRunning: Boolean = false,
    val connectedDeviceName: String? = null,
    val training: TrainingPlan? = null,
    val bpm: Int = DEFAULT_BPM,
    val cadence: Int = DEFAULT_CADENCE,
    val currentSegmentIndex: Int = 0,
    val totalSegments: Int = 0,
    val currentSegment: TrainingSegment = DEFAULT_SEGMENT,
    val elapsedSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val alertState: AlertState = AlertState.fromBpm(DEFAULT_BPM, DEFAULT_SEGMENT.zone),
    val prediction: PacingPrediction = defaultPrediction(DEFAULT_BPM),
    val samples: List<TrainingTelemetrySample> = emptyList()
)

class WorkoutSessionStore(
    private val predictionEngine: PacingPredictionEngine = PacingPredictionEngine()
) {
    private val _state = MutableStateFlow(WorkoutSessionState())
    val state: StateFlow<WorkoutSessionState> = _state

    fun start(training: TrainingPlan, connectedDeviceName: String? = null) {
        predictionEngine.reset()
        val segments = expandedSegments(training)
        val currentSegment = segments.firstOrNull() ?: DEFAULT_SEGMENT
        val seedSamples = listOf(TrainingTelemetrySample(0, DEFAULT_BPM, DEFAULT_CADENCE))
        val prediction = predictionEngine.predict(
            samples = seedSamples,
            targetMinBpm = currentSegment.zone.minBpm,
            targetMaxBpm = currentSegment.zone.maxBpm
        )
        _state.value = WorkoutSessionState(
            isRunning = true,
            connectedDeviceName = connectedDeviceName,
            training = training,
            currentSegmentIndex = 0,
            totalSegments = segments.size,
            currentSegment = currentSegment,
            elapsedSeconds = 0,
            remainingSeconds = training.totalSeconds,
            alertState = AlertState.fromBpm(DEFAULT_BPM, currentSegment.zone),
            prediction = prediction,
            samples = seedSamples
        )
    }

    fun recordTelemetry(bpm: Int, cadence: Int, elapsedSeconds: Int) {
        val current = _state.value
        val training = current.training ?: return
        val segments = expandedSegments(training)
        val currentSegmentIndex = segmentIndexForElapsed(segments, elapsedSeconds)
        val currentSegment = segments.getOrElse(currentSegmentIndex) { DEFAULT_SEGMENT }
        val updatedSamples = (current.samples + TrainingTelemetrySample(elapsedSeconds.toLong(), bpm, cadence))
            .takeLast(MAX_SAMPLE_HISTORY)
        val prediction = predictionEngine.predict(
            samples = updatedSamples,
            targetMinBpm = currentSegment.zone.minBpm,
            targetMaxBpm = currentSegment.zone.maxBpm
        )

        _state.value = current.copy(
            bpm = bpm,
            cadence = cadence,
            currentSegmentIndex = currentSegmentIndex,
            totalSegments = segments.size,
            currentSegment = currentSegment,
            elapsedSeconds = elapsedSeconds,
            remainingSeconds = (training.totalSeconds - elapsedSeconds).coerceAtLeast(0),
            alertState = AlertState.fromBpm(bpm, currentSegment.zone),
            prediction = prediction,
            samples = updatedSamples
        )
    }

    fun stop() {
        predictionEngine.reset()
        _state.value = WorkoutSessionState()
    }

    private fun expandedSegments(training: TrainingPlan): List<TrainingSegment> {
        return List(training.repeats.coerceAtLeast(1)) { training.segments }.flatten()
    }

    private fun segmentIndexForElapsed(segments: List<TrainingSegment>, elapsedSeconds: Int): Int {
        if (segments.isEmpty()) return 0

        var accumulated = 0
        segments.forEachIndexed { index, segment ->
            accumulated += segment.durationSeconds
            if (elapsedSeconds < accumulated) {
                return index
            }
        }
        return segments.lastIndex
    }

}
