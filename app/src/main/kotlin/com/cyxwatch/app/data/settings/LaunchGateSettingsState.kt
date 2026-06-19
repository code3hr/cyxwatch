package com.cyxwatch.app.data.settings

data class LaunchGateSettingsState(
    val hasCompletedLaunchGate: Boolean,
    val hasOpenedPrivacyControlsFromGate: Boolean,
    val completedAtEpochMs: Long?,
) {
    fun withCompleted(
        openedPrivacyControlsFromGate: Boolean,
        completedAtEpochMs: Long,
    ): LaunchGateSettingsState {
        return copy(
            hasCompletedLaunchGate = true,
            hasOpenedPrivacyControlsFromGate = hasOpenedPrivacyControlsFromGate || openedPrivacyControlsFromGate,
            completedAtEpochMs = completedAtEpochMs,
        )
    }
}

val DefaultLaunchGateSettingsState = LaunchGateSettingsState(
    hasCompletedLaunchGate = false,
    hasOpenedPrivacyControlsFromGate = false,
    completedAtEpochMs = null,
)
