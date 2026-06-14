package com.cyxwatch.app.ui

import kotlin.math.max

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
