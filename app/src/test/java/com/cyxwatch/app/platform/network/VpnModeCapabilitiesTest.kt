package com.cyxwatch.app.platform.network

import org.junit.Assert.assertFalse
import org.junit.Test

class VpnModeCapabilitiesTest {
    @Test
    fun `forwarding mode is disabled by default in V1`() {
        assertFalse(VpnModeCapabilities.FORWARDING_MODE_SUPPORTED)
    }
}

