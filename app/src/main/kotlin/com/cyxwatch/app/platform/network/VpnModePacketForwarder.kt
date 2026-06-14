package com.cyxwatch.app.platform.network

interface VpnModePacketForwarder {
    fun forwardPacket(packet: ByteArray, bytesRead: Int)
}
