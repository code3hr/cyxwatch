package com.cyxwatch.app.data.settings

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageAccessConsentRepositoryTest {
    private val preferenceName = "cyxwatch_consent_prefs"

    @Test
    fun `records denial and later grants preserve historical denial state`() {
        val context = context()
        context.deleteSharedPreferences(preferenceName)

        val repository = UsageAccessConsentRepository(context)
        repository.recordCheckResult(granted = false, checkedAtEpochMs = 100L)
        repository.recordSettingsOpened(openedAtEpochMs = 150L)
        repository.recordCheckResult(granted = false, checkedAtEpochMs = 200L)

        val deniedState = repository.readState()
        assertEquals(2, deniedState.checkCount)
        assertEquals(2, deniedState.deniedCount)
        assertTrue(deniedState.hasEverDenied)
        assertEquals(200L, deniedState.lastCheckedAtEpochMs)
        assertEquals(150L, deniedState.lastSettingsOpenedAtEpochMs)

        repository.recordCheckResult(granted = true, checkedAtEpochMs = 300L)
        val recoveredState = repository.readState()
        assertEquals(3, recoveredState.checkCount)
        assertEquals(2, recoveredState.deniedCount)
        assertTrue(recoveredState.hasEverDenied)
        assertTrue(recoveredState.hasEverGranted)
        assertEquals(300L, recoveredState.lastCheckedAtEpochMs)
    }

    private fun context(): Context {
        return InstrumentationRegistry.getInstrumentation().targetContext
    }
}

