package com.cyxwatch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max

enum class SignalLevel {
    LOW,
    MEDIUM,
    HIGH,
}

fun signalLevelForRule(rule: com.cyxwatch.app.domain.ScoringRule): SignalLevel {
    return when (rule) {
        com.cyxwatch.app.domain.ScoringRule.HighBackgroundNetwork -> SignalLevel.HIGH
        com.cyxwatch.app.domain.ScoringRule.MediumBackgroundNetwork -> SignalLevel.MEDIUM
        com.cyxwatch.app.domain.ScoringRule.NewAppWithSensitivePermissions,
        com.cyxwatch.app.domain.ScoringRule.SensitivePermissionAdded,
        com.cyxwatch.app.domain.ScoringRule.ScreenOffAppActivity -> SignalLevel.MEDIUM
        else -> SignalLevel.LOW
    }
}

fun signalLevelLabel(level: SignalLevel): String {
    return when (level) {
        SignalLevel.LOW -> "Low"
        SignalLevel.MEDIUM -> "Medium"
        SignalLevel.HIGH -> "High"
    }
}

fun signalLevelForEventSeverity(level: com.cyxwatch.app.domain.model.Severity): SignalLevel {
    return when (level) {
        com.cyxwatch.app.domain.model.Severity.HIGH -> SignalLevel.HIGH
        com.cyxwatch.app.domain.model.Severity.MEDIUM -> SignalLevel.MEDIUM
        com.cyxwatch.app.domain.model.Severity.LOW -> SignalLevel.LOW
    }
}

fun readablePermissionName(permission: String): String {
    return permission
        .removePrefix("android.permission.")
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}

fun extractPermissionValues(rawText: String): List<String> {
    val permissionPattern = Regex("android\\.permission\\.[A-Za-z0-9_\\.]+")
    return permissionPattern
        .findAll(rawText)
        .map { it.value }
        .distinct()
        .toList()
}

fun appDisplayName(
    packageName: String,
    appLabelsByPackageName: Map<String, String>,
): String {
    val label = appLabelsByPackageName[packageName]
    return if (label.isNullOrBlank()) {
        packageName
    } else {
        "$label (${packageName})"
    }
}

fun clampIndex(totalItemCount: Int, index: Int): Int {
    return max(0, minOf(totalItemCount - 1, index))
}

@Composable
fun ScorePanelSurface(
    isCritical: Boolean,
    isSensitive: Boolean,
    modifier: Modifier = Modifier,
    criticalSurfaceColor: Color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.20f),
    sensitiveSurfaceColor: Color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
    defaultSurfaceColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.then(
            if (isCritical) {
                Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.error,
                    MaterialTheme.shapes.small,
                )
            } else {
                Modifier
            },
        ),
        color = when {
            isCritical -> criticalSurfaceColor
            isSensitive -> sensitiveSurfaceColor
            else -> defaultSurfaceColor
        },
        shape = MaterialTheme.shapes.small,
    ) {
        content()
    }
}

@Composable
fun SignalLevelBadge(
    level: SignalLevel,
    label: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Int = 6,
    verticalPadding: Int = 2,
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = horizontalPadding.dp, vertical = verticalPadding.dp),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}
