package com.cyxwatch.app.data.settings

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageAccessConsentMigrationTest {
    private val preferenceName = "cyxwatch_consent_prefs"

    private val legacyEverGrantedKey = "usage_access_ever_granted"
    private val legacyEverDeniedKey = "usage_access_ever_denied"
    private val legacyCheckCountKey = "usage_access_check_count"
    private val legacyDeniedCountKey = "usage_access_denied_count"
    private val legacyLastCheckedAtKey = "usage_access_last_checked_at"
    private val legacyLastSettingsOpenedAtKey = "usage_access_last_settings_opened_at"

    @Test
    fun `legacy usage access keys are migrated into encrypted store on read`() {
        val context = context()
        context.deleteSharedPreferences(preferenceName)

        val legacyPrefs = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
        legacyPrefs.edit()
            .putBoolean(legacyEverGrantedKey, true)
            .putBoolean(legacyEverDeniedKey, true)
            .putInt(legacyCheckCountKey, 3)
            .putInt(legacyDeniedCountKey, 2)
            .putLong(legacyLastCheckedAtKey, 123_456L)
            .putLong(legacyLastSettingsOpenedAtKey, 111L)
            .apply()

        val repository = UsageAccessConsentRepository(context)
        val migratedState = repository.readState()

        assertEquals(true, migratedState.hasEverGranted)
        assertEquals(true, migratedState.hasEverDenied)
        assertEquals(3, migratedState.checkCount)
        assertEquals(2, migratedState.deniedCount)
        assertEquals(123_456L, migratedState.lastCheckedAtEpochMs)
        assertEquals(111L, migratedState.lastSettingsOpenedAtEpochMs)

        assertFalse("legacy value should be removed after migration", legacyPrefs.contains(legacyEverGrantedKey))
        assertFalse("legacy value should be removed after migration", legacyPrefs.contains(legacyLastCheckedAtKey))

        val reReadState = repository.readState()

        assertEquals(3, reReadState.checkCount)
        assertEquals(2, reReadState.deniedCount)
        assertEquals(123_456L, reReadState.lastCheckedAtEpochMs)
        assertEquals(111L, reReadState.lastSettingsOpenedAtEpochMs)
    }

    private fun context(): Context {
        return InstrumentationRegistry.getInstrumentation().targetContext
    }
}

