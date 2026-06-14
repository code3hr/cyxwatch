package com.cyxwatch.app.platform.network

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VpnModePacketProcessorTest {
    private val trafficStore = VpnModeTrafficStore()

    @Before
    fun setUp() {
        trafficStore.clear()
    }

    @Test
    fun `records parsed packets and ignores empty reads`() {
        val processor = VpnModePacketProcessor(trafficStore = trafficStore)
        val packet = byteArrayOf(
            0x45,
            0x00,
            0x00.toByte(),
            0x28.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            0x40,
            0x06.toByte(),
            0x00,
            0x00,
            0x0A,
            0x00,
            0x00,
            0x01,
            203.toByte(),
            0x00,
            0x71.toByte(),
            0x0A,
            0x1F,
            0x90.toByte(),
            0x01.toByte(),
            0xBB.toByte(),
            0x00,
            0x14,
            0x00,
            0x00,
        )

        processor.processPacket(packet, packet.size)
        processor.processPacket(packet, 0)

        val totals = trafficStore.snapshotTotals()
        assertEquals(1L, totals.totalPackets)
        assertEquals(28L, totals.totalBytes)
        assertEquals(1, totals.uniqueDestinationCount)
        assertEquals(1L, totals.parsedPackets)
        assertEquals(0L, totals.unparsedPackets)
    }

    @Test
    fun `records unparsed packets when parser does not recognize format`() {
        val processor = VpnModePacketProcessor(trafficStore = trafficStore)
        val malformed = byteArrayOf(0x10, 0x00, 0x00, 0x00)

        processor.processPacket(malformed, malformed.size)

        val totals = trafficStore.snapshotTotals()
        assertEquals(1L, totals.totalPackets)
        assertEquals(4L, totals.totalBytes)
        assertEquals(0, totals.uniqueDestinationCount)
        assertEquals(0L, totals.parsedPackets)
        assertEquals(1L, totals.unparsedPackets)
    }

    @Test
    fun `calls forwarding sink for each parsed packet`() {
        var forwardedPacketCount = 0
        val forwardedBytes = IntArray(1)
        val processor = VpnModePacketProcessor(
            trafficStore = trafficStore,
        )
        val packet = byteArrayOf(
            0x45,
            0x00,
            0x00.toByte(),
            0x28.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            0x40,
            0x06.toByte(),
            0x00,
            0x00,
            0x0A,
            0x00,
            0x00,
            0x01,
            203.toByte(),
            0x00,
            0x71.toByte(),
            0x0A,
            0x1F,
            0x90.toByte(),
            0x01.toByte(),
            0xBB.toByte(),
            0x00,
            0x14,
            0x00,
            0x00,
        )

        processor.processPacket(
            buffer = packet,
            bytesRead = packet.size,
            forwardingPacketSink = { _, bytesRead ->
                forwardedPacketCount++
                forwardedBytes[0] = bytesRead
            },
        )

        assertEquals(1, forwardedPacketCount)
        assertEquals(packet.size, forwardedBytes[0])
    }
}
