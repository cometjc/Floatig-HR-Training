package com.cometjc.floatighrtraining.data.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesDataStore(
    private val dataStore: DataStore<Preferences>
) {

    val preferences: Flow<UserPreferences> = dataStore.data.map { stored ->
        val maxHeartRate = stored[KEY_MAX_HEART_RATE] ?: UserPreferences.DEFAULT_MAX_HEART_RATE
        val zoneRanges = stored[KEY_ZONE_RANGES]?.let(::decodeZoneRanges)
            ?: defaultZoneRanges(maxHeartRate)
        UserPreferences(
            maxHeartRate = maxHeartRate,
            zoneRanges = zoneRanges,
            alertPreferences = AlertPreferences(
                soundEnabled = stored[KEY_ALERT_SOUND] ?: true,
                vibrationEnabled = stored[KEY_ALERT_VIBRATION] ?: true,
                tooLowEnabled = stored[KEY_ALERT_TOO_LOW] ?: true,
                tooHighEnabled = stored[KEY_ALERT_TOO_HIGH] ?: true
            ),
            overlayPreferences = OverlayPreferences(
                floatingModeEnabled = stored[KEY_OVERLAY_FLOATING] ?: true,
                size = stored[KEY_OVERLAY_SIZE]?.toOverlaySize() ?: OverlaySizePreference.STANDARD,
                transparencyPercent = stored[KEY_OVERLAY_TRANSPARENCY] ?: 20,
                anchorX = stored[KEY_OVERLAY_ANCHOR_X] ?: 0.5f,
                anchorY = stored[KEY_OVERLAY_ANCHOR_Y] ?: 0.85f
            )
        )
    }

    suspend fun save(preferences: UserPreferences) {
        dataStore.edit { stored ->
            stored[KEY_MAX_HEART_RATE] = preferences.maxHeartRate
            stored[KEY_ZONE_RANGES] = encodeZoneRanges(preferences.zoneRanges)
            stored[KEY_ALERT_SOUND] = preferences.alertPreferences.soundEnabled
            stored[KEY_ALERT_VIBRATION] = preferences.alertPreferences.vibrationEnabled
            stored[KEY_ALERT_TOO_LOW] = preferences.alertPreferences.tooLowEnabled
            stored[KEY_ALERT_TOO_HIGH] = preferences.alertPreferences.tooHighEnabled
            stored[KEY_OVERLAY_FLOATING] = preferences.overlayPreferences.floatingModeEnabled
            stored[KEY_OVERLAY_SIZE] = preferences.overlayPreferences.size.name
            stored[KEY_OVERLAY_TRANSPARENCY] = preferences.overlayPreferences.transparencyPercent
            stored[KEY_OVERLAY_ANCHOR_X] = preferences.overlayPreferences.anchorX
            stored[KEY_OVERLAY_ANCHOR_Y] = preferences.overlayPreferences.anchorY
        }
    }

    private fun String.toOverlaySize(): OverlaySizePreference? =
        runCatching { OverlaySizePreference.valueOf(this) }.getOrNull()

    private companion object {
        val KEY_MAX_HEART_RATE = intPreferencesKey("max_heart_rate")
        val KEY_ZONE_RANGES = stringPreferencesKey("zone_ranges")
        val KEY_ALERT_SOUND = booleanPreferencesKey("alert_sound_enabled")
        val KEY_ALERT_VIBRATION = booleanPreferencesKey("alert_vibration_enabled")
        val KEY_ALERT_TOO_LOW = booleanPreferencesKey("alert_too_low_enabled")
        val KEY_ALERT_TOO_HIGH = booleanPreferencesKey("alert_too_high_enabled")
        val KEY_OVERLAY_FLOATING = booleanPreferencesKey("overlay_floating_mode_enabled")
        val KEY_OVERLAY_SIZE = stringPreferencesKey("overlay_size")
        val KEY_OVERLAY_TRANSPARENCY = intPreferencesKey("overlay_transparency_percent")
        val KEY_OVERLAY_ANCHOR_X = floatPreferencesKey("overlay_anchor_x")
        val KEY_OVERLAY_ANCHOR_Y = floatPreferencesKey("overlay_anchor_y")
    }
}

private const val ZONE_ENTRY_SEPARATOR = ";"
private const val ZONE_FIELD_SEPARATOR = "|"

private fun encodeZoneRanges(zones: List<ZoneRangePreference>): String =
    zones.joinToString(separator = ZONE_ENTRY_SEPARATOR) { zone ->
        listOf(zone.zoneId, zone.minBpm.toString(), zone.maxBpm.toString())
            .joinToString(ZONE_FIELD_SEPARATOR)
    }

private fun decodeZoneRanges(encoded: String): List<ZoneRangePreference> =
    encoded.split(ZONE_ENTRY_SEPARATOR)
        .filter { it.isNotEmpty() }
        .map { entry ->
            val fields = entry.split(ZONE_FIELD_SEPARATOR)
            ZoneRangePreference(
                zoneId = fields[0],
                minBpm = fields[1].toInt(),
                maxBpm = fields[2].toInt()
            )
        }
