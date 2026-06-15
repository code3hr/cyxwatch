package com.cyxwatch.app.ui

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
