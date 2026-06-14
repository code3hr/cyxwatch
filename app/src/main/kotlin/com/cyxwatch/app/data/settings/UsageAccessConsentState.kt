package com.cyxwatch.app.data.settings

data class UsageAccessConsentState(
    val hasEverGranted: Boolean,
    val hasEverDenied: Boolean,
    val checkCount: Int,
    val deniedCount: Int,
    val lastCheckedAtEpochMs: Long?,
    val lastSettingsOpenedAtEpochMs: Long?,
) {
    fun copyOnCheck(granted: Boolean, checkedAtEpochMs: Long): UsageAccessConsentState {
        return copy(
            hasEverGranted = hasEverGranted || granted,
            hasEverDenied = hasEverDenied || !granted,
            checkCount = checkCount + 1,
            deniedCount = deniedCount + if (granted) 0 else 1,
            lastCheckedAtEpochMs = checkedAtEpochMs,
        )
    }

    fun copyOnOpenSettings(openedAtEpochMs: Long): UsageAccessConsentState {
        return copy(lastSettingsOpenedAtEpochMs = openedAtEpochMs)
    }
}

val DefaultUsageAccessConsentState = UsageAccessConsentState(
    hasEverGranted = false,
    hasEverDenied = false,
    checkCount = 0,
    deniedCount = 0,
    lastCheckedAtEpochMs = null,
    lastSettingsOpenedAtEpochMs = null,
)
