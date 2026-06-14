package com.cyxwatch.app.domain

import java.time.Duration
import java.time.Instant

data class PrivacyAlert(
    val rule: ScoringRule,
    val packageName: String,
    val message: String,
    val evidenceEventIds: List<String>,
    val triggerDelta: Int,
    val triggeredAt: Instant,
)

data class PrivacyAlertResult(
    val alerts: List<PrivacyAlert>,
    val suppressedCount: Int,
)

class EvaluatePrivacyAlertsUseCase(
    private val cooldown: Duration = Duration.ofHours(24),
) {
    fun evaluate(
        score: PrivacyScore,
        now: Instant = Instant.now(),
        priorAlerts: List<PrivacyAlert> = emptyList(),
    ): PrivacyAlertResult {
        val recentSuppressCutoff = now.minus(cooldown)
        var suppressedCount = 0

        val alerts = score.reasons
            .filter(::isAlertableReason)
            .mapNotNull { reason ->
                val alert = reason.toAlert(now)
                if (priorAlerts.any { it.isDuplicate(alert, recentSuppressCutoff) }) {
                    suppressedCount += 1
                    null
                } else {
                    alert
                }
            }
            .toList()

        return PrivacyAlertResult(
            alerts = alerts,
            suppressedCount = suppressedCount,
        )
    }

    private fun isAlertableReason(reason: ScoreReason): Boolean {
        return when (reason.rule) {
            ScoringRule.HighBackgroundNetwork,
            ScoringRule.NewAppWithSensitivePermissions,
            ScoringRule.SensitivePermissionAdded -> true
            ScoringRule.LowBackgroundNetwork,
            ScoringRule.MediumBackgroundNetwork,
            ScoringRule.ScreenOffAppActivity,
            ScoringRule.KeyguardAppActivity -> false
        }
    }

    private fun ScoreReason.toAlert(now: Instant): PrivacyAlert {
        return PrivacyAlert(
            rule = rule,
            packageName = packageName,
            message = message,
            evidenceEventIds = evidenceEventIds,
            triggerDelta = rule.delta,
            triggeredAt = now,
        )
    }

    private fun PrivacyAlert.isDuplicate(
        candidate: PrivacyAlert,
        recentSuppressCutoff: Instant,
    ): Boolean {
        return packageName == candidate.packageName &&
            rule == candidate.rule &&
            triggeredAt.isAfter(recentSuppressCutoff)
    }
}
