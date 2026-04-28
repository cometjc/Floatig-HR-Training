package com.example.floatinghr.service

import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingZoneBarGeometryTest {
    private val zones = listOf(
        FloatingZoneSegment("Z1", 91, 110, 0xFF888888.toInt()),
        FloatingZoneSegment("Z2", 110, 128, 0xFF00BCD4.toInt()),
        FloatingZoneSegment("Z3", 128, 146, 0xFF4CAF50.toInt())
    )

    @Test
    fun startsHeartRateScaleAtZoneOneMinimumBpm() {
        val startX = FloatingZoneBarGeometry.positionForBpm(
            bpm = 91,
            zones = zones,
            barLeft = 38f,
            barRight = 482f
        )

        assertEquals(38f, startX, 0.001f)
    }

    @Test
    fun clampsHeartRatesBelowZoneOneMinimumToScaleStart() {
        val startX = FloatingZoneBarGeometry.positionForBpm(
            bpm = 72,
            zones = zones,
            barLeft = 38f,
            barRight = 482f
        )

        assertEquals(38f, startX, 0.001f)
    }

    @Test
    fun defaultFloatingZonesStartZoneOneAtTrainingHeartRateFloor() {
        assertEquals(91, DefaultFloatingZones.first().minBpm)
    }
}
