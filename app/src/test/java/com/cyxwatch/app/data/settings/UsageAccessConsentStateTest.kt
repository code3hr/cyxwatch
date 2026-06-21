package com.cyxwatch.app.data.settings

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class UsageAccessConsentStateTest {
    @Test
    fun `permission state tracks denied then recovery as granted without losing denied history`() {
        val initial = DefaultUsageAccessConsentState

        val deniedThenRecovered = initial
            .copyOnCheck(granted = false, checkedAtEpochMs = 100L)
            .copyOnCheck(granted = true, checkedAtEpochMs = 200L)

        assertEquals(2, deniedThenRecovered.checkCount)
        assertEquals(1, deniedThenRecovered.deniedCount)
        assertTrue(deniedThenRecovered.hasEverDenied)
        assertTrue(deniedThenRecovered.hasEverGranted)
        assertEquals(200L, deniedThenRecovered.lastCheckedAtEpochMs)
    }

    @Test
    fun `permission recovery does not increase denied count when permission is granted again`() {
        val state = DefaultUsageAccessConsentState
            .copyOnCheck(granted = false, checkedAtEpochMs = 100L)

        val recovered = state.copyOnCheck(granted = true, checkedAtEpochMs = 200L)

        assertEquals(2, recovered.checkCount)
        assertEquals(1, recovered.deniedCount)
        assertEquals(200L, recovered.lastCheckedAtEpochMs)
    }
}

