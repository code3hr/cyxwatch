package com.cyxwatch.app.data.settings

import android.content.Context

private const val PREF_NAME = "cyxwatch_consent_prefs"
private const val KEY_EVER_GRANTED = "usage_access_ever_granted"
private const val KEY_EVER_DENIED = "usage_access_ever_denied"
private const val KEY_CHECK_COUNT = "usage_access_check_count"
private const val KEY_DENIED_COUNT = "usage_access_denied_count"
private const val KEY_LAST_CHECKED_AT = "usage_access_last_checked_at"
private const val KEY_LAST_SETTINGS_OPENED_AT = "usage_access_last_settings_opened_at"
private const val PREF_LONG_UNSET = -1L

class UsageAccessConsentRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun readState(): UsageAccessConsentState {
        return DefaultUsageAccessConsentState.copy(
            hasEverGranted = prefs.getBoolean(KEY_EVER_GRANTED, false),
            hasEverDenied = prefs.getBoolean(KEY_EVER_DENIED, false),
            checkCount = prefs.getInt(KEY_CHECK_COUNT, 0),
            deniedCount = prefs.getInt(KEY_DENIED_COUNT, 0),
            lastCheckedAtEpochMs = readLongOrNull(KEY_LAST_CHECKED_AT),
            lastSettingsOpenedAtEpochMs = readLongOrNull(KEY_LAST_SETTINGS_OPENED_AT),
        )
    }

    fun recordCheckResult(granted: Boolean, checkedAtEpochMs: Long) {
        val current = readState()
        val updated = current.copyOnCheck(granted, checkedAtEpochMs)
        prefs.edit()
            .putBoolean(KEY_EVER_GRANTED, updated.hasEverGranted)
            .putBoolean(KEY_EVER_DENIED, updated.hasEverDenied)
            .putInt(KEY_CHECK_COUNT, updated.checkCount)
            .putInt(KEY_DENIED_COUNT, updated.deniedCount)
            .putLong(KEY_LAST_CHECKED_AT, updated.lastCheckedAtEpochMs ?: PREF_LONG_UNSET)
            .putLong(
                KEY_LAST_SETTINGS_OPENED_AT,
                current.lastSettingsOpenedAtEpochMs ?: PREF_LONG_UNSET,
            )
            .apply()
    }

    fun recordSettingsOpened(openedAtEpochMs: Long) {
        val updated = readState().copyOnOpenSettings(openedAtEpochMs)
        prefs.edit()
            .putLong(KEY_LAST_SETTINGS_OPENED_AT, updated.lastSettingsOpenedAtEpochMs ?: PREF_LONG_UNSET)
            .apply()
    }

    private fun readLongOrNull(key: String): Long? {
        val value = prefs.getLong(key, PREF_LONG_UNSET)
        return if (value == PREF_LONG_UNSET) {
            null
        } else {
            value
        }
    }
}
