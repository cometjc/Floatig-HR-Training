package com.cometjc.floatighrtraining.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class TrainingPlanDao {

    @Transaction
    open suspend fun upsertPlan(record: TrainingPlanRecord): Long {
        val planId = if (record.plan.planId == 0L) {
            insertPlan(record.plan)
        } else {
            updatePlan(record.plan)
            record.plan.planId
        }
        deleteSegmentsForPlan(planId)
        insertSegments(record.segments.map { it.copy(segmentId = 0L, planId = planId) })
        return planId
    }

    suspend fun getPlan(planId: Long): TrainingPlanRecord? =
        getPlanRaw(planId)?.let { it.copy(segments = it.segments.sortedBy(TrainingPlanSegmentEntity::segmentIndex)) }

    fun observePlans(): Flow<List<TrainingPlanRecord>> =
        observePlansRaw().map { plans ->
            plans.map { it.copy(segments = it.segments.sortedBy(TrainingPlanSegmentEntity::segmentIndex)) }
        }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPlan(plan: TrainingPlanEntity): Long

    @Update
    protected abstract suspend fun updatePlan(plan: TrainingPlanEntity)

    @Query("DELETE FROM training_plan_segment WHERE planId = :planId")
    protected abstract suspend fun deleteSegmentsForPlan(planId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSegments(segments: List<TrainingPlanSegmentEntity>)

    @Transaction
    @Query("SELECT * FROM training_plan WHERE planId = :planId LIMIT 1")
    protected abstract suspend fun getPlanRaw(planId: Long): TrainingPlanRecord?

    @Transaction
    @Query("SELECT * FROM training_plan ORDER BY updatedAtEpochMs DESC")
    protected abstract fun observePlansRaw(): Flow<List<TrainingPlanRecord>>
}
