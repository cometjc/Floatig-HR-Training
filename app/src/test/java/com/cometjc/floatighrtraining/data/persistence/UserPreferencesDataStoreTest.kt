package com.cometjc.floatighrtraining.data.persistence

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UserPreferencesDataStoreTest {
    @Test
    fun defaultsMatchExpectedMvpSettings() = runTest {
        val store = UserPreferencesDataStore(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { tempPreferencesFile("defaults.preferences_pb") }
            )
        )

        val preferences = store.preferences.first()

        assertEquals(183, preferences.maxHeartRate)
        assertEquals(5, preferences.zoneRanges.size)
        assertTrue(preferences.alertPreferences.soundEnabled)
        assertTrue(preferences.alertPreferences.vibrationEnabled)
        assertTrue(preferences.alertPreferences.tooLowSoundEnabled)
        assertTrue(preferences.alertPreferences.tooLowVibrationEnabled)
        assertTrue(preferences.alertPreferences.tooHighSoundEnabled)
        assertTrue(preferences.alertPreferences.tooHighVibrationEnabled)
        assertTrue(preferences.overlayPreferences.floatingModeEnabled)
    }

    @Test
    fun savePersistsCustomZonesAlertsAndOverlaySettings() = runTest {
        val store = UserPreferencesDataStore(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { tempPreferencesFile("custom.preferences_pb") }
            )
        )
        val custom = UserPreferences(
            maxHeartRate = 191,
            zoneRanges = listOf(
                ZoneRangePreference("Z1", 0, 114),
                ZoneRangePreference("Z2", 115, 133),
                ZoneRangePreference("Z3", 134, 152),
                ZoneRangePreference("Z4", 153, 171),
                ZoneRangePreference("Z5", 172, 191)
            ),
            alertPreferences = AlertPreferences(
                soundEnabled = false,
                vibrationEnabled = true,
                tooLowEnabled = true,
                tooHighEnabled = false,
                tooLowSoundEnabled = false,
                tooLowVibrationEnabled = true,
                tooHighSoundEnabled = true,
                tooHighVibrationEnabled = false
            ),
            overlayPreferences = OverlayPreferences(
                floatingModeEnabled = false,
                size = OverlaySizePreference.LARGE,
                transparencyPercent = 22,
                anchorX = 0.2f,
                anchorY = 0.8f
            )
        )

        store.save(custom)

        val restored = store.preferences.first()
        assertEquals(custom, restored)
        assertFalse(restored.alertPreferences.soundEnabled)
        assertEquals(OverlaySizePreference.LARGE, restored.overlayPreferences.size)
    }

    private fun tempPreferencesFile(name: String): File {
        val directory = File(System.getProperty("java.io.tmpdir"), "user-preferences-tests").apply {
            mkdirs()
        }
        return File(directory, name).apply {
            delete()
            deleteOnExit()
        }
    }
}
