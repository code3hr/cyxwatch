package com.cyxwatch.app.domain

import com.cyxwatch.app.data.network.NetworkUsageTotalsRepository
import com.cyxwatch.app.domain.model.NetworkUsageDailyTotals
import com.cyxwatch.app.domain.model.NetworkUsageSummary

class CollectNetworkUsageEventsUseCase(
    private val permissionStateProvider: UsagePermissionStateProvider,
    private val basicNetworkUsageCollector: NetworkUsageCollector,
    private val vpnModeNetworkUsageCollector: NetworkUsageCollector,
    private val networkUsageTotalsRepository: NetworkUsageTotalsRepository,
) {
    fun collectLast24hNetworkUsageEvents(isVpnModeEnabled: Boolean): UsageCollectionResult {
        if (!permissionStateProvider.hasUsageAccess()) {
            return UsageCollectionResult.PermissionMissing
        }

        val summary = if (isVpnModeEnabled) {
            vpnModeNetworkUsageCollector.collectNetworkUsageSummary()
        } else {
            basicNetworkUsageCollector.collectNetworkUsageSummary()
        }
        networkUsageTotalsRepository.writeTotals(summary.toDailyTotals())
        return UsageCollectionResult.Success(summary.events)
    }

    private fun NetworkUsageSummary.toDailyTotals(): NetworkUsageDailyTotals {
        return NetworkUsageDailyTotals(
            dayStartEpochMs = windowStartEpochMs,
            totalBytes = totalBytes,
            totalBytesByNetworkType = totalBytesByNetworkType,
            totalBytesByPackageName = totalBytesByPackageName,
            persistedAtEpochMs = System.currentTimeMillis(),
        )
    }
}
