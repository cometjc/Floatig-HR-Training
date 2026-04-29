package com.cometjc.floatighrtraining.data.persistence

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeartRateDelayEstimateDaoTest {
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        TrainingDatabase::class.java
    ).allowMainThreadQueries().build()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun latestEstimateForZoneWins() = runTest {
        val dao = database.heartRateDelayEstimateDao()
        dao.insert(
            HeartRateDelayEstimateEntity(
                zoneId = "Z3",
                delaySeconds = 30,
                confidence = 0.42f,
                sampleCount = 8,
                recordedAtEpochMs = 100
            )
        )
        dao.insert(
            HeartRateDelayEstimateEntity(
                zoneId = "Z3",
                delaySeconds = 24,
                confidence = 0.71f,
                sampleCount = 16,
                recordedAtEpochMs = 200
            )
        )

        val restored = dao.getLatestForZone("Z3")

        assertNotNull(restored)
        assertEquals(24, restored!!.delaySeconds)
        assertEquals(0.71f, restored.confidence)
    }
}
