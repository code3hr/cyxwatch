package com.cyxwatch.app.domain

class CollectUsageEventsUseCase(
    private val permissionStateProvider: UsagePermissionStateProvider,
    private val usageEventCollector: UsageEventCollector,
) {
    fun collectLast24hUsageEvents(): UsageCollectionResult {
        if (!permissionStateProvider.hasUsageAccess()) {
            return UsageCollectionResult.PermissionMissing
        }

        val events = usageEventCollector.collectUsageEvents()
        return UsageCollectionResult.Success(events)
    }
}

