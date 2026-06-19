package com.cyxwatch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cyxwatch.app.domain.SensitivePermissionPolicy
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
    val sensitivePermissions = profile.permissions.filter(SensitivePermissionPolicy::isSensitive)
    val orderedPermissions = profile.permissions.sortedBy { permission ->
        if (SensitivePermissionPolicy.isSensitive(permission)) {
            "0-$permission"
        } else {
            "1-$permission"
        }
    }
    val totalPermissionCount = profile.permissions.size
    val listState = rememberLazyListState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("App Profile") }) },
        floatingActionButton = {
            LazyListScrollNavigationControls(
                listState = listState,
                topContentDescription = "Scroll to top of app profile",
                bottomContentDescription = "Scroll to bottom of app profile",
            )
        },
    ) { contentPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.semantics { contentDescription = "Back to previous app profile screen" },
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
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(profile.label, style = MaterialTheme.typography.titleLarge)
                        Text(profile.packageName, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileStatusBadge(
                                label = "Launchable",
                                value = if (profile.isLaunchable) "YES" else "NO",
                                valueColor = if (profile.isLaunchable) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.weight(1f),
                            )
                            ProfileStatusBadge(
                                label = "Version",
                                value = profile.versionName ?: "Unknown",
                                modifier = Modifier.weight(1f),
                            )
                        }
                        ProfileStatusBadge(
                            label = "Version code",
                            value = profile.versionCode.toString(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        HorizontalDivider()
                        Text("First installed: ${formatProfileTimestamp(profile.firstInstallTimeEpochMs)}")
                        Text("Last updated: ${formatProfileTimestamp(profile.lastUpdateTimeEpochMs)}")
                    }
                }
            }

            item {
                val postureLabel = if (sensitivePermissions.isEmpty()) {
                    "LOW RISK"
                } else if (sensitivePermissions.size <= 2) {
                    "SENSITIVE WATCH"
                } else {
                    "SENSITIVE RISK"
                }
                val postureColor = if (sensitivePermissions.isEmpty()) {
                    MaterialTheme.colorScheme.tertiary
                } else if (sensitivePermissions.size <= 2) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.error
                }
                val postureDescription = when (postureLabel) {
                    "LOW RISK" -> "No common sensitive permissions detected in this manifest snapshot."
                    "SENSITIVE WATCH" -> "Limited sensitive permissions detected. Monitor permission usage trend."
                    else -> "Elevated sensitive permission footprint. Review permission evidence."
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Permission posture", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileStatusBadge(
                                label = "Total permissions",
                                value = totalPermissionCount.toString(),
                                modifier = Modifier.weight(1f),
                            )
                            ProfileStatusBadge(
                                label = "Sensitive",
                                value = sensitivePermissions.size.toString(),
                                valueColor = postureColor,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            "Risk level: $postureLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = postureColor,
                        )
                        Text(postureDescription, style = MaterialTheme.typography.bodySmall)
                        if (sensitivePermissions.isNotEmpty()) {
                            Text(
                                "Tap a sensitive badge to review permission evidence.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item {
                if (sensitivePermissions.isNotEmpty()) {
                    Text("Sensitive permissions", style = MaterialTheme.typography.titleMedium)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        sensitivePermissions.forEach { permission ->
                            AssistChip(
                                onClick = { onSensitivePermissionClick(permission) },
                                modifier = Modifier.semantics {
                                    contentDescription = "Open evidence for ${readablePermissionName(permission)} permission"
                                },
                                label = {
                                    Text(
                                        text = readablePermissionName(permission),
                                        style = MaterialTheme.typography.titleSmall,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (profile.permissions.isNotEmpty()) {
                item { Text("All permissions ($totalPermissionCount)", style = MaterialTheme.typography.titleMedium) }
                items(orderedPermissions) { permission ->
                    val isSensitive = SensitivePermissionPolicy.isSensitive(permission)
                    PermissionListRow(
                        permission = permission,
                        isSensitive = isSensitive,
                    )
                }
            } else {
                item {
                    Text("No declared permissions found.")
                }
            }

            item {
                Text(
                    "CyxWatch keeps this profile local-only. No payloads or cloud telemetry are collected.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PermissionListRow(
    permission: String,
    isSensitive: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSensitive) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(readablePermissionName(permission), style = MaterialTheme.typography.titleSmall)
                Text(permission, style = MaterialTheme.typography.bodySmall)
            }
            PermissionRiskBadge(
                permissionRiskLabel = if (isSensitive) {
                    "Sensitive"
                } else {
                    "Normal"
                },
                isSensitive = isSensitive,
            )
        }
    }
}

@Composable
private fun PermissionRiskBadge(
    permissionRiskLabel: String,
    isSensitive: Boolean,
) {
    Text(
        permissionRiskLabel,
        style = MaterialTheme.typography.labelSmall,
        color = if (isSensitive) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isSensitive) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
                },
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun ProfileStatusBadge(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.labelMedium, color = valueColor)
        }
    }
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
