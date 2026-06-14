package com.cyxwatch.app.platform.network

data class VpnTrafficDestination(
    val destinationIp: String,
    val destinationPort: Int?,
    val protocol: String,
    val bytes: Long,
    val packetCount: Int,
)

data class VpnTrafficTotals(
    val totalPackets: Long,
    val totalBytes: Long,
    val uniqueDestinationCount: Int,
    val parsedPackets: Long,
    val unparsedPackets: Long,
    val forwardingEnabled: Boolean,
    val captureMode: String,
)

class VpnModeTrafficStore {
    private val lock = Any()
    private val destinationTotals = HashMap<String, Accumulator>()
    private var totalPackets = 0L
    private var totalBytes = 0L
    private var unparsedPackets = 0L
    private var forwardingEnabled: Boolean = false

    fun setForwardingEnabled(enabled: Boolean) {
        synchronized(lock) {
            forwardingEnabled = enabled
        }
    }

    fun recordPacket(metadata: VpnPacketMetadata) {
        if (metadata.packetBytes <= 0L) {
            return
        }
        val key = endpointKey(metadata)
        synchronized(lock) {
            totalPackets++
            totalBytes += metadata.packetBytes
            val accumulator = destinationTotals.getOrPut(key) {
                Accumulator(
                    destinationIp = metadata.destinationIp,
                    destinationPort = metadata.destinationPort,
                    protocol = metadata.protocol,
                )
            }
            accumulator.bytes += metadata.packetBytes
            accumulator.packetCount++
        }
    }

    fun recordUnparsedPacket(packetBytes: Long) {
        synchronized(lock) {
            totalPackets++
            if (packetBytes > 0L) {
                totalBytes += packetBytes
            }
            unparsedPackets++
        }
    }

    fun snapshotTotals(): VpnTrafficTotals {
        return synchronized(lock) {
            val parsedPackets = (totalPackets - unparsedPackets).coerceAtLeast(0L)
            VpnTrafficTotals(
                totalPackets = totalPackets,
                totalBytes = totalBytes,
                uniqueDestinationCount = destinationTotals.size,
                parsedPackets = parsedPackets,
                unparsedPackets = unparsedPackets,
                forwardingEnabled = forwardingEnabled,
                captureMode = if (forwardingEnabled) "forwarding" else "monitor-only",
            )
        }
    }

    fun snapshotTopDestinations(limit: Int = DEFAULT_DESTINATION_LIMIT): List<VpnTrafficDestination> {
        if (limit <= 0) return emptyList()
        return synchronized(lock) {
            destinationTotals.values
                .sortedWith(
                    compareByDescending<Accumulator> { it.bytes }
                        .thenByDescending { it.packetCount }
                        .thenBy { it.destinationKey() },
                )
                .take(limit)
                .map { accumulator ->
                    VpnTrafficDestination(
                        destinationIp = accumulator.destinationIp,
                        destinationPort = accumulator.destinationPort,
                        protocol = accumulator.protocol,
                        bytes = accumulator.bytes,
                        packetCount = accumulator.packetCount,
                    )
                }
        }
    }

    internal fun clear() {
        synchronized(lock) {
            totalPackets = 0L
            totalBytes = 0L
            unparsedPackets = 0L
            destinationTotals.clear()
        }
    }

    private fun endpointKey(metadata: VpnPacketMetadata): String {
        return "${metadata.destinationIp}|${metadata.destinationPort ?: 0}|${metadata.protocol}"
    }

    private data class Accumulator(
        val destinationIp: String,
        val destinationPort: Int?,
        val protocol: String,
    ) {
        var bytes = 0L
        var packetCount = 0

        fun destinationKey(): String {
            return "$destinationIp:$destinationPort:$protocol"
        }
    }

    companion object {
        val shared = VpnModeTrafficStore()
        private const val DEFAULT_DESTINATION_LIMIT = 6
    }
}
