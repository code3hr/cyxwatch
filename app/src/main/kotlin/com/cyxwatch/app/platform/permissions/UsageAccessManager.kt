package com.cyxwatch.app.platform.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

@Suppress("DEPRECATION")
fun hasUsageAccess(context: Context): Boolean {
    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun openUsageAccessSettingsIntent(): Intent {
    return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).also {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
