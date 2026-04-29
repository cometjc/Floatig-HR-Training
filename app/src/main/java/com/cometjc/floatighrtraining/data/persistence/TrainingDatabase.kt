package com.cometjc.floatighrtraining.data.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkoutEntity::class,
        WorkoutSampleEntity::class,
        HeartRateDelayEstimateEntity::class,
        TrainingPlanEntity::class,
        TrainingPlanSegmentEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class TrainingDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun heartRateDelayEstimateDao(): HeartRateDelayEstimateDao
    abstract fun trainingPlanDao(): TrainingPlanDao
}
