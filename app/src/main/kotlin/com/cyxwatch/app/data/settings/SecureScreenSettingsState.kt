package com.cyxwatch.app.data.settings

data class SecureScreenSettingsState(
    val isEnabled: Boolean,
)

val DefaultSecureScreenSettingsState = SecureScreenSettingsState(
    isEnabled = false,
)