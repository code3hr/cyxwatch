package com.cyxwatch.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cyxwatch.app.R

@Composable
fun LaunchGateScreen(
    onStartMonitoringClick: () -> Unit,
    onOpenPrivacyControlsClick: () -> Unit,
    onBackToDashboardClick: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Image(
                        modifier = Modifier.size(84.dp),
                        painter = painterResource(R.drawable.ic_cyxwatch_logo),
                        contentDescription = "CyxWatch logo",
                    )
                    Text(
                        text = "CyxWatch",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Local-first privacy observability for app activity and network visibility.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.10f),
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Important", color = MaterialTheme.colorScheme.error)
                    Text(
                        "CyxWatch is observability only. It is not a private VPN or traffic anonymizer.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            MonitorInfoCard(
                title = "Why this app needs setup",
                rows = listOf(
                    "Read local app activity timelines for on-device scoring.",
                    "Offer optional VPN-mode network visibility metadata.",
                    "Keep evidence visible and auditable with retention controls.",
                ),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Setup behavior", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Usage Access is required to show local app activity. If denied, collection stays paused.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Network visibility starts in basic mode. Enable advanced mode only when you want local endpoint visibility.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "No telemetry, no cloud sync, no payload uploads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF78FFD1),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onStartMonitoringClick,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Start monitoring from launch gate" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("Start monitoring")
                }
                OutlinedButton(
                    onClick = onOpenPrivacyControlsClick,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Open privacy controls from launch gate" },
                ) {
                    Text("View privacy controls")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("First launch behavior", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "This screen is shown only on first setup. You can skip now and finish setup later.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Privacy controls remain available from Privacy Settings at any time.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            OutlinedButton(
                onClick = onBackToDashboardClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Skip launch gate for now" }
                    .heightIn(min = 48.dp),
            ) {
                Text("Skip for now")
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "You can close this now and continue in Privacy Settings when you are ready.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        ScrollNavigationControls(
            scrollState = scrollState,
            topContentDescription = "Scroll to top of launch gate",
            bottomContentDescription = "Scroll to bottom of launch gate",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun MonitorInfoCard(title: String, rows: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            rows.forEach { row ->
                Text("- $row", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
