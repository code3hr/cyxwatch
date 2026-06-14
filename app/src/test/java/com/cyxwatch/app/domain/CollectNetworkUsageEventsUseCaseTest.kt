package com.cyxwatch.app.domain

import com.cyxwatch.app.data.network.NetworkUsageTotalsRepository
import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.NetworkUsageSummary
import com.cyxwatch.app.domain.model.NetworkUsageDailyTotals
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CollectNetworkUsageEventsUseCaseTest {
    @Test
    fun `uses basic collector when vpn mode is disabled and persists network totals`() {
        val basicCollector = FakeNetworkUsageCollector(
            NetworkUsageSummary(
                windowStartEpochMs = 1L,
                windowEndEpochMs = 2L,
                totalBytes = 512L,
                totalBytesByNetworkType = mapOf("wifi" to 500L, "mobile" to 12L),
                totalBytesByPackageName = mapOf(
                    "com.example.chat" to 300L,
                    "com.example.maps" to 212L,
                ),
                events = listOf(
                    networkEvent("network-chat", "com.example.chat", bytes = 300L),
                    networkEvent("network-maps", "com.example.maps", bytes = 212L),
                ),
            ),
        )
        val vpnCollector = FakeNetworkUsageCollector(
            NetworkUsageSummary(
                windowStartEpochMs = 1L,
                windowEndEpochMs = 2L,
                totalBytes = 111L,
                totalBytesByNetworkType = mapOf("wifi" to 111L),
                totalBytesByPackageName = mapOf("com.example.chat" to 111L),
                events = listOf(networkEvent("network-vpn-chat", "com.example.chat", bytes = 111L)),
            ),
        )
        val repository = FakeNetworkUsageTotalsRepository()
        val useCase = CollectNetworkUsageEventsUseCase(
            permissionStateProvider = FakeUsagePermissionStateProvider(true),
            basicNetworkUsageCollector = basicCollector,
            vpnModeNetworkUsageCollector = vpnCollector,
            networkUsageTotalsRepository = repository,
        )

        val result = useCase.collectLast24hNetworkUsageEvents(isVpnModeEnabled = false)

        assertTrue(result is UsageCollectionResult.Success)
        assertEquals(1, basicCollector.callCount)
        assertEquals(0, vpnCollector.callCount)
        assertEquals(2, (result as UsageCollectionResult.Success).events.size)
        val stored = repository.written
        assertEquals(1L, stored?.dayStartEpochMs)
        assertEquals(512L, stored?.totalBytes)
        assertEquals(mapOf("wifi" to 500L, "mobile" to 12L), stored?.totalBytesByNetworkType)
        assertEquals(300L, stored?.totalBytesByPackageName?.get("com.example.chat"))
        assertEquals(212L, stored?.totalBytesByPackageName?.get("com.example.maps"))
    }

    @Test
    fun `uses vpn collector when vpn mode is enabled`() {
        val basicCollector = FakeNetworkUsageCollector(
            NetworkUsageSummary(
                windowStartEpochMs = 1L,
                windowEndEpochMs = 2L,
                totalBytes = 10L,
                totalBytesByNetworkType = mapOf("wifi" to 10L),
                totalBytesByPackageName = mapOf("com.example.chat" to 10L),
                events = listOf(networkEvent("network-chat", "com.example.chat", bytes = 10L)),
            ),
        )
        val vpnCollector = FakeNetworkUsageCollector(
            NetworkUsageSummary(
                windowStartEpochMs = 1L,
                windowEndEpochMs = 2L,
                totalBytes = 11L,
                totalBytesByNetworkType = mapOf("wifi" to 11L),
                totalBytesByPackageName = mapOf("com.example.chat" to 11L),
                events = listOf(networkEvent("network-vpn-chat", "com.example.chat", bytes = 11L)),
            ),
        )
        val repository = FakeNetworkUsageTotalsRepository()
        val useCase = CollectNetworkUsageEventsUseCase(
            permissionStateProvider = FakeUsagePermissionStateProvider(true),
            basicNetworkUsageCollector = basicCollector,
            vpnModeNetworkUsageCollector = vpnCollector,
            networkUsageTotalsRepository = repository,
        )

        val result = useCase.collectLast24hNetworkUsageEvents(isVpnModeEnabled = true)

        assertTrue(result is UsageCollectionResult.Success)
        assertEquals(0, basicCollector.callCount)
        assertEquals(1, vpnCollector.callCount)
        val stored = repository.written
        assertEquals(11L, stored?.totalBytes)
    }

    @Test
    fun `returns permission missing and does not write totals when usage access is missing`() {
        val basicCollector = FakeNetworkUsageCollector(
            summary = NetworkUsageSummary(
                windowStartEpochMs = 1L,
                windowEndEpochMs = 2L,
                totalBytes = 1024L,
                totalBytesByNetworkType = mapOf("wifi" to 1024L),
                totalBytesByPackageName = mapOf("com.example.chat" to 1024L),
                events = listOf(
                    networkEvent("network-chat", "com.example.chat", bytes = 1024L),
                ),
            ),
        )
        val vpnCollector = FakeNetworkUsageCollector(
            summary = NetworkUsageSummary(
                windowStartEpochMs = 1L,
                windowEndEpochMs = 2L,
                totalBytes = 1024L,
                totalBytesByNetworkType = mapOf("wifi" to 1024L),
                totalBytesByPackageName = mapOf("com.example.chat" to 1024L),
                events = listOf(
                    networkEvent("network-chat", "com.example.chat", bytes = 1024L),
                ),
            ),
        )
        val repository = FakeNetworkUsageTotalsRepository()
        val useCase = CollectNetworkUsageEventsUseCase(
            permissionStateProvider = FakeUsagePermissionStateProvider(false),
            basicNetworkUsageCollector = basicCollector,
            vpnModeNetworkUsageCollector = vpnCollector,
            networkUsageTotalsRepository = repository,
        )

        val result = useCase.collectLast24hNetworkUsageEvents(isVpnModeEnabled = false)

        assertTrue(result is UsageCollectionResult.PermissionMissing)
        assertEquals(0, basicCollector.callCount)
        assertEquals(0, vpnCollector.callCount)
        assertNull(repository.written)
    }
}

private class FakeNetworkUsageCollector(
    private val summary: NetworkUsageSummary,
) : NetworkUsageCollector {
    var callCount = 0
        private set

    override fun collectNetworkUsageSummary(): NetworkUsageSummary {
        callCount++
        return summary
    }
}

private class FakeNetworkUsageTotalsRepository : NetworkUsageTotalsRepository {
    var written: NetworkUsageDailyTotals? = null
        private set

    override fun readLatestTotals(): NetworkUsageDailyTotals? = null

    override fun writeTotals(totals: NetworkUsageDailyTotals) {
        written = totals
    }
}

private class FakeUsagePermissionStateProvider(
    private val hasAccess: Boolean,
) : UsagePermissionStateProvider {
    override fun hasUsageAccess(): Boolean = hasAccess
}

private fun networkEvent(
    id: String,
    packageName: String,
    bytes: Long,
): PrivacyEvent {
    return PrivacyEvent(
        eventId = id,
        timestamp = Instant.parse("2026-06-13T12:00:00Z"),
        packageName = packageName,
        eventType = EventType.NETWORK_USAGE,
        severity = Severity.LOW,
        source = "NetworkStats",
        title = "Network usage observed",
        explanation = "Observed $bytes bytes for $packageName",
        evidenceJson = """{"source":"network_summary","packageName":"$packageName","packageBytes":$bytes}""",
    )
}
