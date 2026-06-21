package com.cyxwatch.app

import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        val parsedAction = LaunchActionParser.parse(rawIntent.extras)
        if (parsedAction == null) {
            if (rawIntent.extras != null) {
                Log.w(TAG, "Ignoring malformed or unsupported launch action extras.")
            }
            return null
        }

        val targetPackageName = parsedAction.targetPackageName
        if (rawIntent.extras?.containsKey(EXTRA_ALERT_RULE) == true) {
            Log.d(
                TAG,
                "Parsed launch action for package=$targetPackageName rule=${parsedAction.targetRule}",
            )
        } else {
            Log.d(TAG, "Parsed launch action for package=$targetPackageName")
        }

        return parsedAction
    }

    companion object {
        private const val TAG = "CyxWatch"
    }
}
