package com.cometjc.floatighrtraining.data.persistence

import com.cometjc.floatighrtraining.prediction.HeartRateDelayEstimate

class HeartRateDelayEstimateRepository(
    private val dao: HeartRateDelayEstimateDao
) {
    suspend fun latestFor(userId: String, zoneId: String): HeartRateDelayEstimateEntity? {
        return dao.getLatestForUserZone(userId = userId, zoneId = zoneId)
    }

    suspend fun save(
        userId: String,
        zoneId: String,
        delayEstimate: HeartRateDelayEstimate,
        sampleCount: Int
    ): Long {
        return dao.insert(
            HeartRateDelayEstimateEntity(
                userId = userId,
                zoneId = zoneId,
                delaySeconds = delayEstimate.seconds,
                confidence = delayEstimate.confidence,
                sampleCount = sampleCount,
                recordedAtEpochMs = System.currentTimeMillis()
            )
        )
    }
}
