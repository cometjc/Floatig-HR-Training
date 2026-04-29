package com.cometjc.floatighrtraining.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class WorkoutDao {

    @Insert
    abstract suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSamples(samples: List<WorkoutSampleEntity>)

    @Transaction
    @Query("SELECT * FROM workout WHERE workoutId = :workoutId LIMIT 1")
    protected abstract suspend fun getWorkoutWithSamplesRaw(workoutId: Long): WorkoutWithSamples?

    suspend fun getWorkoutWithSamples(workoutId: Long): WorkoutWithSamples? =
        getWorkoutWithSamplesRaw(workoutId)?.let {
            it.copy(samples = it.samples.sortedBy(WorkoutSampleEntity::elapsedSeconds))
        }
}
