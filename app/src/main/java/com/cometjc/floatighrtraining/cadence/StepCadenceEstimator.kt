package com.cometjc.floatighrtraining.cadence

import kotlin.math.roundToInt

class StepCadenceEstimator(
    private val maxSamples: Int = 8
) {
    private val stepTimestampsMs = ArrayDeque<Long>()

    fun onStep(timestampMs: Long): Int? {
        stepTimestampsMs.addLast(timestampMs)
        while (stepTimestampsMs.size > maxSamples) {
            stepTimestampsMs.removeFirst()
        }
        if (stepTimestampsMs.size < 2) return null

        val elapsedMs = stepTimestampsMs.last() - stepTimestampsMs.first()
        if (elapsedMs <= 0L) return null

        val stepIntervals = stepTimestampsMs.size - 1
        return ((stepIntervals * 60_000.0) / elapsedMs.toDouble()).roundToInt()
    }
}
