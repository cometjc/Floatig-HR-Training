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
class WorkoutDaoTest {
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        TrainingDatabase::class.java
    ).allowMainThreadQueries().build()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertWorkoutWithSamplesReturnsSortedTimeline() = runTest {
        val workoutId = database.workoutDao().insertWorkout(
            WorkoutEntity(
                planId = null,
                planName = "Freies Training",
                startedAtEpochMs = 1_714_380_000_000,
                endedAtEpochMs = 1_714_381_800_000,
                averageHeartRateBpm = 141,
                averageCadenceSpm = 168,
                connectedDeviceName = "Polar H10"
            )
        )

        database.workoutDao().insertSamples(
            listOf(
                WorkoutSampleEntity(
                    workoutId = workoutId,
                    elapsedSeconds = 30,
                    heartRateBpm = 126,
                    cadenceSpm = 164,
                    zoneId = "Z2"
                ),
                WorkoutSampleEntity(
                    workoutId = workoutId,
                    elapsedSeconds = 10,
                    heartRateBpm = 118,
                    cadenceSpm = 160,
                    zoneId = "Z2"
                )
            )
        )

        val restored = database.workoutDao().getWorkoutWithSamples(workoutId)

        assertNotNull(restored)
        assertEquals("Freies Training", restored!!.workout.planName)
        assertEquals(listOf(10, 30), restored.samples.map { it.elapsedSeconds })
        assertEquals(listOf(118, 126), restored.samples.map { it.heartRateBpm })
    }
}
