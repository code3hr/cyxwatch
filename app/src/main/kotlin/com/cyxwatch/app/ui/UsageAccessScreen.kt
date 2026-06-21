package com.cyxwatch.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cyxwatch.app.R

@Composable
fun UsageAccessScreen(
    hasUsageAccess: Boolean,
    hasEverDenied: Boolean,
    deniedCount: Int,
    checkCount: Int,
    lastCheckedLabel: String,
    runtimeIntegrityNotice: String? = null,
    onOpenSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    val status = if (hasUsageAccess) "ACCESS GRANTED" else "ACCESS REQUIRED"
    val statusColor = if (hasUsageAccess) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Image(
                        modifier = Modifier.size(56.dp),
                        painter = painterResource(R.drawable.ic_cyxwatch_logo),
                        contentDescription = "CyxWatch logo",
                    )
                    Text("Usage Access", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "App activity visibility requires Usage Access permission.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("State", style = MaterialTheme.typography.labelSmall)
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = statusColor)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Checks", style = MaterialTheme.typography.labelSmall)
                        Text(checkCount.toString(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.40f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "What this permission is for:",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "It allows CyxWatch to read local app-activity windows so it can build evidence timelines.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "CyxWatch does not capture packet payloads, does not upload telemetry, and does not modify network traffic.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "All processing remains local to this device. No payloads or analytics are sent to the cloud.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Advanced VPN mode is visibility only and is not a private/anonymizing tunnel.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (hasEverDenied) {
                        Text(
                            "Recent check history: denied $deniedCount time(s), opened $checkCount time(s).",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (lastCheckedLabel.isNotBlank()) {
                        Text(
                            "Last checked: $lastCheckedLabel",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Button(
                onClick = onOpenSettingsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Open usage access settings" },
            ) {
                Text("Enable Usage Access")
            }
            OutlinedButton(
                onClick = onRefreshClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Recheck usage access status" },
            ) {
                Text(if (hasUsageAccess) "Recheck: Access Enabled" else "Recheck Permission")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        ScrollNavigationControls(
            scrollState = scrollState,
            topContentDescription = "Scroll to top of usage access screen",
            bottomContentDescription = "Scroll to bottom of usage access screen",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp),
        )
    }
}
