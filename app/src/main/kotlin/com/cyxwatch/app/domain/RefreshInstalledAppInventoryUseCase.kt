package com.cyxwatch.app.domain

import com.cyxwatch.app.data.inventory.AppInventorySnapshotRepository
import com.cyxwatch.app.domain.model.AppInventoryChange
import com.cyxwatch.app.domain.model.AppProfile
import com.cyxwatch.app.domain.model.PrivacyEvent
import java.time.Instant

data class InventoryRefreshResult(
    val currentProfiles: List<AppProfile>,
    val inventoryChanges: List<AppInventoryChange>,
    val inventoryEvents: List<PrivacyEvent>,
)

class RefreshInstalledAppInventoryUseCase(
    private val readInstalledAppInventoryUseCase: ReadInstalledAppInventoryUseCase,
    private val snapshotRepository: AppInventorySnapshotRepository,
    private val detectAppInventoryChangesUseCase: DetectAppInventoryChangesUseCase,
    private val buildInventoryChangeEventsUseCase: BuildInventoryChangeEventsUseCase,
) {
    fun refresh(): InventoryRefreshResult {
        val currentProfiles = readInstalledAppInventoryUseCase.readInstalledAppProfiles()
        val previousProfiles = snapshotRepository.readSnapshot()
        val changes = detectAppInventoryChangesUseCase.detectChanges(
            currentProfiles = currentProfiles,
            previousProfiles = previousProfiles,
        )
        val events = buildInventoryChangeEventsUseCase.buildEvents(
            changes = changes,
            at = Instant.ofEpochMilli(System.currentTimeMillis()),
        )
        snapshotRepository.writeSnapshot(currentProfiles)

        return InventoryRefreshResult(
            currentProfiles = currentProfiles,
            inventoryChanges = changes,
            inventoryEvents = events,
        )
    }
}

