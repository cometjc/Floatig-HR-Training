package com.cometjc.floatighrtraining.data

import com.cometjc.floatighrtraining.model.DiscoveredHeartRateDevice
import com.cometjc.floatighrtraining.model.WorkoutHistory
import com.cometjc.floatighrtraining.model.defaultZones
import com.cometjc.floatighrtraining.model.sampleTrainingPlan

object SampleRepository {
    val zones = defaultZones()
    val trainingPlans = listOf(sampleTrainingPlan(zones))
    val devices = listOf(
        DiscoveredHeartRateDevice(
            address = "00:11:22:33:44:55",
            name = "Polar H10 0FC4C438",
            rssi = -52
        )
    )
    val workouts = listOf(
        WorkoutHistory("v1", "25. Apr. 2026 · 16:58", "00:24", 126),
        WorkoutHistory("v1", "25. Apr. 2026 · 16:23", "24:21", 126),
        WorkoutHistory("Freies Training", "11. Apr. 2026 · 20:33", "28:24", 130)
    )
}
