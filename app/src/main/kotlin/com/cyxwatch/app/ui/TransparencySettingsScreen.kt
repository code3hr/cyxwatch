package com.cyxwatch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cyxwatch.app.data.settings.UsageAccessConsentState
import com.cyxwatch.app.domain.RetentionSettings

data class TransparencySettingsUiState(
    val hasUsageAccess: Boolean,
    val consentState: UsageAccessConsentState,
    val lastCheckedLabel: String,
    val lastSettingsOpenedLabel: String,
    val isVpnModeEnabled: Boolean,
    val vpnEnabledAtLabel: String,
    val vpnDisabledAtLabel: String,
    val retentionSettings: RetentionSettings,
    val retentionStatus: String,
    val allowedRetentionDays: List<Int>,
    val loadedUsageEventCount: Int,
    val loadedInventoryEventCount: Int,
    val loadedNetworkEventCount: Int,
    val vpnPacketsObserved: Long,
    val vpnBytesObserved: Long,
    val vpnUniqueDestinationCount: Int,
    val vpnParsedPacketsObserved: Long,
    val vpnUnparsedPacketsObserved: Long,
    val vpnCaptureMode: String,
    val vpnForwardingEnabled: Boolean,
    val vpnForwardingRequested: Boolean,
    val vpnForwardingSupported: Boolean,
    val isSecureScreenModeEnabled: Boolean,
) {
    val visibilityModeLabel: String
        get() = if (isVpnModeEnabled) "ADVANCED" else "BASIC"

    val visibilityModeDescription: String
        get() = if (isVpnModeEnabled) "ADVANCED VISIBILITY" else "BASIC VISIBILITY"

    val trafficModeLabel: String
        get() = if (isVpnModeEnabled) "METADATA" else "APP SUMMARY"

    val usageAccessLabel: String
        get() = if (hasUsageAccess) "GRANTED" else "DENIED"

    val vpnModeCopy: String
        get() = if (isVpnModeEnabled) {
            "Advanced VPN visibility mode is active."
        } else {
            "Network visibility is in basic mode."
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransparencySettingsScreen(
    state: TransparencySettingsUiState,
    onEnableVpnModeClick: () -> Unit,
    onDisableVpnModeClick: () -> Unit,
    runtimeIntegrityNotice: String? = null,
    onToggleSecureScreenModeClick: () -> Unit,
    onToggleVpnForwardingModeClick: (Boolean) -> Unit,
    onRetentionDaysClick: (Int) -> Unit,
    onPruneNowClick: () -> Unit,
    onDeleteLoadedEventsClick: () -> Unit,
    onRefreshVpnDiagnosticsClick: () -> Unit,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Privacy Settings") }) },
        floatingActionButton = {
            ScrollNavigationControls(
                scrollState = scrollState,
                topContentDescription = "Scroll to top of privacy settings",
                bottomContentDescription = "Scroll to bottom of privacy settings",
            )
        },
        ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            runtimeIntegrityNotice?.takeIf { it.isNotBlank() }?.let { notice ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    ),
                ) {
                    Text(
                        notice,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back from privacy settings" },
            ) {
                Text("Back")
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingStatusBadge(
                            label = "Mode",
                            value = state.visibilityModeLabel,
                            valueColor = if (state.isVpnModeEnabled) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.weight(1f),
                        )
                        SettingStatusBadge(
                            label = "Privacy mode",
                            value = "LOCAL",
                            valueColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingStatusBadge(
                            label = "Usage access",
                            value = state.usageAccessLabel,
                            valueColor = if (state.hasUsageAccess) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.weight(1f),
                        )
                        SettingStatusBadge(
                            label = "Retention",
                            value = "${state.retentionSettings.retentionDays} day(s)",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text("Privacy mode", style = MaterialTheme.typography.titleMedium)
                    Text(state.visibilityModeDescription, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "No cloud sync, telemetry upload, packet payload capture, or background VPN mode is enabled.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Advanced network visibility is for local traffic transparency only and is not a private/anonymous VPN.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "All processing and storage remain on-device. No cloud sync or packet payload collection.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Secure screen mode ${if (state.isSecureScreenModeEnabled) "is enabled" else "is disabled"}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        if (state.isSecureScreenModeEnabled) {
                            "Screens with sensitive evidence and permission details block screenshots and screen recording."
                        } else {
                            "Enable secure mode to block screenshots and screen recording on sensitive screens."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = onToggleSecureScreenModeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = if (state.isSecureScreenModeEnabled) {
                                    "Disable secure screen mode"
                                } else {
                                    "Enable secure screen mode"
                                }
                            },
                    ) {
                        Text(
                            if (state.isSecureScreenModeEnabled) {
                                "Disable secure screen mode"
                            } else {
                                "Enable secure screen mode"
                            },
                        )
                    }
                }
            }

            NetworkVisibilitySettingsCard(
                state = state,
                onEnableVpnModeClick = onEnableVpnModeClick,
                onDisableVpnModeClick = onDisableVpnModeClick,
            )

            AdvancedVisibilityDiagnosticsCard(
                state = state,
                onToggleVpnForwardingModeClick = onToggleVpnForwardingModeClick,
                onRefreshVpnDiagnosticsClick = onRefreshVpnDiagnosticsClick,
            )

            RetentionSettingsCard(
                state = state,
                onRetentionDaysClick = onRetentionDaysClick,
                onPruneNowClick = onPruneNowClick,
                onDeleteLoadedEventsClick = onDeleteLoadedEventsClick,
            )

            UsageAccessStatusCard(
                state = state,
            )

            LoadedEvidenceCard(
                loadedUsageEventCount = state.loadedUsageEventCount,
                loadedInventoryEventCount = state.loadedInventoryEventCount,
                loadedNetworkEventCount = state.loadedNetworkEventCount,
            )
        }
    }
}

@Composable
private fun NetworkVisibilitySettingsCard(
    state: TransparencySettingsUiState,
    onEnableVpnModeClick: () -> Unit,
    onDisableVpnModeClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Network visibility", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingStatusBadge(
                    label = "Mode",
                    value = state.visibilityModeLabel,
                    valueColor = if (state.isVpnModeEnabled) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.weight(1f),
                )
                SettingStatusBadge(
                    label = "Traffic mode",
                    value = state.trafficModeLabel,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(state.vpnModeCopy, style = MaterialTheme.typography.bodySmall)
            Text(
                "This mode is for endpoint metadata visibility and does not secure, hide, or reroute your traffic.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "This is local monitoring only. It does not provide anonymity or traffic tunneling.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.vpnEnabledAtLabel.isNotBlank()) {
                Text("Enabled at: ${state.vpnEnabledAtLabel}", style = MaterialTheme.typography.bodySmall)
            }
            if (state.vpnDisabledAtLabel.isNotBlank()) {
                Text("Disabled at: ${state.vpnDisabledAtLabel}", style = MaterialTheme.typography.bodySmall)
            }
            if (state.isVpnModeEnabled) {
                Button(
                    onClick = onDisableVpnModeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Stop advanced network visibility" },
                ) {
                    Text("Stop advanced network visibility")
                }
            } else {
                Button(
                    onClick = onEnableVpnModeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Enable advanced network visibility" },
                ) {
                    Text("Enable advanced network visibility")
                }
            }
        }
    }
}

@Composable
private fun AdvancedVisibilityDiagnosticsCard(
    state: TransparencySettingsUiState,
    onToggleVpnForwardingModeClick: (Boolean) -> Unit,
    onRefreshVpnDiagnosticsClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Advanced visibility diagnostics", style = MaterialTheme.typography.titleMedium)
            if (state.isVpnModeEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingStatusBadge(
                        label = "Packets",
                        value = state.vpnPacketsObserved.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    SettingStatusBadge(
                        label = "Bytes",
                        value = state.vpnBytesObserved.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    SettingStatusBadge(
                        label = "Destinations",
                        value = state.vpnUniqueDestinationCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingStatusBadge(
                        label = "Parsed",
                        value = state.vpnParsedPacketsObserved.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    SettingStatusBadge(
                        label = "Unparsed",
                        value = state.vpnUnparsedPacketsObserved.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("Collection mode: ${state.vpnCaptureMode}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Forwarding requested: ${if (state.vpnForwardingRequested) "enabled" else "disabled"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Packet forwarding: ${if (state.vpnForwardingEnabled) "enabled" else "disabled"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (state.vpnForwardingRequested && !state.vpnForwardingEnabled) {
                    Text(
                        "Forwarding is not active in this build.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (state.vpnForwardingSupported) {
                    Button(
                        onClick = { onToggleVpnForwardingModeClick(!state.vpnForwardingRequested) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = if (state.vpnForwardingRequested) {
                                    "Disable VPN forwarding mode"
                                } else {
                                    "Enable VPN forwarding mode"
                                }
                            },
                    ) {
                        Text(
                            if (state.vpnForwardingRequested) {
                                "Disable VPN forwarding mode"
                            } else {
                                "Enable VPN forwarding mode"
                            },
                        )
                    }
                } else {
                    Text(
                        "Forwarding mode is currently not available in this build.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = onRefreshVpnDiagnosticsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Refresh VPN visibility diagnostics" },
                ) {
                    Text("Refresh VPN visibility diagnostics")
                }
            } else {
                Text("Advanced visibility is not active.", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "Note: packet payloads are never captured in VPN visibility. " +
                    "It is a local monitoring mode, not a private network tunnel.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RetentionSettingsCard(
    state: TransparencySettingsUiState,
    onRetentionDaysClick: (Int) -> Unit,
    onPruneNowClick: () -> Unit,
    onDeleteLoadedEventsClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Retention", style = MaterialTheme.typography.titleMedium)
            Text(state.retentionStatus, style = MaterialTheme.typography.bodySmall)
            state.allowedRetentionDays.forEach { days ->
                Button(
                    onClick = { onRetentionDaysClick(days) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Set retention window to $days days" },
                ) {
                    val selected = if (state.retentionSettings.retentionDays == days) "Current" else "Set"
                    Text("$selected: $days days")
                }
            }
            Button(
                onClick = onPruneNowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Prune loaded evidence now" },
            ) {
                Text("Prune loaded evidence")
            }
            Button(
                onClick = onDeleteLoadedEventsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Delete all loaded evidence" },
            ) {
                Text("Delete loaded evidence")
            }
        }
    }
}

@Composable
private fun UsageAccessStatusCard(
    state: TransparencySettingsUiState,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Usage access", style = MaterialTheme.typography.titleMedium)
            Text(if (state.hasUsageAccess) "Granted" else "Not granted", style = MaterialTheme.typography.titleSmall)
            Text(
                "Checks: ${state.consentState.checkCount}; denied: ${state.consentState.deniedCount}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.lastCheckedLabel.isNotBlank()) {
                Text("Last checked: ${state.lastCheckedLabel}", style = MaterialTheme.typography.bodySmall)
            }
            if (state.lastSettingsOpenedLabel.isNotBlank()) {
                Text("Settings opened: ${state.lastSettingsOpenedLabel}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LoadedEvidenceCard(
    loadedUsageEventCount: Int,
    loadedInventoryEventCount: Int,
    loadedNetworkEventCount: Int,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Loaded evidence", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingStatusBadge(
                    label = "Usage",
                    value = loadedUsageEventCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                SettingStatusBadge(
                    label = "Inventory",
                    value = loadedInventoryEventCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                SettingStatusBadge(
                    label = "Network",
                    value = loadedNetworkEventCount.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SettingStatusBadge(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.labelMedium, color = valueColor)
        }
    }
}
