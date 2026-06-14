package com.cyxwatch.app.platform.network

class NoopVpnModePacketForwarder : VpnModePacketForwarder {
    override fun forwardPacket(packet: ByteArray, bytesRead: Int) {
        // Intentionally no-op in V1.
    }
}
