package com.cyxwatch.app.data.settings

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchGateSettingsRepositoryTest {
    private val preferenceName = "cyxwatch_launch_gate_prefs"

    @Test
    fun `readState returns defaults for empty state`() {
        val context = context()
        context.deleteSharedPreferences(preferenceName)

        val repository = LaunchGateSettingsRepository(context)
        val state = repository.readState()

        assertFalse(state.hasCompletedLaunchGate)
        assertFalse(state.hasOpenedPrivacyControlsFromGate)
        assertEquals(null, state.completedAtEpochMs)
    }

    @Test
    fun `markCompleted persists fields as expected`() {
        val context = context()
        context.deleteSharedPreferences(preferenceName)

        val repository = LaunchGateSettingsRepository(context)
        repository.markCompleted(openedPrivacyControlsFromGate = true, completedAtEpochMs = 1_701_200_001L)

        val state = repository.readState()

        assertTrue(state.hasCompletedLaunchGate)
        assertTrue(state.hasOpenedPrivacyControlsFromGate)
        assertEquals(1_701_200_001L, state.completedAtEpochMs)
    }

    @Test
    fun `markCompleted can upgrade opened privacy controls state`() {
        val context = context()
        context.deleteSharedPreferences(preferenceName)

        val repository = LaunchGateSettingsRepository(context)
        repository.markCompleted(openedPrivacyControlsFromGate = false, completedAtEpochMs = 10L)
        repository.markCompleted(openedPrivacyControlsFromGate = true, completedAtEpochMs = 20L)

        val state = repository.readState()

        assertTrue(state.hasCompletedLaunchGate)
        assertTrue(state.hasOpenedPrivacyControlsFromGate)
        assertEquals(20L, state.completedAtEpochMs)
    }

    private fun context(): Context {
        return InstrumentationRegistry.getInstrumentation().targetContext
    }
}
