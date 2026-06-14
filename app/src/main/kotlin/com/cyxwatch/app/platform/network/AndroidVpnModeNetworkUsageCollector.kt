package com.cyxwatch.app.platform.network

import com.cyxwatch.app.domain.NetworkUsageCollector
import com.cyxwatch.app.domain.model.NetworkUsageSummary
import com.cyxwatch.app.domain.model.PrivacyEvent
import java.util.Locale

class AndroidVpnModeNetworkUsageCollector(
    private val delegateCollector: NetworkUsageCollector,
    private val trafficStore: VpnModeTrafficStore = VpnModeTrafficStore.shared,
) : NetworkUsageCollector {
    override fun collectNetworkUsageSummary(): NetworkUsageSummary {
        val summary = delegateCollector.collectNetworkUsageSummary()
        return summary.copy(
            events = summary.events.map(::enrichVpnModeMetadata),
        )
    }

    private fun enrichVpnModeMetadata(event: PrivacyEvent): PrivacyEvent {
        val trafficStats = trafficStore.snapshotTotals()
        return event.copy(
            eventId = "${event.eventId}-vpn",
            source = "VpnMode",
            title = "Advanced network usage observed",
            explanation = event.explanation,
            evidenceJson = event.evidenceJson.withVpnModeMetadata(
                trafficDestinations = trafficStore.snapshotTopDestinations(),
                trafficTotals = trafficStats,
            ),
        )
    }

    private fun String.withVpnModeMetadata(
        trafficDestinations: List<VpnTrafficDestination>,
        trafficTotals: VpnTrafficTotals,
    ): String {
        val trimmed = trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return this
        }

        val trafficMetadata = buildTrafficMetadataJson(trafficDestinations)
        val trafficTotalsMetadata = buildTrafficTotalsMetadataJson(trafficTotals)
        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        return """{$inner${if (inner.isBlank()) "" else ","}"collectionMode":"vpn","trafficTotals":$trafficTotalsMetadata,"trafficDestinations":$trafficMetadata}"""
    }

    private fun buildTrafficTotalsMetadataJson(trafficTotals: VpnTrafficTotals): String {
        return buildString {
            append("{")
            append("\"packetsObserved\":${trafficTotals.totalPackets},")
            append("\"bytesObserved\":${trafficTotals.totalBytes},")
            append("\"uniqueDestinations\":${trafficTotals.uniqueDestinationCount},")
            append("\"parsedPackets\":${trafficTotals.parsedPackets},")
            append("\"unparsedPackets\":${trafficTotals.unparsedPackets},")
            append("\"captureMode\":\"${trafficTotals.captureMode}\",")
            append("\"forwardingEnabled\":${trafficTotals.forwardingEnabled}")
            append("}")
        }
    }

    private fun buildTrafficMetadataJson(trafficDestinations: List<VpnTrafficDestination>): String {
        if (trafficDestinations.isEmpty()) {
            return "[]"
        }

        return trafficDestinations.joinToString(
            separator = ",",
            prefix = "[",
            postfix = "]",
        ) { destination ->
            buildString {
                append("{")
                append("\"destinationIp\":\"${destination.destinationIp}\",")
                append("\"protocol\":\"${destination.protocol}\",")
                if (destination.destinationPort != null) {
                    append("\"destinationPort\":${destination.destinationPort},")
                } else {
                    append("\"destinationPort\":null,")
                }
                append("\"bytes\":${destination.bytes},")
                append("\"packetCount\":${destination.packetCount},")
                append("\"avgBytesPerPacket\":${formatAverageBytes(destination.bytes, destination.packetCount)}")
                append("}")
            }
        }
    }

    private fun formatAverageBytes(bytes: Long, packetCount: Int): String {
        if (packetCount <= 0) return "0"
        return String.format(Locale.US, "%.2f", bytes.toDouble() / packetCount.toDouble())
    }
}
