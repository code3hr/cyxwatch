package com.cyxwatch.app.data.network

import android.content.Context
import com.cyxwatch.app.domain.model.NetworkUsageDailyTotals

private const val NETWORK_USAGE_PREFERENCES_FILE = "cyxwatch_network_usage"
private const val NETWORK_USAGE_LATEST_KEY = "latest_daily_totals"
private const val FIELD_SEPARATOR = "|"
private const val PACKAGE_SEPARATOR = ","
private const val PACKAGE_BYTES_SEPARATOR = ":"

class SharedPrefsNetworkUsageTotalsRepository(
    context: Context,
) : NetworkUsageTotalsRepository {
    private val sharedPreferences = context.getSharedPreferences(
        NETWORK_USAGE_PREFERENCES_FILE,
        Context.MODE_PRIVATE,
    )

    override fun readLatestTotals(): NetworkUsageDailyTotals? {
        val raw = sharedPreferences.getString(NETWORK_USAGE_LATEST_KEY, null) ?: return null
        return deserialize(raw)
    }

    override fun writeTotals(totals: NetworkUsageDailyTotals) {
        sharedPreferences.edit()
            .putString(NETWORK_USAGE_LATEST_KEY, serialize(totals))
            .apply()
    }

    private fun serialize(totals: NetworkUsageDailyTotals): String {
        return buildString {
            append(totals.dayStartEpochMs)
            append(FIELD_SEPARATOR)
            append(totals.totalBytes)
            append(FIELD_SEPARATOR)
            append(totals.totalBytesByNetworkType["wifi"] ?: 0L)
            append(FIELD_SEPARATOR)
            append(totals.totalBytesByNetworkType["mobile"] ?: 0L)
            append(FIELD_SEPARATOR)
            append(totals.persistedAtEpochMs)
            append(FIELD_SEPARATOR)
            append(
                totals.totalBytesByPackageName
                    .entries
                    .sortedBy { it.key }
                    .joinToString(PACKAGE_SEPARATOR) { (packageName, bytes) ->
                        "$packageName$PACKAGE_BYTES_SEPARATOR$bytes"
                    },
            )
        }
    }

    private fun deserialize(raw: String): NetworkUsageDailyTotals? {
        val parts = raw.split(FIELD_SEPARATOR, limit = 6)
        if (parts.size != 6) return null

        val dayStartEpochMs = parts[0].toLongOrNull() ?: return null
        val totalBytes = parts[1].toLongOrNull() ?: return null
        val wifiBytes = parts[2].toLongOrNull() ?: return null
        val mobileBytes = parts[3].toLongOrNull() ?: return null
        val persistedAtEpochMs = parts[4].toLongOrNull() ?: return null
        val packageSection = parts[5]

        val packageTotals = if (packageSection.isBlank()) {
            emptyMap()
        } else {
            packageSection.split(PACKAGE_SEPARATOR)
                .mapNotNull { entry ->
                    val entryParts = entry.split(PACKAGE_BYTES_SEPARATOR, limit = 2)
                    if (entryParts.size != 2) return@mapNotNull null
                    val packageName = entryParts[0]
                    val bytes = entryParts[1].toLongOrNull() ?: return@mapNotNull null
                    if (packageName.isBlank()) return@mapNotNull null
                    packageName to bytes
                }
                .toMap()
        }

        return NetworkUsageDailyTotals(
            dayStartEpochMs = dayStartEpochMs,
            totalBytes = totalBytes,
            totalBytesByNetworkType = mapOf(
                "wifi" to wifiBytes,
                "mobile" to mobileBytes,
            ),
            totalBytesByPackageName = packageTotals,
            persistedAtEpochMs = persistedAtEpochMs,
        )
    }
}

