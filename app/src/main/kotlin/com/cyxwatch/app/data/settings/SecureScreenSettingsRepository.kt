package com.cyxwatch.app.data.settings

import android.content.Context
import com.cyxwatch.app.data.EncryptedPreferencesStore

private const val PREF_NAME = "cyxwatch_secure_screen_settings"
private const val KEY_SECURE_SCREEN_MODE_ENABLED = "secure_screen_mode_enabled"

class SecureScreenSettingsRepository(context: Context) {
    private val prefs = EncryptedPreferencesStore(
        context = context,
        preferenceFileName = PREF_NAME,
    )

    fun readState(): SecureScreenSettingsState {
        return DefaultSecureScreenSettingsState.copy(
            isEnabled = prefs.readBoolean(KEY_SECURE_SCREEN_MODE_ENABLED, false),
        )
    }

    fun setEnabled(enabled: Boolean): SecureScreenSettingsState {
        prefs.writeBoolean(KEY_SECURE_SCREEN_MODE_ENABLED, enabled)
        return DefaultSecureScreenSettingsState.copy(isEnabled = enabled)
    }
}
