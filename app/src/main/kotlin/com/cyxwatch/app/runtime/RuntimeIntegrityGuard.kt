package com.cyxwatch.app.runtime

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.util.Log

private const val TAG = "RuntimeIntegrityGuard"

data class RuntimeIntegrityResult(
    val isAllowed: Boolean,
    val reasons: List<String>,
) {
    val isBlocked: Boolean get() = !isAllowed
}

class RuntimeIntegrityGuard(
    private val isBuildDebuggable: Boolean,
    private val isApplicationDebuggable: () -> Boolean = { false },
    private val isDebuggerConnected: () -> Boolean = { false },
    private val isAdbEnabled: () -> Boolean = { false },
) {
    fun canRunHighRiskAction(action: String): RuntimeIntegrityResult {
        val reasons = mutableListOf<String>()

        if (isBuildDebuggable) {
            return RuntimeIntegrityResult(
                isAllowed = true,
                reasons = reasons,
            )
        }

        if (isApplicationDebuggable()) {
            reasons.add("app is debuggable")
        }
        if (isDebuggerConnected()) {
            reasons.add("active debugger connection")
        }
        if (isAdbEnabled()) {
            reasons.add("ADB is enabled")
        }

        if (reasons.isNotEmpty()) {
            kotlin.runCatching {
                Log.w(TAG, "Blocking $action due to runtime integrity findings: $reasons")
            }
        }

        return RuntimeIntegrityResult(
            isAllowed = reasons.isEmpty(),
            reasons = reasons,
        )
    }

    companion object {
        fun create(context: Context, isBuildDebuggable: Boolean): RuntimeIntegrityGuard {
            val packageManager = context.packageManager
            val applicationInfo = runCatching {
                packageManager.getApplicationInfo(context.packageName, 0)
            }.getOrNull()

            return RuntimeIntegrityGuard(
                isBuildDebuggable = isBuildDebuggable,
                isApplicationDebuggable = {
                    applicationInfo?.let {
                        (it.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    } == true
                },
                isDebuggerConnected = { Debug.isDebuggerConnected() },
                isAdbEnabled = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        runCatching {
                            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED) == 1
                        }.getOrNull() == true
                    } else {
                        false
                    }
                },
            )
        }
    }
}
