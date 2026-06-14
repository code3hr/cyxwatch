package com.cyxwatch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfileScreen(
    profile: AppProfile,
    onBack: () -> Unit,
    onSensitivePermissionClick: (String) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("App Profile") }) }) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back to previous app profile screen" },
            ) {
                Text("Back")
            }

            Text(text = profile.label, style = MaterialTheme.typography.headlineSmall)
            Text(text = profile.packageName, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Launchable: ${if (profile.isLaunchable) "Yes" else "No"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (profile.versionName != null) {
                Text(
                    text = "Version: ${profile.versionName}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = "First installed: ${formatProfileTimestamp(profile.firstInstallTimeEpochMs)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Last updated: ${formatProfileTimestamp(profile.lastUpdateTimeEpochMs)}",
                style = MaterialTheme.typography.bodySmall,
            )

            val sensitivePermissions = profile.permissions.filter(::isSensitivePermission)
            Text(
                text = "Permissions: ${profile.permissions.size} (sensitive: ${sensitivePermissions.size})",
                style = MaterialTheme.typography.titleMedium,
            )
            if (sensitivePermissions.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Sensitive permissions", style = MaterialTheme.typography.titleSmall)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sensitivePermissions.forEach { permission ->
                                AssistChip(
                                    onClick = { onSensitivePermissionClick(permission) },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Open evidence for ${readablePermission(permission)} permission"
                                    },
                                    label = {
                                        Text(
                                            text = readablePermission(permission),
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1,
                                        )
                                    },
                                )
                            }
                        }
                        Text(
                            text = "Tap a badge to surface the related timeline evidence path.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Text("All permissions", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(profile.permissions) { permission ->
                    Text(
                        text = readablePermission(permission),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

        }
    }
}

private fun isSensitivePermission(permission: String): Boolean {
    return permission in SENSITIVE_PERMISSIONS
}

private val SENSITIVE_PERMISSIONS = setOf(
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.RECORD_AUDIO",
    "android.permission.CAMERA",
    "android.permission.READ_CONTACTS",
    "android.permission.WRITE_CONTACTS",
    "android.permission.GET_ACCOUNTS",
    "android.permission.READ_CALENDAR",
    "android.permission.WRITE_CALENDAR",
    "android.permission.READ_SMS",
    "android.permission.READ_CALL_LOG",
    "android.permission.WRITE_CALL_LOG",
    "android.permission.READ_PHONE_STATE",
    "android.permission.CALL_PHONE",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.ACTIVITY_RECOGNITION",
    "android.permission.BODY_SENSORS",
    "android.permission.USE_FINGERPRINT",
    "android.permission.USE_BIOMETRIC",
)

private fun readablePermission(permission: String): String {
    return permission
        .removePrefix("android.permission.")
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}

private fun formatProfileTimestamp(timestamp: Long): String {
    return try {
        val instant = Instant.ofEpochMilli(timestamp)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        ""
    }
}
