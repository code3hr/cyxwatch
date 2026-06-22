package com.cyxwatch.app.data.settings

import android.content.Context
import com.cyxwatch.app.data.EncryptedPreferencesStore

private const val UI_SETTINGS_PREF_NAME = "cyxwatch_ui_settings"
private const val KEY_THEME = "ui_theme_selection"
private const val KEY_LANGUAGE = "ui_language_selection"
private const val KEY_SCAN_SCHEDULE = "ui_scan_schedule"
private const val KEY_NOTIFICATIONS_ENABLED = "ui_notifications_enabled"

data class UserInterfaceSettingsState(
    val theme: String,
    val language: String,
    val scanSchedule: String,
    val notificationsEnabled: Boolean,
)

class UserInterfaceSettingsRepository(context: Context) {
    private val prefs = EncryptedPreferencesStore(
        context = context,
        preferenceFileName = UI_SETTINGS_PREF_NAME,
    )

    fun readState(): UserInterfaceSettingsState {
        return UserInterfaceSettingsState(
            theme = prefs.readString(KEY_THEME, "Dark"),
            language = prefs.readString(KEY_LANGUAGE, "English"),
            scanSchedule = prefs.readString(KEY_SCAN_SCHEDULE, "Every 24h"),
            notificationsEnabled = prefs.readBoolean(KEY_NOTIFICATIONS_ENABLED, true),
        )
    }

    fun setTheme(theme: String): UserInterfaceSettingsState {
        val updated = readState().copy(theme = theme)
        prefs.writeString(KEY_THEME, updated.theme)
        return updated
    }

    fun setLanguage(language: String): UserInterfaceSettingsState {
        val updated = readState().copy(language = language)
        prefs.writeString(KEY_LANGUAGE, updated.language)
        return updated
    }

    fun setScanSchedule(scanSchedule: String): UserInterfaceSettingsState {
        val updated = readState().copy(scanSchedule = scanSchedule)
        prefs.writeString(KEY_SCAN_SCHEDULE, updated.scanSchedule)
        return updated
    }

    fun setNotificationsEnabled(isEnabled: Boolean): UserInterfaceSettingsState {
        val updated = readState().copy(notificationsEnabled = isEnabled)
        prefs.writeBoolean(KEY_NOTIFICATIONS_ENABLED, updated.notificationsEnabled)
        return updated
    }
}
