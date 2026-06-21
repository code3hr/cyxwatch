package com.cyxwatch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
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
    val listState = rememberLazyListState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Permission Evidence") }) },
        floatingActionButton = {
            LazyListScrollNavigationControls(
                listState = listState,
                topContentDescription = "Scroll to top of permission evidence",
                bottomContentDescription = "Scroll to bottom of permission evidence",
            )
        },
    ) { contentPadding ->
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

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Profile: ${profile.label}", style = MaterialTheme.typography.titleMedium)
                    Text("Package: ${profile.packageName}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Permission: ${readablePermissionName(permission)}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    HorizontalDivider()
                    Text(
                        "This screen shows only local permission evidence; no sensitive permission data is uploaded.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (evidenceEvents.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                    ),
                ) {
                    Text(
                        "No recorded timeline evidence for this permission yet.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            } else {
                Text("Evidence timeline", style = MaterialTheme.typography.titleSmall)
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
                                val eventSignalLevel = signalLevelForEventSeverity(event.severity)
                                Text(
                                    "Signal: ${signalLevelLabel(eventSignalLevel)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text("Evidence ID: ${event.eventId}", style = MaterialTheme.typography.bodySmall)
                                Text(event.explanation, style = MaterialTheme.typography.bodyMedium)
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
}

private fun formatEvidenceTimestamp(timestamp: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    return formatter.format(timestamp)
}
