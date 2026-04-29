package com.cometjc.floatighrtraining.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HeartRateDelayEstimateDao {

    @Insert
    suspend fun insert(estimate: HeartRateDelayEstimateEntity): Long

    @Query(
        "SELECT * FROM heart_rate_delay_estimate WHERE userId = :userId AND zoneId = :zoneId " +
            "ORDER BY recordedAtEpochMs DESC LIMIT 1"
    )
    suspend fun getLatestForUserZone(userId: String, zoneId: String): HeartRateDelayEstimateEntity?
}
