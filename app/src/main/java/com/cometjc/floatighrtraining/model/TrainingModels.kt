package com.cometjc.floatighrtraining.model

import androidx.compose.ui.graphics.Color

enum class HeartRateStatus {
    TooLow,
    InTarget,
    TooHigh
}

data class HeartRateZone(
    val id: String,
    val label: String,
    val minBpm: Int,
    val maxBpm: Int,
    val color: Color
) {
    val shortLabel: String = id

    companion object {
        val entries: List<HeartRateZone>
            get() = defaultZones()

        val Z1: HeartRateZone
            get() = entries[0]
        val Z2: HeartRateZone
            get() = entries[1]
        val Z3: HeartRateZone
            get() = entries[2]
        val Z4: HeartRateZone
            get() = entries[3]
        val Z5: HeartRateZone
            get() = entries[4]
    }
}

data class TrainingSegment(
    val zone: HeartRateZone,
    val durationSeconds: Int,
    val note: String = ""
) {
    val minutes: Int = durationSeconds / 60
}

data class TrainingPlan(
    val name: String,
    val repeats: Int,
    val segments: List<TrainingSegment>,
    val freeTraining: Boolean = false
) {
    val totalSeconds: Int = segments.sumOf { it.durationSeconds } * repeats
    val totalSegmentCount: Int = segments.size * repeats
    val totalMinutes: Int = totalSeconds / 60
    val totalSegments: Int = totalSegmentCount
    val zonesUsed: List<HeartRateZone> = segments.map { it.zone }.distinctBy { it.id }
}

data class WorkoutHistory(
    val title: String,
    val dateLabel: String,
    val durationLabel: String,
    val averageBpm: Int
) {
    val name: String = title
    val date: String = dateLabel
    val duration: String = durationLabel
}

fun defaultZones(maxHeartRate: Int = 183): List<HeartRateZone> {
    fun pct(value: Double) = (maxHeartRate * value).toInt()
    return listOf(
        HeartRateZone("Z1", "Z1 Erholung", 0, pct(0.60), Color(0xFF9AA0A6)),
        HeartRateZone("Z2", "Z2 Ausdauer", pct(0.60), pct(0.70), Color(0xFF4FC3F7)),
        HeartRateZone("Z3", "Z3 Tempo", pct(0.70), pct(0.80), Color(0xFF4CAF50)),
        HeartRateZone("Z4", "Z4 Schwelle", pct(0.80), pct(0.90), Color(0xFFFF7043)),
        HeartRateZone("Z5", "Z5 VO2 Max", pct(0.90), maxHeartRate, Color(0xFF7E57C2))
    )
}

fun sampleTrainingPlan(zones: List<HeartRateZone> = defaultZones()): TrainingPlan {
    val z2 = zones.first { it.id == "Z2" }
    val z3 = zones.first { it.id == "Z3" }
    val z4 = zones.first { it.id == "Z4" }
    return TrainingPlan(
        name = "v1",
        repeats = 3,
        segments = listOf(
            TrainingSegment(z2, 15 * 60),
            TrainingSegment(z3, 60),
            TrainingSegment(z4, 3 * 60),
            TrainingSegment(z3, 60)
        )
    )
}

fun heartRateStatus(bpm: Int, zone: HeartRateZone): HeartRateStatus = when {
    bpm < zone.minBpm -> HeartRateStatus.TooLow
    bpm > zone.maxBpm -> HeartRateStatus.TooHigh
    else -> HeartRateStatus.InTarget
}

data class AlertState(
    val status: HeartRateStatus,
    val label: String,
    val color: Color
) {
    companion object {
        fun fromBpm(bpm: Int, zone: HeartRateZone): AlertState {
            return when (heartRateStatus(bpm, zone)) {
                HeartRateStatus.TooLow -> AlertState(HeartRateStatus.TooLow, "加快步伐", Color(0xFF4FC3F7))
                HeartRateStatus.InTarget -> AlertState(HeartRateStatus.InTarget, "Im Ziel", Color(0xFF37C96B))
                HeartRateStatus.TooHigh -> AlertState(HeartRateStatus.TooHigh, "放慢步伐", Color(0xFFFF453A))
            }
        }
    }
}

data class DiscoveredHeartRateDevice(
    val address: String,
    val name: String,
    val rssi: Int
)

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
