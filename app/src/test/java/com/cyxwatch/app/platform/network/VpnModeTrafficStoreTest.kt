package com.cyxwatch.app.platform.network

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VpnModeTrafficStoreTest {
    private val store = VpnModeTrafficStore()

    @Before
    fun setUp() {
        store.clear()
    }

    @Test
    fun `aggregates destination totals and returns top snapshot`() {
        store.recordPacket(
            VpnPacketMetadata(
                destinationIp = "203.0.113.10",
                destinationPort = 443,
                protocol = "TCP",
                packetBytes = 512L,
            ),
        )
        store.recordPacket(
            VpnPacketMetadata(
                destinationIp = "198.51.100.20",
                destinationPort = 443,
                protocol = "TCP",
                packetBytes = 128L,
            ),
        )
        store.recordPacket(
            VpnPacketMetadata(
                destinationIp = "198.51.100.20",
                destinationPort = 443,
                protocol = "TCP",
                packetBytes = 128L,
            ),
        )

        val top = store.snapshotTopDestinations()

        assertEquals(2, top.size)
        assertEquals("203.0.113.10", top[0].destinationIp)
        assertEquals(512L, top[0].bytes)
        assertEquals(1, top[0].packetCount)
        assertEquals("198.51.100.20", top[1].destinationIp)
        assertEquals(256L, top[1].bytes)
        assertEquals(2, top[1].packetCount)
        val totals = store.snapshotTotals()
        assertEquals(3L, totals.totalPackets)
        assertEquals(768L, totals.totalBytes)
        assertEquals(2, totals.uniqueDestinationCount)
        assertEquals(3L, totals.parsedPackets)
        assertEquals(0L, totals.unparsedPackets)
        assertEquals("monitor-only", totals.captureMode)
        assertEquals(false, totals.forwardingEnabled)
    }

    @Test
    fun `counts unparsed packets and bytes separately`() {
        store.setForwardingEnabled(enabled = true)
        store.recordPacket(
            VpnPacketMetadata(
                destinationIp = "198.51.100.30",
                destinationPort = 53,
                protocol = "UDP",
                packetBytes = 64L,
            ),
        )
        store.recordUnparsedPacket(packetBytes = 88L)
        store.recordUnparsedPacket(packetBytes = 12L)

        val totals = store.snapshotTotals()
        assertEquals(3L, totals.totalPackets)
        assertEquals(164L, totals.totalBytes)
        assertEquals(1, totals.uniqueDestinationCount)
        assertEquals(1L, totals.parsedPackets)
        assertEquals(2L, totals.unparsedPackets)
        assertEquals("forwarding", totals.captureMode)
        assertEquals(true, totals.forwardingEnabled)
    }
}
