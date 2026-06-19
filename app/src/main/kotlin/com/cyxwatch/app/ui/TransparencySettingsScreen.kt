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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransparencySettingsScreen(
    hasUsageAccess: Boolean,
    consentState: UsageAccessConsentState,
    lastCheckedLabel: String,
    lastSettingsOpenedLabel: String,
    isVpnModeEnabled: Boolean,
    onEnableVpnModeClick: () -> Unit,
    onDisableVpnModeClick: () -> Unit,
    vpnEnabledAtLabel: String,
    vpnDisabledAtLabel: String,
    retentionSettings: RetentionSettings,
    retentionStatus: String,
    allowedRetentionDays: List<Int>,
    loadedUsageEventCount: Int,
    loadedInventoryEventCount: Int,
    loadedNetworkEventCount: Int,
    vpnPacketsObserved: Long,
    vpnBytesObserved: Long,
    vpnUniqueDestinationCount: Int,
    vpnParsedPacketsObserved: Long,
    vpnUnparsedPacketsObserved: Long,
    vpnCaptureMode: String,
    vpnForwardingEnabled: Boolean,
    vpnForwardingRequested: Boolean,
    vpnForwardingSupported: Boolean,
    onToggleVpnForwardingModeClick: (Boolean) -> Unit,
    onRetentionDaysClick: (Int) -> Unit,
    onPruneNowClick: () -> Unit,
    onDeleteLoadedEventsClick: () -> Unit,
    onRefreshVpnDiagnosticsClick: () -> Unit,
    onBack: () -> Unit,
) {
    val visibilityLabel = if (isVpnModeEnabled) "ADVANCED" else "BASIC"
    val visibilityStateCopy = if (isVpnModeEnabled) "ADVANCED VISIBILITY" else "BASIC VISIBILITY"
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
                            value = visibilityLabel,
                            valueColor = if (isVpnModeEnabled) {
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
                            value = if (hasUsageAccess) "GRANTED" else "DENIED",
                            valueColor = if (hasUsageAccess) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.weight(1f),
                        )
                        SettingStatusBadge(
                            label = "Retention",
                            value = "${retentionSettings.retentionDays} day(s)",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text("Privacy mode", style = MaterialTheme.typography.titleMedium)
                    Text(visibilityStateCopy, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "No cloud sync, telemetry upload, packet payload capture, or background VPN mode is enabled.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Advanced network visibility is for local traffic transparency only and is not a private/anonymous VPN.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            NetworkVisibilitySettingsCard(
                isVpnModeEnabled = isVpnModeEnabled,
                vpnEnabledAtLabel = vpnEnabledAtLabel,
                vpnDisabledAtLabel = vpnDisabledAtLabel,
                onEnableVpnModeClick = onEnableVpnModeClick,
                onDisableVpnModeClick = onDisableVpnModeClick,
            )

            AdvancedVisibilityDiagnosticsCard(
                isVpnModeEnabled = isVpnModeEnabled,
                vpnPacketsObserved = vpnPacketsObserved,
                vpnBytesObserved = vpnBytesObserved,
                vpnUniqueDestinationCount = vpnUniqueDestinationCount,
                vpnParsedPacketsObserved = vpnParsedPacketsObserved,
                vpnUnparsedPacketsObserved = vpnUnparsedPacketsObserved,
                vpnCaptureMode = vpnCaptureMode,
                vpnForwardingEnabled = vpnForwardingEnabled,
                vpnForwardingRequested = vpnForwardingRequested,
                vpnForwardingSupported = vpnForwardingSupported,
                onToggleVpnForwardingModeClick = onToggleVpnForwardingModeClick,
                onRefreshVpnDiagnosticsClick = onRefreshVpnDiagnosticsClick,
            )

            RetentionSettingsCard(
                retentionSettings = retentionSettings,
                retentionStatus = retentionStatus,
                allowedRetentionDays = allowedRetentionDays,
                onRetentionDaysClick = onRetentionDaysClick,
                onPruneNowClick = onPruneNowClick,
                onDeleteLoadedEventsClick = onDeleteLoadedEventsClick,
            )

            UsageAccessStatusCard(
                hasUsageAccess = hasUsageAccess,
                consentState = consentState,
                lastCheckedLabel = lastCheckedLabel,
                lastSettingsOpenedLabel = lastSettingsOpenedLabel,
            )

            LoadedEvidenceCard(
                loadedUsageEventCount = loadedUsageEventCount,
                loadedInventoryEventCount = loadedInventoryEventCount,
                loadedNetworkEventCount = loadedNetworkEventCount,
            )
        }
    }
}

@Composable
private fun NetworkVisibilitySettingsCard(
    isVpnModeEnabled: Boolean,
    vpnEnabledAtLabel: String,
    vpnDisabledAtLabel: String,
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
                    value = if (isVpnModeEnabled) "ADVANCED" else "BASIC",
                    valueColor = if (isVpnModeEnabled) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.weight(1f),
                )
                SettingStatusBadge(
                    label = "Traffic mode",
                    value = if (isVpnModeEnabled) "METADATA" else "APP SUMMARY",
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                if (isVpnModeEnabled) {
                    "Advanced VPN visibility mode is active."
                } else {
                    "Network visibility is in basic mode."
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "This mode is for endpoint metadata visibility and does not secure, hide, or reroute your traffic.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (vpnEnabledAtLabel.isNotBlank()) {
                Text("Enabled at: $vpnEnabledAtLabel", style = MaterialTheme.typography.bodySmall)
            }
            if (vpnDisabledAtLabel.isNotBlank()) {
                Text("Disabled at: $vpnDisabledAtLabel", style = MaterialTheme.typography.bodySmall)
            }
            if (isVpnModeEnabled) {
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
    isVpnModeEnabled: Boolean,
    vpnPacketsObserved: Long,
    vpnBytesObserved: Long,
    vpnUniqueDestinationCount: Int,
    vpnParsedPacketsObserved: Long,
    vpnUnparsedPacketsObserved: Long,
    vpnCaptureMode: String,
    vpnForwardingEnabled: Boolean,
    vpnForwardingRequested: Boolean,
    vpnForwardingSupported: Boolean,
    onToggleVpnForwardingModeClick: (Boolean) -> Unit,
    onRefreshVpnDiagnosticsClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Advanced visibility diagnostics", style = MaterialTheme.typography.titleMedium)
            if (isVpnModeEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingStatusBadge(
                        label = "Packets",
                        value = vpnPacketsObserved.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    SettingStatusBadge(
                        label = "Bytes",
                        value = vpnBytesObserved.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    SettingStatusBadge(
                        label = "Destinations",
                        value = vpnUniqueDestinationCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingStatusBadge(
                        label = "Parsed",
                        value = vpnParsedPacketsObserved.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    SettingStatusBadge(
                        label = "Unparsed",
                        value = vpnUnparsedPacketsObserved.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("Collection mode: $vpnCaptureMode", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Forwarding requested: ${if (vpnForwardingRequested) "enabled" else "disabled"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Packet forwarding: ${if (vpnForwardingEnabled) "enabled" else "disabled"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (vpnForwardingRequested && !vpnForwardingEnabled) {
                    Text(
                        "Forwarding is not active in this build.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (vpnForwardingSupported) {
                    Button(
                        onClick = { onToggleVpnForwardingModeClick(!vpnForwardingRequested) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = if (vpnForwardingRequested) {
                                    "Disable VPN forwarding mode"
                                } else {
                                    "Enable VPN forwarding mode"
                                }
                            },
                    ) {
                        Text(
                            if (vpnForwardingRequested) {
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
    retentionSettings: RetentionSettings,
    retentionStatus: String,
    allowedRetentionDays: List<Int>,
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
            Text(retentionStatus, style = MaterialTheme.typography.bodySmall)
            allowedRetentionDays.forEach { days ->
                Button(
                    onClick = { onRetentionDaysClick(days) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Set retention window to $days days" },
                ) {
                    val selected = if (retentionSettings.retentionDays == days) "Current" else "Set"
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
    hasUsageAccess: Boolean,
    consentState: UsageAccessConsentState,
    lastCheckedLabel: String,
    lastSettingsOpenedLabel: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Usage access", style = MaterialTheme.typography.titleMedium)
            Text(if (hasUsageAccess) "Granted" else "Not granted", style = MaterialTheme.typography.titleSmall)
            Text(
                "Checks: ${consentState.checkCount}; denied: ${consentState.deniedCount}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (lastCheckedLabel.isNotBlank()) {
                Text("Last checked: $lastCheckedLabel", style = MaterialTheme.typography.bodySmall)
            }
            if (lastSettingsOpenedLabel.isNotBlank()) {
                Text("Settings opened: $lastSettingsOpenedLabel", style = MaterialTheme.typography.bodySmall)
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
