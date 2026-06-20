package com.cyxwatch.app.data.settings

import android.content.Context
import com.cyxwatch.app.data.EncryptedPreferencesStore

private const val VPN_PREF_NAME = "cyxwatch_vpn_mode_prefs"
private const val KEY_VPN_ENABLED = "vpn_mode_enabled"
private const val KEY_VPN_FORWARDING_ENABLED = "vpn_mode_forwarding_enabled"
private const val KEY_LAST_ENABLED_AT = "vpn_mode_last_enabled_at"
private const val KEY_LAST_DISABLED_AT = "vpn_mode_last_disabled_at"
private const val PREF_LONG_UNSET = -1L

class VpnModeSettingsRepository(context: Context) {
    private val prefs = EncryptedPreferencesStore(
        context = context,
        preferenceFileName = VPN_PREF_NAME,
    )

    fun readState(): VpnModeState {
        return VpnModeState(
            isEnabled = prefs.readBoolean(KEY_VPN_ENABLED, false),
            isForwardingEnabled = prefs.readBoolean(KEY_VPN_FORWARDING_ENABLED, false),
            lastEnabledAtEpochMs = readLongOrNull(KEY_LAST_ENABLED_AT),
            lastDisabledAtEpochMs = readLongOrNull(KEY_LAST_DISABLED_AT),
        )
    }

    fun setEnabled(isEnabled: Boolean, changedAtEpochMs: Long): VpnModeState {
        val current = readState()
        val updated = current.withEnabled(
            enabled = isEnabled,
            enabledAtEpochMs = changedAtEpochMs,
            disabledAtEpochMs = if (!isEnabled) changedAtEpochMs else current.lastDisabledAtEpochMs,
            enabledTimestamp = if (isEnabled) changedAtEpochMs else current.lastEnabledAtEpochMs,
        )
        prefs.writeBoolean(KEY_VPN_ENABLED, updated.isEnabled)
        prefs.writeBoolean(KEY_VPN_FORWARDING_ENABLED, updated.isForwardingEnabled)
        prefs.writeLong(KEY_LAST_ENABLED_AT, updated.lastEnabledAtEpochMs ?: PREF_LONG_UNSET)
        prefs.writeLong(KEY_LAST_DISABLED_AT, updated.lastDisabledAtEpochMs ?: PREF_LONG_UNSET)
        return updated
    }

    fun setForwardingEnabled(isEnabled: Boolean): VpnModeState {
        val updated = readState().withForwardingEnabled(isEnabled)
        prefs.writeBoolean(KEY_VPN_FORWARDING_ENABLED, updated.isForwardingEnabled)
        return updated
    }

    private fun readLongOrNull(key: String): Long? {
        val raw = prefs.readLong(key, PREF_LONG_UNSET)
        return if (raw == PREF_LONG_UNSET) null else raw
    }
}
