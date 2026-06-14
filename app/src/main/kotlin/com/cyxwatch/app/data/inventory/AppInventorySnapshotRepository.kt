package com.cyxwatch.app.data.inventory

import com.cyxwatch.app.domain.model.AppProfile

interface AppInventorySnapshotRepository {
    fun readSnapshot(): List<AppProfile>
    fun writeSnapshot(profiles: List<AppProfile>)
}

