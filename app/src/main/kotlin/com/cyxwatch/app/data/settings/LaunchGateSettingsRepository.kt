package com.cyxwatch.app.data.settings

import android.content.Context

private const val PREF_NAME = "cyxwatch_launch_gate_prefs"
private const val KEY_HAS_COMPLETED_LAUNCH_GATE = "launch_gate_completed"
private const val KEY_HAS_OPENED_PRIVACY_CONTROLS_FROM_GATE = "launch_gate_privacy_controls_opened"
private const val KEY_COMPLETED_AT = "launch_gate_completed_at"
private const val PREF_LONG_UNSET = -1L

class LaunchGateSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun readState(): LaunchGateSettingsState {
        return DefaultLaunchGateSettingsState.copy(
            hasCompletedLaunchGate = prefs.getBoolean(KEY_HAS_COMPLETED_LAUNCH_GATE, false),
            hasOpenedPrivacyControlsFromGate = prefs.getBoolean(KEY_HAS_OPENED_PRIVACY_CONTROLS_FROM_GATE, false),
            completedAtEpochMs = readLongOrNull(KEY_COMPLETED_AT),
        )
    }

    fun markCompleted(openedPrivacyControlsFromGate: Boolean, completedAtEpochMs: Long) {
        val current = readState().withCompleted(
            openedPrivacyControlsFromGate = openedPrivacyControlsFromGate,
            completedAtEpochMs = completedAtEpochMs,
        )
        prefs.edit()
            .putBoolean(KEY_HAS_COMPLETED_LAUNCH_GATE, current.hasCompletedLaunchGate)
            .putBoolean(
                KEY_HAS_OPENED_PRIVACY_CONTROLS_FROM_GATE,
                current.hasOpenedPrivacyControlsFromGate,
            )
            .putLong(KEY_COMPLETED_AT, current.completedAtEpochMs ?: PREF_LONG_UNSET)
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
