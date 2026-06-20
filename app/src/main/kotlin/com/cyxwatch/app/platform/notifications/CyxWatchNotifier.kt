package com.cyxwatch.app.platform.notifications

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cyxwatch.app.MainActivity
import com.cyxwatch.app.domain.PrivacyAlert
import kotlin.math.abs

private const val CHANNEL_ID = "cyxwatch_warnings"
private const val CHANNEL_NAME = "CyxWatch Permission Warnings"
private const val CHANNEL_DESCRIPTION = "Warning notifications for sensitive permission changes"
private const val NOTIFICATION_ID_BASE = 8100
private const val NOTIFICATION_ID_ACCESS_WARNING = 9200
const val EXTRA_ALERT_PACKAGE = "cyxwatch_open_package"
const val EXTRA_ALERT_RULE = "cyxwatch_open_rule"

class CyxWatchNotifier(
    private val context: Context,
) {
    @SuppressLint("MissingPermission")
    fun postPermissionWarning(alert: PrivacyAlert, appDisplayName: String) {
        if (!hasPostNotificationPermission()) {
            return
        }

        createChannel()
        val title = "New sensitive-permission risk"
        val contentText = "$appDisplayName requires attention."
        val packageName = alert.packageName

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ALERT_PACKAGE, packageName)
            putExtra(EXTRA_ALERT_RULE, alert.rule.name)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationRequestCode(alert),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$contentText ${alert.message}")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        NotificationManagerCompat.from(context)
            .notify(notificationId(alert), notification)
    }

    @SuppressLint("MissingPermission")
    fun postUsageAccessWarning() {
        if (!hasPostNotificationPermission()) {
            return
        }

        createChannel()
        val title = "CyxWatch access is paused"
        val contentText = "Usage Access was revoked or disabled. Open settings to restore observations."

        val settingsIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9910,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID_ACCESS_WARNING, notification)
    }

    private fun hasPostNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }

    private fun notificationRequestCode(alert: PrivacyAlert): Int {
        return abs((alert.packageName.hashCode() * 31) + alert.rule.ordinal + 1)
    }

    private fun notificationId(alert: PrivacyAlert): Int {
        return NOTIFICATION_ID_BASE + abs(alert.packageName.hashCode() % 1000) + alert.rule.ordinal
    }
}
