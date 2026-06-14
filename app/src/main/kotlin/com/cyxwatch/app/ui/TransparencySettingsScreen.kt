package com.cyxwatch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Scaffold(topBar = { TopAppBar(title = { Text("Privacy Settings") }) }) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Privacy mode", style = MaterialTheme.typography.titleMedium)
                    Text("Local-only processing", style = MaterialTheme.typography.titleSmall)
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Network visibility", style = MaterialTheme.typography.titleMedium)
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Advanced visibility diagnostics", style = MaterialTheme.typography.titleMedium)
                    if (isVpnModeEnabled) {
                        Text("Observed packets: $vpnPacketsObserved", style = MaterialTheme.typography.bodySmall)
                        Text("Observed bytes: $vpnBytesObserved", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Unique destinations: $vpnUniqueDestinationCount",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text("Parsed packets: $vpnParsedPacketsObserved", style = MaterialTheme.typography.bodySmall)
                        Text("Unparsed packets: $vpnUnparsedPacketsObserved", style = MaterialTheme.typography.bodySmall)
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Usage Access", style = MaterialTheme.typography.titleMedium)
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Loaded evidence", style = MaterialTheme.typography.titleMedium)
                    Text("Usage events: $loadedUsageEventCount", style = MaterialTheme.typography.bodySmall)
                    Text("Inventory events: $loadedInventoryEventCount", style = MaterialTheme.typography.bodySmall)
                    Text("Network events: $loadedNetworkEventCount", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
