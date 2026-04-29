package com.cometjc.floatighrtraining.data.persistence

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "training_plan")
data class TrainingPlanEntity(
    @PrimaryKey(autoGenerate = true) val planId: Long = 0,
    val name: String,
    val repeats: Int,
    val freeTraining: Boolean,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "training_plan_segment",
    foreignKeys = [
        ForeignKey(
            entity = TrainingPlanEntity::class,
            parentColumns = ["planId"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class TrainingPlanSegmentEntity(
    @PrimaryKey(autoGenerate = true) val segmentId: Long = 0,
    val planId: Long = 0,
    val segmentIndex: Int,
    val zoneId: String,
    val zoneLabel: String,
    val targetMinBpm: Int,
    val targetMaxBpm: Int,
    val durationSeconds: Int,
    val note: String? = null
)

data class TrainingPlanRecord(
    @Embedded val plan: TrainingPlanEntity,
    @Relation(parentColumn = "planId", entityColumn = "planId")
    val segments: List<TrainingPlanSegmentEntity>
)
