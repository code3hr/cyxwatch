package com.cyxwatch.app.data.inventory

import com.cyxwatch.app.domain.model.AppProfile

interface AppInventoryRepository {
    fun fetchInstalledAppProfiles(): List<AppProfile>
}

