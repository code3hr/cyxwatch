package com.cyxwatch.app

import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cyxwatch.app.domain.ScoringRule
import com.cyxwatch.app.platform.notifications.EXTRA_ALERT_PACKAGE
import com.cyxwatch.app.platform.notifications.EXTRA_ALERT_RULE

class MainActivity : ComponentActivity() {
    private val pendingLaunchAction = mutableStateOf<LaunchAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingLaunchAction.value = parseLaunchAction(intent)
        enableEdgeToEdge()
        setContent {
            CyxWatchApp(
                pendingLaunchAction = pendingLaunchAction.value,
                consumePendingLaunchAction = { pendingLaunchAction.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingLaunchAction.value = parseLaunchAction(intent)
    }

    private fun parseLaunchAction(rawIntent: Intent): LaunchAction? {
        val extras = rawIntent.extras ?: return null

        val targetPackageName = extras.getString(EXTRA_ALERT_PACKAGE)
            ?.trim()
            ?.takeIf { isValidPackageName(it) }
            ?: run {
                if (extras.containsKey(EXTRA_ALERT_PACKAGE)) {
                    Log.w(TAG, "Ignoring launch extra because package name is missing or invalid.")
                }
                return null
            }

        val targetRule = extras.getString(EXTRA_ALERT_RULE)
            ?.trim()
            ?.let { rawRuleName ->
                runCatching { ScoringRule.valueOf(rawRuleName) }
                    .onFailure { throwable ->
                        Log.w(TAG, "Ignoring invalid alert rule '$rawRuleName' for launch action.", throwable)
                    }
                    .getOrNull()
            }

        return LaunchAction(
            targetPackageName = targetPackageName,
            targetRule = targetRule,
        )
    }

    private fun isValidPackageName(value: String): Boolean {
        return PACKAGE_NAME_PATTERN.matches(value)
    }

    companion object {
        private const val TAG = "CyxWatch"
        private val PACKAGE_NAME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
    }
}
