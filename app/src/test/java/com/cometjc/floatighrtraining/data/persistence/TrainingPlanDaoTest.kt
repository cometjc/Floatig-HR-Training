package com.cometjc.floatighrtraining.data.persistence

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainingPlanDaoTest {
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        TrainingDatabase::class.java
    ).allowMainThreadQueries().build()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertPlanPersistsSegmentsInDisplayOrder() = runTest {
        val planId = database.trainingPlanDao().upsertPlan(
            TrainingPlanRecord(
                plan = TrainingPlanEntity(
                    name = "Tempo 4x4",
                    repeats = 2,
                    freeTraining = true,
                    updatedAtEpochMs = 1_714_380_000_000
                ),
                segments = listOf(
                    TrainingPlanSegmentEntity(
                        segmentIndex = 0,
                        zoneId = "Z2",
                        zoneLabel = "Z2 Ausdauer",
                        targetMinBpm = 110,
                        targetMaxBpm = 128,
                        durationSeconds = 240
                    ),
                    TrainingPlanSegmentEntity(
                        segmentIndex = 1,
                        zoneId = "Z4",
                        zoneLabel = "Z4 Schwelle",
                        targetMinBpm = 146,
                        targetMaxBpm = 165,
                        durationSeconds = 240,
                        note = "push"
                    )
                )
            )
        )

        val restored = database.trainingPlanDao().observePlans().first().single()

        assertEquals(planId, restored.plan.planId)
        assertEquals(listOf("Z2", "Z4"), restored.segments.map { it.zoneId })
        assertEquals(listOf(0, 1), restored.segments.map { it.segmentIndex })
        assertEquals("push", restored.segments.last().note)
    }

    @Test
    fun upsertPlanReplacesPreviousSegmentsForSameId() = runTest {
        val dao = database.trainingPlanDao()
        val planId = dao.upsertPlan(
            TrainingPlanRecord(
                plan = TrainingPlanEntity(
                    name = "Base",
                    repeats = 1,
                    freeTraining = false,
                    updatedAtEpochMs = 100
                ),
                segments = listOf(
                    TrainingPlanSegmentEntity(
                        segmentIndex = 0,
                        zoneId = "Z2",
                        zoneLabel = "Z2",
                        targetMinBpm = 110,
                        targetMaxBpm = 128,
                        durationSeconds = 300
                    )
                )
            )
        )

        dao.upsertPlan(
            TrainingPlanRecord(
                plan = TrainingPlanEntity(
                    planId = planId,
                    name = "Base",
                    repeats = 3,
                    freeTraining = true,
                    updatedAtEpochMs = 200
                ),
                segments = listOf(
                    TrainingPlanSegmentEntity(
                        segmentIndex = 0,
                        zoneId = "Z1",
                        zoneLabel = "Z1",
                        targetMinBpm = 0,
                        targetMaxBpm = 109,
                        durationSeconds = 180
                    ),
                    TrainingPlanSegmentEntity(
                        segmentIndex = 1,
                        zoneId = "Z5",
                        zoneLabel = "Z5",
                        targetMinBpm = 166,
                        targetMaxBpm = 183,
                        durationSeconds = 60
                    )
                )
            )
        )

        val restored = dao.getPlan(planId)

        assertNotNull(restored)
        assertEquals(3, restored!!.plan.repeats)
        assertEquals(true, restored.plan.freeTraining)
        assertEquals(listOf("Z1", "Z5"), restored.segments.map { it.zoneId })
    }
}
