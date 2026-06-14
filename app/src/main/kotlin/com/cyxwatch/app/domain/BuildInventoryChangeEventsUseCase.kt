package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.AppInventoryChange
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.Severity
import java.time.Instant

class BuildInventoryChangeEventsUseCase {
    fun buildEvents(
        changes: List<AppInventoryChange>,
        at: Instant,
    ): List<PrivacyEvent> {
        return changes.flatMapIndexed { index, change ->
            when (change) {
                is AppInventoryChange.NewInstall -> listOf(buildInstallEvent(change, at, index))
                is AppInventoryChange.PermissionDelta -> buildPermissionChangeEvents(change, at, index)
            }
        }
    }

    private fun buildInstallEvent(
        change: AppInventoryChange.NewInstall,
        at: Instant,
        index: Int,
    ): PrivacyEvent {
        return PrivacyEvent(
            eventId = "inventory-change-${change.packageName}-$index-install",
            timestamp = at,
            packageName = change.packageName,
            eventType = EventType.PERMISSION_CHANGED,
            severity = Severity.LOW,
            source = "AppInventory",
            title = "New app installed",
            explanation = "${change.installedProfile.label} was newly detected on the device.",
            evidenceJson = buildString {
                append("{")
                append("\"kind\":\"new_install\",")
                append("\"packageName\":${change.packageName.toJsonValue()},")
                append("\"label\":${change.installedProfile.label.toJsonValue()},")
                append("\"versionName\":${change.installedProfile.versionName.toJsonValue()},")
                append("\"versionCode\":${change.installedProfile.versionCode},")
                append("\"permissions\":${change.installedProfile.permissions.toJsonArray()}")
                append("}")
            },
        )
    }

    private fun buildPermissionChangeEvents(
        change: AppInventoryChange.PermissionDelta,
        at: Instant,
        index: Int,
    ): List<PrivacyEvent> {
        if (change.addedPermissions.isEmpty() && change.removedPermissions.isEmpty()) {
            return emptyList()
        }
        return listOf(
            PrivacyEvent(
                eventId = "inventory-change-${change.packageName}-$index-permission-delta",
                timestamp = at,
                packageName = change.packageName,
                eventType = EventType.PERMISSION_CHANGED,
                severity = Severity.MEDIUM,
                source = "AppInventory",
                title = "App permissions changed",
                explanation = "Declared permissions for ${change.packageName} changed.",
                evidenceJson = buildString {
                    append("{")
                    append("\"kind\":\"permission_delta\",")
                    append("\"packageName\":${change.packageName.toJsonValue()},")
                    append("\"addedPermissions\":${change.addedPermissions.toJsonArray()},")
                    append("\"removedPermissions\":${change.removedPermissions.toJsonArray()}")
                    append("}")
                },
            ),
        )
    }
}

private fun List<String>.toJsonArray(): String {
    if (isEmpty()) {
        return "[]"
    }

    return buildString {
        append("[")
        append(joinToString(separator = ",") { permission ->
            permission.toJsonValue()
        })
        append("]")
    }
}

private fun String?.toJsonValue(): String {
    return if (this == null) {
        "null"
    } else {
        buildString {
            append("\"")
            append(this@toJsonValue.jsonEscape())
            append("\"")
        }
    }
}

private fun String.jsonEscape(): String {
    return buildString {
        this@jsonEscape.forEach { ch ->
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch < ' ' ) {
                        append(String.format("\\u%04x", ch.code))
                    } else {
                        append(ch)
                    }
                }
            }
        }
    }
}
