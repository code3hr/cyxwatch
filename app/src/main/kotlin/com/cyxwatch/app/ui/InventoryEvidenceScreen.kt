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
import com.cyxwatch.app.domain.model.AppProfile
import com.cyxwatch.app.domain.model.PrivacyEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryEvidenceScreen(
    profile: AppProfile,
    permission: String,
    evidenceEvents: List<PrivacyEvent>,
    onBack: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Permission Evidence") }) }) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back from permission evidence" },
            ) {
                Text("Back")
            }

            Text("Profile: ${profile.label}", style = MaterialTheme.typography.titleMedium)
            Text("Permission: ${readablePermission(permission)}", style = MaterialTheme.typography.titleSmall)

            if (evidenceEvents.isEmpty()) {
                Text("No recorded timeline evidence for this permission yet.")
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
                                text = "time: ${formatEvidenceTimestamp(event.timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "source: ${event.source}",
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

private fun readablePermission(permission: String): String {
    return permission
        .removePrefix("android.permission.")
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}
