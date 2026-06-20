package com.cyxwatch.app.data.settings

import android.content.Context
import com.cyxwatch.app.data.EncryptedPreferencesStore
import com.cyxwatch.app.domain.RetentionPolicy
import com.cyxwatch.app.domain.RetentionSettings

private const val RETENTION_PREF_NAME = "cyxwatch_retention_prefs"
private const val KEY_RETENTION_DAYS = "retention_days"

class RetentionSettingsRepository(context: Context) {
    private val prefs = EncryptedPreferencesStore(
        context = context,
        preferenceFileName = RETENTION_PREF_NAME,
    )
    private val retentionPolicy = RetentionPolicy()

    fun readSettings(): RetentionSettings {
        val days = prefs.readInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS)
        return RetentionSettings(
            retentionDays = retentionPolicy.normalizeRetentionDays(days),
        )
    }

    fun writeRetentionDays(days: Int): RetentionSettings {
        val normalizedDays = retentionPolicy.normalizeRetentionDays(days)
        prefs.writeInt(KEY_RETENTION_DAYS, normalizedDays)
        return RetentionSettings(retentionDays = normalizedDays)
    }

    private companion object {
        private const val DEFAULT_RETENTION_DAYS = 14
    }
}

