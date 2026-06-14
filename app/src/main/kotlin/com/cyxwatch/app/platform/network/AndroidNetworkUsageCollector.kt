package com.cyxwatch.app.platform.network

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import com.cyxwatch.app.domain.NetworkUsageCollector
import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.NetworkUsageSummary
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import java.time.Instant

class AndroidNetworkUsageCollector(
    private val context: Context,
) : NetworkUsageCollector {
    private val packageManager = context.packageManager

    override fun collectNetworkUsageSummary(): NetworkUsageSummary {
        val now = System.currentTimeMillis()
        val start = now - MILLISECONDS_IN_24_HOURS
        return collectNetworkUsageSummaryBetween(start, now)
    }

    internal fun collectNetworkUsageSummaryBetween(
        startEpochMs: Long,
        endEpochMs: Long,
    ): NetworkUsageSummary {
        if (endEpochMs <= startEpochMs) {
            return emptySummary(startEpochMs, endEpochMs)
        }

        val networkStatsManager = context.getSystemService(
            Context.NETWORK_STATS_SERVICE,
        ) as NetworkStatsManager

        val bytesByPackage = mutableMapOf<String, Long>()
        val bytesByType = mutableMapOf<String, Long>()

        bytesByType[NETWORK_TYPE_WIFI] = collectBytesForNetworkType(
            networkStatsManager = networkStatsManager,
            networkType = wifiNetworkType(),
            subscriberId = "",
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            bytesByPackage = bytesByPackage,
        )

        bytesByType[NETWORK_TYPE_MOBILE] = collectBytesForNetworkType(
            networkStatsManager = networkStatsManager,
            networkType = mobileNetworkType(),
            subscriberId = "",
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            bytesByPackage = bytesByPackage,
        )

        val networkEvents = bytesByPackage
            .filter { it.value > 0 }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
            .map { (packageName, bytes) ->
                PrivacyEvent(
                    eventId = "network-$packageName-$startEpochMs-${bytes}B-${endEpochMs}",
                    timestamp = Instant.ofEpochMilli(endEpochMs),
                    packageName = packageName,
                    eventType = EventType.NETWORK_USAGE,
                    severity = resolveNetworkSeverity(bytes),
                    source = "NetworkStats",
                    title = "Network usage observed",
                    explanation = "Observed ${bytesLabel(bytes)} network usage for $packageName in the last 24h.",
                    evidenceJson = buildString {
                        append("{")
                        append("\"source\":\"network_summary\",")
                        append("\"packageName\":\"${packageName.jsonEscape()}\",")
                        append("\"startEpochMs\":$startEpochMs,")
                        append("\"endEpochMs\":$endEpochMs,")
                        append("\"packageBytes\":$bytes,")
                        append("\"networkTypeBytes\":")
                        append(buildNetworkTypeJson(bytesByType))
                        append("}")
                    },
                )
            }

        return NetworkUsageSummary(
            windowStartEpochMs = startEpochMs,
            windowEndEpochMs = endEpochMs,
            totalBytes = bytesByPackage.values.sum(),
            totalBytesByNetworkType = bytesByType.toMap(),
            totalBytesByPackageName = bytesByPackage.toMap(),
            events = networkEvents,
        )
    }

    private fun collectBytesForNetworkType(
        networkStatsManager: NetworkStatsManager,
        networkType: Int,
        subscriberId: String,
        startEpochMs: Long,
        endEpochMs: Long,
        bytesByPackage: MutableMap<String, Long>,
    ): Long {
        val stats = try {
            networkStatsManager.querySummary(networkType, subscriberId, startEpochMs, endEpochMs)
        } catch (_: Exception) {
            return 0L
        }

        val bucket = NetworkStats.Bucket()
        var totalBytes = 0L

        while (stats.hasNextBucket()) {
            stats.getNextBucket(bucket)
            val bucketBytes = maxOf(0L, bucket.rxBytes + bucket.txBytes)
            if (bucketBytes == 0L) {
                continue
            }

            totalBytes += bucketBytes
            val packages = resolvePackagesForUid(bucket.uid)
            if (packages.isEmpty()) {
                val uidPackageName = "uid_${bucket.uid}"
                bytesByPackage[uidPackageName] = (bytesByPackage[uidPackageName] ?: 0L) + bucketBytes
            } else {
                val bytesPerPackage = bucketBytes / packages.size
                val remainder = (bucketBytes % packages.size)
                packages.forEachIndexed { index, packageName ->
                    val packageBytes = bytesPerPackage + if (index == 0) remainder else 0L
                    bytesByPackage[packageName] = (bytesByPackage[packageName] ?: 0L) + packageBytes
                }
            }
        }

        return totalBytes
    }

    @Suppress("DEPRECATION")
    private fun wifiNetworkType(): Int = ConnectivityManager.TYPE_WIFI

    @Suppress("DEPRECATION")
    private fun mobileNetworkType(): Int = ConnectivityManager.TYPE_MOBILE

    private fun resolvePackagesForUid(uid: Int): List<String> {
        if (uid <= 0) return emptyList()
        val packages = runCatching {
            packageManager.getPackagesForUid(uid) ?: emptyArray()
        }.getOrNull() ?: emptyArray()

        return packages
            .asSequence()
            .filter { it.isNotBlank() }
            .sorted()
            .distinct()
            .toList()
    }

    private fun emptySummary(startEpochMs: Long, endEpochMs: Long): NetworkUsageSummary {
        return NetworkUsageSummary(
            windowStartEpochMs = startEpochMs,
            windowEndEpochMs = endEpochMs,
            totalBytes = 0L,
            totalBytesByNetworkType = emptyMap(),
            totalBytesByPackageName = emptyMap(),
            events = emptyList(),
        )
    }

    private fun resolveNetworkSeverity(bytes: Long): Severity {
        return when {
            bytes >= HIGH_NETWORK_USAGE_BYTES -> Severity.HIGH
            bytes >= MEDIUM_NETWORK_USAGE_BYTES -> Severity.MEDIUM
            else -> Severity.LOW
        }
    }

    private fun bytesLabel(bytes: Long): String {
        val bytesInMb = bytes.toDouble() / (1024.0 * 1024.0)
        return "%.2f MB".format(bytesInMb)
    }

    private fun buildNetworkTypeJson(bytesByType: Map<String, Long>): String {
        return buildString {
            append("{")
            append(
                bytesByType.entries
                    .sortedBy { it.key }
                    .joinToString(separator = ",") { (networkType, bytes) ->
                        "\"$networkType\":$bytes"
                    },
            )
            append("}")
        }
    }

    private fun String.jsonEscape(): String {
        return buildString {
            this@jsonEscape.forEach { ch ->
                when (ch) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (ch < ' ') {
                            append(String.format("\\u%04x", ch.code))
                        } else {
                            append(ch)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val MILLISECONDS_IN_24_HOURS = 24L * 60L * 60L * 1000L
        private const val MEDIUM_NETWORK_USAGE_BYTES = 10L * 1024L * 1024L
        private const val HIGH_NETWORK_USAGE_BYTES = 100L * 1024L * 1024L
        private const val NETWORK_TYPE_WIFI = "wifi"
        private const val NETWORK_TYPE_MOBILE = "mobile"
    }
}
