package com.cometjc.floatighrtraining.data.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heart_rate_delay_estimate")
data class HeartRateDelayEstimateEntity(
    @PrimaryKey(autoGenerate = true) val estimateId: Long = 0,
    val zoneId: String,
    val delaySeconds: Int,
    val confidence: Float,
    val sampleCount: Int,
    val recordedAtEpochMs: Long
)
