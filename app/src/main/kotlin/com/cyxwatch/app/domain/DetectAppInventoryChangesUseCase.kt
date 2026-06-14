package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.AppInventoryChange
import com.cyxwatch.app.domain.model.AppProfile

class DetectAppInventoryChangesUseCase {
    fun detectChanges(
        currentProfiles: List<AppProfile>,
        previousProfiles: List<AppProfile>,
    ): List<AppInventoryChange> {
        val previousByPackage = previousProfiles.associateBy { it.packageName }

        val changes = mutableListOf<AppInventoryChange>()
        val orderedCurrent = currentProfiles.sortedBy { it.packageName }

        for (current in orderedCurrent) {
            val previous = previousByPackage[current.packageName]

            if (previous == null) {
                changes.add(AppInventoryChange.NewInstall(current.packageName, current))
                continue
            }

            val previousPermissions = previous.permissions.toSet()
            val currentPermissions = current.permissions.toSet()
            val addedPermissions = currentPermissions.minus(previousPermissions).sorted()
            val removedPermissions = previousPermissions.minus(currentPermissions).sorted()

            if (addedPermissions.isNotEmpty() || removedPermissions.isNotEmpty()) {
                changes.add(
                    AppInventoryChange.PermissionDelta(
                        packageName = current.packageName,
                        addedPermissions = addedPermissions,
                        removedPermissions = removedPermissions,
                    ),
                )
            }
        }

        return changes
    }
}

