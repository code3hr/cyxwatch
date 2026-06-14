package com.cyxwatch.app.domain.model

sealed interface AppInventoryChange {
    val packageName: String

    data class NewInstall(
        override val packageName: String,
        val installedProfile: AppProfile,
    ) : AppInventoryChange

    data class PermissionDelta(
        override val packageName: String,
        val addedPermissions: List<String>,
        val removedPermissions: List<String>,
    ) : AppInventoryChange
}

