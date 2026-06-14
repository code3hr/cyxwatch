package com.cyxwatch.app.domain

import com.cyxwatch.app.data.inventory.AppInventoryRepository
import com.cyxwatch.app.domain.model.AppProfile

class ReadInstalledAppInventoryUseCase(
    private val appInventoryRepository: AppInventoryRepository,
) {
    fun readInstalledAppProfiles(): List<AppProfile> {
        return appInventoryRepository.fetchInstalledAppProfiles()
    }
}

