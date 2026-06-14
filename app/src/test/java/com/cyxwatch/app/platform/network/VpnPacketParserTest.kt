package com.cyxwatch.app.platform.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VpnPacketParserTest {
    @Test
    fun `parses ipv4 udp packet metadata`() {
        val packet = byteArrayOf(
            0x45, 0x00, 0x00.toByte(), 0x28.toByte(),
            0x00, 0x00, 0x00, 0x00,
            0x40, 0x11.toByte(), 0x00, 0x00,
            0x0A, 0x00, 0x00, 0x01,
            (203).toByte(), 0x00, 0x71.toByte(), 0x0A,
            0x1F, 0x90.toByte(), 0x01.toByte(), 0xBB.toByte(),
            0x00, 0x14, 0x00, 0x00,
        )
        val metadata = VpnPacketParser.parsePacket(packet, 28)

        assertEquals("203.0.113.10", metadata?.destinationIp)
        assertEquals(443, metadata?.destinationPort)
        assertEquals("UDP", metadata?.protocol)
        assertEquals(28L, metadata?.packetBytes)
    }

    @Test
    fun `parses ipv4 tcp packet metadata`() {
        val packet = byteArrayOf(
            0x45, 0x00, 0x00.toByte(), 0x28.toByte(),
            0x00, 0x00, 0x00, 0x00,
            0x40, 0x06.toByte(), 0x00, 0x00,
            0x0A, 0x00, 0x00, 0x01,
            (198).toByte(), 51.toByte(), 100, 0x20,
            0x1F, 0x90.toByte(), 0x01.toByte(), 0xBB.toByte(),
            0x00, 0x14, 0x00, 0x00,
        )
        val metadata = VpnPacketParser.parsePacket(packet, 28)

        assertEquals("198.51.100.32", metadata?.destinationIp)
        assertEquals(443, metadata?.destinationPort)
        assertEquals("TCP", metadata?.protocol)
        assertEquals(28L, metadata?.packetBytes)
    }

    @Test
    fun `returns null for short packet`() {
        val packet = byteArrayOf(0x45, 0x00, 0x00)
        assertNull(VpnPacketParser.parsePacket(packet, 3))
    }

}
