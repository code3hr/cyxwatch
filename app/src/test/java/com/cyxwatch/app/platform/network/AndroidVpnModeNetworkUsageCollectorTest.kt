package com.cyxwatch.app.platform.network

import com.cyxwatch.app.domain.NetworkUsageCollector
import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.NetworkUsageSummary
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AndroidVpnModeNetworkUsageCollectorTest {
    @Test
    fun `adds vpn mode metadata to collected network events`() {
        val trafficStore = VpnModeTrafficStore()
        trafficStore.recordPacket(
            VpnPacketMetadata(
                destinationIp = "203.0.113.10",
                destinationPort = 443,
                protocol = "TCP",
                packetBytes = 140L,
            ),
        )

        val delegate = FakeNetworkUsageCollectorForVpnTest(
            NetworkUsageSummary(
                windowStartEpochMs = 1L,
                windowEndEpochMs = 2L,
                totalBytes = 128L,
                totalBytesByNetworkType = mapOf("wifi" to 128L),
                totalBytesByPackageName = mapOf("com.example.chat" to 128L),
                events = listOf(
                    PrivacyEvent(
                        eventId = "network-chat",
                        timestamp = Instant.parse("2026-06-13T12:00:00Z"),
                        packageName = "com.example.chat",
                        eventType = EventType.NETWORK_USAGE,
                        severity = Severity.LOW,
                        source = "NetworkStats",
                        title = "Network usage observed",
                        explanation = "Observed 128 bytes for com.example.chat",
                        evidenceJson = """{"source":"network_summary","packageName":"com.example.chat","packageBytes":128}""",
                    ),
                ),
            ),
        )
        val collector = AndroidVpnModeNetworkUsageCollector(
            delegateCollector = delegate,
            trafficStore = trafficStore,
        )
        val summary = collector.collectNetworkUsageSummary()
        val event = summary.events.single()

        assertEquals(1, delegate.callCount)
        assertEquals("VpnMode", event.source)
        assertEquals("Advanced network usage observed", event.title)
        assertEquals("network-chat-vpn", event.eventId)
        assertTrue(event.evidenceJson.contains("\"collectionMode\":\"vpn\""))
        assertTrue(event.evidenceJson.contains("\"packetsObserved\":1"))
        assertTrue(event.evidenceJson.contains("\"bytesObserved\":140"))
        assertTrue(event.evidenceJson.contains("\"uniqueDestinations\":1"))
        assertTrue(event.evidenceJson.contains("\"parsedPackets\":1"))
        assertTrue(event.evidenceJson.contains("\"unparsedPackets\":0"))
        assertTrue(event.evidenceJson.contains("\"captureMode\":\"monitor-only\""))
        assertTrue(event.evidenceJson.contains("\"forwardingEnabled\":false"))
        assertTrue(event.evidenceJson.contains("203.0.113.10"))
        assertTrue(event.evidenceJson.contains("\"destinationPort\":443"))
        assertEquals(128L, summary.totalBytesByPackageName["com.example.chat"])
        assertEquals(128L, summary.totalBytes)
    }
}

private class FakeNetworkUsageCollectorForVpnTest(
    private val summary: NetworkUsageSummary,
) : NetworkUsageCollector {
    var callCount = 0
        private set

    override fun collectNetworkUsageSummary(): NetworkUsageSummary {
        callCount++
        return summary
    }
}
