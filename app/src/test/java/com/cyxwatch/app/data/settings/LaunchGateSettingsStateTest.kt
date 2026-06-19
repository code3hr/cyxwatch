package com.cyxwatch.app.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchGateSettingsStateTest {
    @Test
    fun `withCompleted marks completed and sets opened privacy controls`() {
        val initialState = DefaultLaunchGateSettingsState.copy()

        val completedState = initialState.withCompleted(
            openedPrivacyControlsFromGate = true,
            completedAtEpochMs = 123L,
        )

        assertTrue(completedState.hasCompletedLaunchGate)
        assertTrue(completedState.hasOpenedPrivacyControlsFromGate)
        assertEquals(123L, completedState.completedAtEpochMs)
    }

    @Test
    fun `withCompleted preserves opened flag when already opened`() {
        val alreadyOpened = LaunchGateSettingsState(
            hasCompletedLaunchGate = false,
            hasOpenedPrivacyControlsFromGate = true,
            completedAtEpochMs = null,
        )

        val completedState = alreadyOpened.withCompleted(
            openedPrivacyControlsFromGate = false,
            completedAtEpochMs = 777L,
        )

        assertTrue(completedState.hasCompletedLaunchGate)
        assertTrue(completedState.hasOpenedPrivacyControlsFromGate)
        assertEquals(777L, completedState.completedAtEpochMs)
    }

    @Test
    fun `withCompleted can set completion without privacy controls`() {
        val initialState = DefaultLaunchGateSettingsState.copy()

        val completedState = initialState.withCompleted(
            openedPrivacyControlsFromGate = false,
            completedAtEpochMs = 999L,
        )

        assertTrue(completedState.hasCompletedLaunchGate)
        assertFalse(completedState.hasOpenedPrivacyControlsFromGate)
        assertEquals(999L, completedState.completedAtEpochMs)
    }
}
