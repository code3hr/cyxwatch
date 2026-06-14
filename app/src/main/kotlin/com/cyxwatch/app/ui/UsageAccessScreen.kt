package com.cyxwatch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun UsageAccessScreen(
    hasUsageAccess: Boolean,
    hasEverDenied: Boolean,
    deniedCount: Int,
    checkCount: Int,
    lastCheckedLabel: String,
    onOpenSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Usage access is required for activity timeline and privacy signals.",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "CyxWatch works locally on-device and needs Usage Access to read app activity.",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (hasEverDenied && checkCount > 0) {
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Recent check history: denied $deniedCount time(s), opened $checkCount time(s).",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (lastCheckedLabel.isNotBlank()) {
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = "Last checked: $lastCheckedLabel",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.size(16.dp))
        Button(
            onClick = onOpenSettingsClick,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Open usage access settings" },
        ) {
            Text("Enable Usage Access")
        }
        Spacer(modifier = Modifier.size(10.dp))
        Button(
            onClick = onRefreshClick,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Recheck usage access status" },
        ) {
            Text(if (hasUsageAccess) "Recheck: Access Enabled" else "Recheck Permission")
        }
    }
}
