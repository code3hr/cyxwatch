package com.cyxwatch.app.platform.permissions

import android.content.Context
import com.cyxwatch.app.domain.UsagePermissionStateProvider

class UsageAccessPermissionStateProvider(
    private val context: Context,
) : UsagePermissionStateProvider {
    override fun hasUsageAccess(): Boolean {
        return hasUsageAccess(context)
    }
}

