package com.cyxwatch.app.runtime

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class RuntimeIntegrityGuardTest {
    @Test
    fun `build debug bypasses runtime blocking`() {
        val guard = RuntimeIntegrityGuard(
            isBuildDebuggable = true,
            isApplicationDebuggable = { true },
            isDebuggerConnected = { true },
            isAdbEnabled = { true },
        )

        val result = guard.canRunHighRiskAction("vpn start")

        assertTrue(result.isAllowed)
        assertTrue(result.reasons.isEmpty())
    }

    @Test
    fun `release blocks when app is debuggable`() {
        val guard = RuntimeIntegrityGuard(
            isBuildDebuggable = false,
            isApplicationDebuggable = { true },
            isDebuggerConnected = { false },
            isAdbEnabled = { false },
        )

        val result = guard.canRunHighRiskAction("permission settings")

        assertFalse(result.isAllowed)
        assertEquals(listOf("app is debuggable"), result.reasons)
    }

    @Test
    fun `release blocks when debugger is attached`() {
        val guard = RuntimeIntegrityGuard(
            isBuildDebuggable = false,
            isApplicationDebuggable = { false },
            isDebuggerConnected = { true },
            isAdbEnabled = { false },
        )

        val result = guard.canRunHighRiskAction("vpn start")

        assertFalse(result.isAllowed)
        assertEquals(listOf("active debugger connection"), result.reasons)
    }

    @Test
    fun `release blocks when adb is enabled`() {
        val guard = RuntimeIntegrityGuard(
            isBuildDebuggable = false,
            isApplicationDebuggable = { false },
            isDebuggerConnected = { false },
            isAdbEnabled = { true },
        )

        val result = guard.canRunHighRiskAction("vpn start")

        assertFalse(result.isAllowed)
        assertEquals(listOf("ADB is enabled"), result.reasons)
    }

    @Test
    fun `release allows when no integrity concerns detected`() {
        val guard = RuntimeIntegrityGuard(
            isBuildDebuggable = false,
            isApplicationDebuggable = { false },
            isDebuggerConnected = { false },
            isAdbEnabled = { false },
        )

        val result = guard.canRunHighRiskAction("vpn start")

        assertTrue(result.isAllowed)
        assertTrue(result.reasons.isEmpty())
    }
}

