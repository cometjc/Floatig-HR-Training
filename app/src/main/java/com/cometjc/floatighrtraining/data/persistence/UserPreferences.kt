package com.cometjc.floatighrtraining.data.persistence

data class UserPreferences(
    val maxHeartRate: Int = DEFAULT_MAX_HEART_RATE,
    val zoneRanges: List<ZoneRangePreference> = defaultZoneRanges(DEFAULT_MAX_HEART_RATE),
    val alertPreferences: AlertPreferences = AlertPreferences(),
    val overlayPreferences: OverlayPreferences = OverlayPreferences()
) {
    companion object {
        const val DEFAULT_MAX_HEART_RATE = 183
    }
}

data class ZoneRangePreference(
    val zoneId: String,
    val minBpm: Int,
    val maxBpm: Int
)

data class AlertPreferences(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val tooLowEnabled: Boolean = true,
    val tooHighEnabled: Boolean = true,
    /** Per-alert toggles: too-low pacing (e.g. SpeedUp) */
    val tooLowSoundEnabled: Boolean = true,
    val tooLowVibrationEnabled: Boolean = true,
    /** Per-alert toggles: too-high pacing (SlowDownSoon / SlowDownNow) */
    val tooHighSoundEnabled: Boolean = true,
    val tooHighVibrationEnabled: Boolean = true
)

data class OverlayPreferences(
    val floatingModeEnabled: Boolean = true,
    val size: OverlaySizePreference = OverlaySizePreference.STANDARD,
    val transparencyPercent: Int = 20,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.85f
)

enum class OverlaySizePreference {
    COMPACT,
    STANDARD,
    LARGE
}

fun defaultZoneRanges(maxHeartRate: Int): List<ZoneRangePreference> {
    fun pct(ratio: Double) = (maxHeartRate * ratio).toInt()
    return listOf(
        ZoneRangePreference("Z1", 0, pct(0.60)),
        ZoneRangePreference("Z2", pct(0.60), pct(0.70)),
        ZoneRangePreference("Z3", pct(0.70), pct(0.80)),
        ZoneRangePreference("Z4", pct(0.80), pct(0.90)),
        ZoneRangePreference("Z5", pct(0.90), maxHeartRate)
    )
}
