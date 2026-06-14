package com.cyxwatch.app.data.settings

data class VpnModeState(
    val isEnabled: Boolean,
    val lastEnabledAtEpochMs: Long?,
    val lastDisabledAtEpochMs: Long?,
    val isForwardingEnabled: Boolean,
) {
    fun withEnabled(
        enabled: Boolean,
        enabledAtEpochMs: Long,
        disabledAtEpochMs: Long? = this.lastDisabledAtEpochMs,
        enabledTimestamp: Long? = this.lastEnabledAtEpochMs,
    ): VpnModeState {
        return VpnModeState(
            isEnabled = enabled,
            lastEnabledAtEpochMs = if (enabled) {
                enabledAtEpochMs
            } else {
                enabledTimestamp
            },
            lastDisabledAtEpochMs = if (!enabled) {
                disabledAtEpochMs
            } else {
                this.lastDisabledAtEpochMs
            },
            isForwardingEnabled = this.isForwardingEnabled,
        )
    }

    fun withForwardingEnabled(enabled: Boolean): VpnModeState {
        return copy(isForwardingEnabled = enabled)
    }
}
