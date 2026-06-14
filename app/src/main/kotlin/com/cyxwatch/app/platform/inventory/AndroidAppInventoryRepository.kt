package com.cyxwatch.app.platform.inventory

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.cyxwatch.app.data.inventory.AppInventoryRepository
import com.cyxwatch.app.domain.model.AppProfile
import java.util.Locale

class AndroidAppInventoryRepository(
    context: Context,
) : AppInventoryRepository {
    private val packageManager = context.packageManager

    override fun fetchInstalledAppProfiles(): List<AppProfile> {
        val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)

        return installedPackages
            .mapNotNull { packageInfo ->
                mapToAppProfile(packageInfo)
            }
            .sortedWith(
                compareBy<AppProfile> { it.label.lowercase(Locale.getDefault()) }
                    .thenBy { it.packageName },
            )
    }

    private fun mapToAppProfile(packageInfo: PackageInfo): AppProfile? {
        val packageName = packageInfo.packageName
        val applicationInfo = packageInfo.applicationInfo ?: return null
        val label = packageManager.getApplicationLabel(applicationInfo).toString()
            .ifBlank { packageName }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            deprecatedVersionCode(packageInfo).toLong()
        }

        val permissions = packageInfo.requestedPermissions
            ?.toSet()
            ?.filterNotNull()
            ?.toList()
            ?.sorted()
            ?: emptyList()
        val isLaunchable = packageManager.getLaunchIntentForPackage(packageName) != null

        return AppProfile(
            packageName = packageName,
            label = label,
            versionName = packageInfo.versionName,
            versionCode = versionCode,
            firstInstallTimeEpochMs = packageInfo.firstInstallTime,
            lastUpdateTimeEpochMs = packageInfo.lastUpdateTime,
            isLaunchable = isLaunchable,
            permissions = permissions,
        )
    }

    @Suppress("DEPRECATION")
    private fun deprecatedVersionCode(packageInfo: PackageInfo): Int {
        return packageInfo.versionCode
    }
}
