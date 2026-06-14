package com.cyxwatch.app.platform.network

object VpnPacketParser {
    private const val IPV4_VERSION = 4
    private const val IPV6_VERSION = 6
    private const val IPV4_MIN_HEADER_BYTES = 20
    private const val IPV6_MIN_HEADER_BYTES = 40
    private const val IPV6_BASE_HEADER_BYTES = 40
    private const val PROTOCOL_TCP = 6
    private const val PROTOCOL_UDP = 17

    private val ipv6ExtensionHeaders = setOf(
        0, 43, 44, 50, 51, 60, 135, 139, 140, 253, 254,
    )

    fun parsePacket(packet: ByteArray, length: Int = packet.size): VpnPacketMetadata? {
        if (length <= 0 || length > packet.size) {
            return null
        }

        val version = (packet[0].toInt() ushr 4) and 0x0F
        return when (version) {
            IPV4_VERSION -> parseIpv4Packet(packet, length)
            IPV6_VERSION -> parseIpv6Packet(packet, length)
            else -> null
        }
    }

    private fun parseIpv4Packet(packet: ByteArray, length: Int): VpnPacketMetadata? {
        if (length < IPV4_MIN_HEADER_BYTES) {
            return null
        }

        val firstByte = packet[0].toInt()
        val ihlBytes = (firstByte and 0x0F) * 4
        if (ihlBytes < IPV4_MIN_HEADER_BYTES || ihlBytes + 4 > length) {
            return null
        }

        val protocol = packet[9].toInt() and 0xFF
        val destinationIp = buildString {
            append(packet[16].toInt() and 0xFF)
            append('.')
            append(packet[17].toInt() and 0xFF)
            append('.')
            append(packet[18].toInt() and 0xFF)
            append('.')
            append(packet[19].toInt() and 0xFF)
        }

        val destinationPort = if (
            (protocol == PROTOCOL_TCP || protocol == PROTOCOL_UDP) &&
            ihlBytes + 4 <= length
        ) {
            parseDestinationPort(packet, ihlBytes)
        } else {
            null
        }

        return VpnPacketMetadata(
            destinationIp = destinationIp,
            destinationPort = destinationPort,
            protocol = protocolName(protocol),
            packetBytes = length.toLong(),
        )
    }

    private fun parseIpv6Packet(packet: ByteArray, length: Int): VpnPacketMetadata? {
        if (length < IPV6_MIN_HEADER_BYTES) {
            return null
        }

        val destinationIp = buildString {
            for (index in 24 until 40 step 2) {
                if (index > 24) append(':')
                val segment = ((packet[index].toInt() and 0xFF) shl 8) or
                    (packet[index + 1].toInt() and 0xFF)
                append(segment.toString(16))
            }
        }

        var protocol = packet[6].toInt() and 0xFF
        var transportOffset = IPV6_BASE_HEADER_BYTES
        while (protocol in ipv6ExtensionHeaders && length >= transportOffset + 2) {
            val nextHeader = packet[transportOffset].toInt() and 0xFF
            val headerLength = ((packet[transportOffset + 1].toInt() and 0xFF) + 1) * 8
            protocol = nextHeader
            transportOffset += headerLength
            if (transportOffset + 1 > length) {
                return null
            }
        }

        if (transportOffset + 3 >= length) {
            return null
        }

        val destinationPort = if (protocol == PROTOCOL_TCP || protocol == PROTOCOL_UDP) {
            parseDestinationPort(packet, transportOffset)
        } else {
            null
        }

        return VpnPacketMetadata(
            destinationIp = destinationIp,
            destinationPort = destinationPort,
            protocol = protocolName(protocol),
            packetBytes = length.toLong(),
        )
    }

    private fun parseDestinationPort(packet: ByteArray, transportOffset: Int): Int {
        return ((packet[transportOffset + 2].toInt() and 0xFF) shl 8) or
            (packet[transportOffset + 3].toInt() and 0xFF)
    }

    private fun protocolName(protocol: Int): String {
        return when (protocol) {
            PROTOCOL_TCP -> "TCP"
            PROTOCOL_UDP -> "UDP"
            else -> "OTHER($protocol)"
        }
    }
}
