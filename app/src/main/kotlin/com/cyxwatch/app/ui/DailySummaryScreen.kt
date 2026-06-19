package com.cyxwatch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cyxwatch.app.domain.DailySummary
import com.cyxwatch.app.domain.PrivacyAlert
import com.cyxwatch.app.domain.ScoreReason
import com.cyxwatch.app.domain.model.PrivacyEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySummaryScreen(
    summary: DailySummary,
    scoringEvents: List<PrivacyEvent>,
    onBack: () -> Unit,
    onOpenReason: (ScoreReason) -> Unit,
    onOpenAlert: (PrivacyAlert) -> Unit,
    onOpenAlertProfile: (String) -> Unit,
    onOpenTopApp: (String) -> Unit,
    appLabelsByPackageName: Map<String, String> = emptyMap(),
) {
    val listState = rememberLazyListState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Daily Summary") }) },
        floatingActionButton = {
            LazyListScrollNavigationControls(
                listState = listState,
                topContentDescription = "Scroll to top of report",
                bottomContentDescription = "Scroll to bottom of report",
            )
        },
    ) { contentPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.semantics { contentDescription = "Back to dashboard from daily summary" },
                ) {
                    Text("Back")
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Report window", style = MaterialTheme.typography.titleMedium)
                        Text("Date: ${summary.dateLabel}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Generated: ${formatSummaryTimestamp(summary.generatedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Window: ${formatSummaryTimestamp(summary.windowStart)} -> ${formatSummaryTimestamp(summary.windowEnd)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MonitorMetric(
                                "Score",
                                "${summary.score}/100",
                                MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            MonitorMetric(
                                "Signals",
                                summary.topReasons.size.toString(),
                                MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f),
                            )
                            MonitorMetric(
                                "Evidence",
                                (summary.usageEventCount + summary.networkEventCount + summary.inventoryEventCount).toString(),
                                MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Usage events: ${summary.usageEventCount}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "Network events: ${summary.networkEventCount}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "Inventory events: ${summary.inventoryEventCount}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Monitoring scope", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Monitoring is local-first and does not upload evidence.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "VPN visibility (if enabled) reads packet metadata only (endpoint and size), never payload.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "This mode is not a private VPN tunnel and does not hide traffic.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Top risk reasons", style = MaterialTheme.typography.titleMedium)
                        if (summary.topReasons.isEmpty()) {
                            Text("No risk reasons for the current evidence window.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            summary.topReasons
                                .take(5)
                                .forEach { reason ->
                                    DailySummaryRiskReasonPanel(
                                        reason = reason,
                                        appLabelsByPackageName = appLabelsByPackageName,
                                        onOpenReason = onOpenReason,
                                    )
                                }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Alerts", style = MaterialTheme.typography.titleMedium)
                        Text("New alerts generated today: ${summary.topAlertCount}", style = MaterialTheme.typography.bodySmall)
                        if (summary.recentAlerts.isEmpty()) {
                            Text("No alerts in this window.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            summary.recentAlerts.forEach { alert ->
                                DailySummaryAlertPanel(
                                    alert = alert,
                                    appLabelsByPackageName = appLabelsByPackageName,
                                    onOpenAlert = onOpenAlert,
                                    onOpenAlertProfile = onOpenAlertProfile,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Top active apps", style = MaterialTheme.typography.titleMedium)
                        if (summary.topApps.isEmpty()) {
                            Text("No app activity is currently loaded.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            val loadedPackageSet = scoringEvents
                                .map { it.packageName }
                                .toSet()
                            summary.topApps.forEach { packageName ->
                                val isLoaded = loadedPackageSet.contains(packageName)
                                OutlinedButton(
                                    onClick = { onOpenTopApp(packageName) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics {
                                            contentDescription = "Open app profile for $packageName"
                                        },
                                    enabled = isLoaded,
                                ) {
                                    Text(
                                        text = appDisplayName(packageName, appLabelsByPackageName),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySummaryRiskReasonPanel(
    reason: ScoreReason,
    appLabelsByPackageName: Map<String, String>,
    onOpenReason: (ScoreReason) -> Unit,
) {
    val isSensitivePermissionReason = reason.rule.isSensitivePermissionWarning()
    val isCriticalSignal = reason.rule.isCriticalWarning()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCriticalSignal) {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.shapes.small,
                    )
                } else {
                    Modifier
                },
            ),
        color = if (isCriticalSignal) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        },
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val signalLevel = signalLevelForRule(reason.rule)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SummarySignalBadge(
                    level = signalLevel,
                    label = signalLevelLabel(signalLevel),
                )
                if (isCriticalSignal) {
                    Text(
                        "Critical",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    appDisplayName(reason.packageName, appLabelsByPackageName),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "-${reason.delta}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                reason.rule.description,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                reason.message,
                style = MaterialTheme.typography.bodySmall,
            )
            if (isSensitivePermissionReason) {
                val permissions = extractPermissionValues(reason.message)
                    .map(::readablePermissionName)
                    .joinToString(", ")
                if (permissions.isNotBlank()) {
                    Text(
                        "Sensitive permissions: $permissions",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                "Evidence events: ${reason.evidenceEventIds.size}",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = { onOpenReason(reason) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Open score reason evidence for ${reason.packageName}"
                    },
            ) {
                Text(
                    if (isSensitivePermissionReason) {
                        "Review permission evidence"
                    } else {
                        "Open evidence"
                    },
                )
            }
        }
    }
}

@Composable
private fun DailySummaryAlertPanel(
    alert: PrivacyAlert,
    appLabelsByPackageName: Map<String, String>,
    onOpenAlert: (PrivacyAlert) -> Unit,
    onOpenAlertProfile: (String) -> Unit,
) {
    val isSensitivePermissionWarning = alert.rule.isSensitivePermissionWarning()
    val isCriticalAlert = alert.rule.isCriticalWarning()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCriticalAlert) {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.shapes.small,
                    )
                } else {
                    Modifier
                },
            ),
        color = if (isCriticalAlert) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        } else if (isSensitivePermissionWarning) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
        },
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val signalLevel = signalLevelForRule(alert.rule)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SummarySignalBadge(
                    level = signalLevel,
                    label = signalLevelLabel(signalLevel),
                )
                if (isCriticalAlert) {
                    Text(
                        "Critical",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    if (isSensitivePermissionWarning) {
                        "Permission warning"
                    } else {
                        "Behavior alert"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSensitivePermissionWarning) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            Text(
                appDisplayName(alert.packageName, appLabelsByPackageName),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Triggered: ${formatSummaryTimestamp(alert.triggeredAt)} | score impact -${alert.triggerDelta}",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = { onOpenAlert(alert) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Open alert evidence for ${alert.packageName}"
                    },
            ) {
                Text("Open supporting evidence")
            }
            if (isSensitivePermissionWarning) {
                OutlinedButton(
                    onClick = { onOpenAlertProfile(alert.packageName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Open app profile from alert ${alert.packageName}"
                        },
                    enabled = appLabelsByPackageName.containsKey(alert.packageName),
                ) {
                    Text("Open app profile")
                }
            }
        }
    }
}

private fun formatSummaryTimestamp(timestamp: Instant): String {
    return DateTimeFormatter
        .ofPattern("MMM d, HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(timestamp)
}

@Composable
private fun MonitorMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .then(modifier)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SummarySignalBadge(level: SignalLevel, label: String) {
    val background = when (level) {
        SignalLevel.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        SignalLevel.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        SignalLevel.LOW -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    }
    val textColor = when (level) {
        SignalLevel.HIGH -> MaterialTheme.colorScheme.onErrorContainer
        SignalLevel.MEDIUM -> MaterialTheme.colorScheme.onTertiaryContainer
        SignalLevel.LOW -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}
