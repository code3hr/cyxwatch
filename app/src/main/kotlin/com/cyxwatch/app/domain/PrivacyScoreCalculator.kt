package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import java.util.Locale

data class ScoreReason(
    val rule: ScoringRule,
    val message: String,
    val packageName: String,
    val delta: Int,
    val evidenceEventIds: List<String>,
)

data class PrivacyScore(
    val score: Int,
    val reasons: List<ScoreReason>,
)

private const val MAX_SCORE = 100

class PrivacyScoreCalculator {
    fun calculate(events: List<PrivacyEvent>): PrivacyScore {
        val reasons = buildReasons(events)
        val score = MAX_SCORE - reasons.sumOf { it.delta }
        return PrivacyScore(score = score.coerceIn(0, MAX_SCORE), reasons = reasons)
    }

    private fun buildReasons(events: List<PrivacyEvent>): List<ScoreReason> {
        return buildList {
            events
                .filter { it.eventType == EventType.PERMISSION_CHANGED && it.source == "AppInventory" }
                .filter { it.hasUsefulSensitivePermissionSignal() }
                .groupBy { it.packageName }
                .forEach { (packageName, packageEvents) ->
                    val hasNewInstall = packageEvents.any { it.isNewAppInstallEvent() }
                    val hasSensitivePermissionsFromInstall = packageEvents.any {
                        it.isNewAppInstallEvent() && it.sensitivePermissionsInArray("permissions").isNotEmpty()
                    }
                    val hasSensitiveAddedPermissions = packageEvents.any {
                        it.sensitivePermissionsInArray("addedPermissions").isNotEmpty()
                    }

                    val rule = when {
                        hasNewInstall && hasSensitivePermissionsFromInstall -> ScoringRule.NewAppWithSensitivePermissions
                        hasSensitiveAddedPermissions -> ScoringRule.SensitivePermissionAdded
                        else -> return@forEach
                    }

                    add(reason(rule = rule, packageName = packageName, events = packageEvents))
                }

            events
                .filter { it.eventType == EventType.NETWORK_USAGE }
                .groupBy { it.packageName }
                .forEach { (packageName, packageEvents) ->
                    if (packageEvents.isEmpty()) {
                        return@forEach
                    }

                    val hasHighSeverity = packageEvents.any { it.severity == Severity.HIGH }
                    val hasMediumSeverity = packageEvents.any { it.severity == Severity.MEDIUM }
                    val hasLowSeverity = packageEvents.any { it.severity == Severity.LOW }
                    val rule = when {
                        hasHighSeverity -> ScoringRule.HighBackgroundNetwork
                        hasMediumSeverity -> ScoringRule.MediumBackgroundNetwork
                        hasLowSeverity -> ScoringRule.LowBackgroundNetwork
                        else -> ScoringRule.LowBackgroundNetwork
                    }
                    add(reason(rule = rule, packageName = packageName, events = packageEvents))
                }

            events
                .filter { it.eventType == EventType.APP_FOREGROUND && it.source == "UsageStats" }
                .filter {
                    it.evidenceJson.contains("\"screenState\":\"screen_off\"") ||
                        it.evidenceJson.contains("\"source\":\"screen_non_interactive\"")
                }
                .groupBy { it.packageName }
                .forEach { (packageName, packageEvents) ->
                    add(reason(ScoringRule.ScreenOffAppActivity, packageName, packageEvents))
                }

            events
                .filter { it.eventType == EventType.APP_FOREGROUND && it.source == "UsageStats" }
                .filter {
                    it.evidenceJson.contains("\"lockState\":\"keyguard_shown\"")
                }
                .groupBy { it.packageName }
                .forEach { (packageName, packageEvents) ->
                    add(reason(ScoringRule.KeyguardAppActivity, packageName, packageEvents))
                }
        }.sortedWith(
            compareByDescending<ScoreReason> { it.delta }
                .thenBy { it.packageName }
                .thenBy { it.rule.name },
        )
    }

    private fun reason(
        rule: ScoringRule,
        packageName: String,
        events: List<PrivacyEvent>,
    ): ScoreReason {
        val evidenceIds = events
            .map { it.eventId }
            .distinct()
            .sorted()

        return ScoreReason(
            rule = rule,
            message = reasonMessage(rule = rule, packageName = packageName, events = events),
            packageName = packageName,
            delta = rule.delta,
            evidenceEventIds = evidenceIds,
        )
    }

    private fun reasonMessage(
        rule: ScoringRule,
        packageName: String,
        events: List<PrivacyEvent>,
    ): String {
        val eventCount = events.size
        return when (rule) {
            ScoringRule.LowBackgroundNetwork,
            ScoringRule.MediumBackgroundNetwork,
            ScoringRule.HighBackgroundNetwork ->
                networkReasonMessage(packageName, events, rule)
            ScoringRule.SensitivePermissionAdded,
            ScoringRule.NewAppWithSensitivePermissions ->
                permissionReasonMessage(rule, packageName, events)
            ScoringRule.ScreenOffAppActivity,
            ScoringRule.KeyguardAppActivity ->
                "${rule.description} for $packageName (${eventCount} observed transition event(s))."
        }
    }

    private fun networkReasonMessage(
        packageName: String,
        events: List<PrivacyEvent>,
        rule: ScoringRule,
    ): String {
        val totalBytes = events.mapNotNull { it.packageBytesFromEvidence() }.sum()
        val normalizedBytes = if (totalBytes > 0) bytesLabel(totalBytes) else "0 bytes"
        val eventCount = events.size
        return "${rule.description} for $packageName in the last 24h using $eventCount observed bucket(s), totalling $normalizedBytes."
    }

    private fun permissionReasonMessage(
        rule: ScoringRule,
        packageName: String,
        events: List<PrivacyEvent>,
    ): String {
                    val permissions = events
                    .flatMap { event ->
                        if (event.isNewAppInstallEvent()) {
                            event.sensitivePermissionsInArray("permissions")
                        } else {
                            event.sensitivePermissionsInArray("addedPermissions")
                        }
                    }
            .filter { SensitivePermissionPolicy.isSensitive(it) }
                    .distinct()
                    .sorted()

        return if (permissions.isNotEmpty()) {
            "${rule.description} for $packageName (${permissions.joinToString(", ")})."
        } else {
            "${rule.description} for $packageName."
        }
    }

    private fun PrivacyEvent.hasUsefulSensitivePermissionSignal(): Boolean {
        if (isNewAppInstallEvent()) {
            return sensitivePermissionsInArray("permissions").isNotEmpty()
        }
        return sensitivePermissionsInArray("addedPermissions").isNotEmpty()
    }

    private fun PrivacyEvent.isNewAppInstallEvent(): Boolean {
        return evidenceJson.contains("\"kind\":\"new_install\"")
    }

    private fun PrivacyEvent.sensitivePermissionsInArray(arrayName: String): List<String> {
        val arrayJson = permissionArrayRegex(arrayName).find(evidenceJson)?.groupValues?.getOrNull(1) ?: return emptyList()
        return PERMISSION_VALUE_PATTERN.findAll(arrayJson).map { it.groupValues[1] }.toList()
    }

    private fun permissionArrayRegex(arrayName: String): Regex {
        return Regex("\"$arrayName\"\\s*:\\s*\\[(.*?)\\]")
    }

    private fun PrivacyEvent.packageBytesFromEvidence(): Long? {
        return PACKAGE_BYTES_PATTERN.find(evidenceJson)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
    }

    private fun bytesLabel(totalBytes: Long): String {
        val kb = 1024.0
        return when {
            totalBytes >= kb * kb * kb ->
                "%.2f GB".format(Locale.US, totalBytes / (kb * kb * kb))
            totalBytes >= kb * kb ->
                "%.2f MB".format(Locale.US, totalBytes / (kb * kb))
            totalBytes >= kb ->
                "%.2f KB".format(Locale.US, totalBytes / kb)
            else -> "$totalBytes bytes"
        }
    }

    private companion object {
        private val PERMISSION_VALUE_PATTERN =
            Regex("\"(android\\.permission\\.[A-Za-z0-9_\\.]+)\"")
        private val PACKAGE_BYTES_PATTERN =
            Regex("\"packageBytes\"\\s*:\\s*(\\d+)")
    }
}
