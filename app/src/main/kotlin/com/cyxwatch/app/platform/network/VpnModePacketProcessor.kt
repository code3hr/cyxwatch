package com.cyxwatch.app.platform.network

class VpnModePacketProcessor(
    private val trafficStore: VpnModeTrafficStore,
    private val packetParser: VpnPacketParser = VpnPacketParser,
) {
    fun processPacket(
        buffer: ByteArray,
        bytesRead: Int,
        forwardingPacketSink: ((ByteArray, Int) -> Unit)? = null,
    ) {
        if (bytesRead <= 0) {
            return
        }

        packetParser.parsePacket(buffer, bytesRead)?.let { metadata ->
            trafficStore.recordPacket(metadata)
        } ?: trafficStore.recordUnparsedPacket(packetBytes = bytesRead.toLong())

        forwardingPacketSink?.invoke(buffer, bytesRead)
    }
}
