package com.cyxwatch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cyxwatch.app.domain.ScoreReason
import com.cyxwatch.app.domain.ScoringRule
import com.cyxwatch.app.domain.model.PrivacyEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreEvidenceScreen(
    reason: ScoreReason,
    evidenceEvents: List<PrivacyEvent>,
    onBack: () -> Unit,
    appLabelsByPackageName: Map<String, String> = emptyMap(),
) {
    val signalLevel = signalLevelForRule(reason.rule)

    Scaffold(topBar = { TopAppBar(title = { Text("Score Evidence") }) }) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back from score evidence" },
            ) {
                Text("Back")
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(reason.rule.description, style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SignalBadge(level = signalLevel, label = signalLevelLabel(signalLevel))
                        Text(
                            "Signal: ${signalLevelLabel(signalLevel)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    HorizontalDivider()
                    Text(
                        "App: ${appDisplayName(reason.packageName, appLabelsByPackageName)}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text("Score impact: -${reason.delta}", style = MaterialTheme.typography.bodyMedium)
                    if (
                        reason.rule == ScoringRule.SensitivePermissionAdded ||
                        reason.rule == ScoringRule.NewAppWithSensitivePermissions
                    ) {
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
                }
            }

            if (evidenceEvents.isEmpty()) {
                Text("No matching evidence events are currently loaded.")
                return@Scaffold
            }

            Text("Evidence timeline", style = MaterialTheme.typography.titleSmall)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(evidenceEvents) { event ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(event.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${formatEvidenceTimestamp(event.timestamp)} | ${event.source}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(event.explanation, style = MaterialTheme.typography.bodyMedium)
                            val eventSignalLevel = signalLevelForEventSeverity(event.severity)
                            Text(
                                "Event signal: ${signalLevelLabel(eventSignalLevel)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "Evidence ID: ${event.eventId}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = event.evidenceJson,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatEvidenceTimestamp(timestamp: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    return formatter.format(timestamp)
}

@Composable
private fun SignalBadge(
    level: SignalLevel,
    label: String,
) {
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
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}
