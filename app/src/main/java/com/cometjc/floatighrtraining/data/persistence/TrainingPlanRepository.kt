package com.cometjc.floatighrtraining.data.persistence

import com.cometjc.floatighrtraining.model.HeartRateZone
import com.cometjc.floatighrtraining.model.TrainingPlan
import com.cometjc.floatighrtraining.model.TrainingSegment
import com.cometjc.floatighrtraining.model.defaultZones
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrainingPlanRepository(
    private val dao: TrainingPlanDao
) {
    val plans: Flow<List<PersistedTrainingPlan>> = dao.observePlans().map { records ->
        records.map { record ->
            PersistedTrainingPlan(
                id = record.plan.planId,
                plan = record.toDomain()
            )
        }
    }

    suspend fun upsert(id: Long?, plan: TrainingPlan): Long {
        val record = TrainingPlanRecord(
            plan = TrainingPlanEntity(
                planId = id ?: 0L,
                name = plan.name,
                repeats = plan.repeats,
                freeTraining = plan.freeTraining,
                updatedAtEpochMs = System.currentTimeMillis()
            ),
            segments = plan.segments.mapIndexed { index, segment ->
                TrainingPlanSegmentEntity(
                    segmentIndex = index,
                    zoneId = segment.zone.id,
                    zoneLabel = segment.zone.label,
                    targetMinBpm = segment.zone.minBpm,
                    targetMaxBpm = segment.zone.maxBpm,
                    durationSeconds = segment.durationSeconds,
                    note = segment.note.takeIf { it.isNotBlank() }
                )
            }
        )
        return dao.upsertPlan(record)
    }
}

data class PersistedTrainingPlan(
    val id: Long,
    val plan: TrainingPlan
)

private fun TrainingPlanRecord.toDomain(): TrainingPlan {
    val zonesById = defaultZones().associateBy(HeartRateZone::id)
    return TrainingPlan(
        name = plan.name,
        repeats = plan.repeats.coerceAtLeast(1),
        freeTraining = plan.freeTraining,
        segments = segments.sortedBy(TrainingPlanSegmentEntity::segmentIndex).map { segment ->
            val zone = zonesById[segment.zoneId] ?: HeartRateZone(
                id = segment.zoneId,
                label = segment.zoneLabel,
                minBpm = segment.targetMinBpm,
                maxBpm = segment.targetMaxBpm,
                color = defaultZones().first().color
            )
            TrainingSegment(
                zone = zone,
                durationSeconds = segment.durationSeconds.coerceAtLeast(1),
                note = segment.note.orEmpty()
            )
        }
    )
}
