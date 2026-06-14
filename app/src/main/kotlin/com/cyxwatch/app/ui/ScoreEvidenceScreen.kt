package com.cyxwatch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

            Text(reason.rule.description, style = MaterialTheme.typography.titleMedium)
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

            if (evidenceEvents.isEmpty()) {
                Text("No matching evidence events are currently loaded.")
                return@Scaffold
            }

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
                            Text(
                                text = event.evidenceJson,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("severity: ${event.severity}", style = MaterialTheme.typography.bodySmall)
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
