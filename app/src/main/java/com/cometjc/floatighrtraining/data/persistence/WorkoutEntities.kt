package com.cometjc.floatighrtraining.data.persistence

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "workout")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "workoutId") val workoutId: Long = 0,
    val planId: Long?,
    val planName: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val averageHeartRateBpm: Int,
    val averageCadenceSpm: Int,
    val connectedDeviceName: String
)

@Entity(
    tableName = "workout_sample",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["workoutId"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId")]
)
data class WorkoutSampleEntity(
    @PrimaryKey(autoGenerate = true) val sampleId: Long = 0,
    val workoutId: Long,
    val elapsedSeconds: Int,
    val heartRateBpm: Int,
    val cadenceSpm: Int,
    val zoneId: String
)

data class WorkoutWithSamples(
    @Embedded val workout: WorkoutEntity,
    @Relation(parentColumn = "workoutId", entityColumn = "workoutId")
    val samples: List<WorkoutSampleEntity>
)
