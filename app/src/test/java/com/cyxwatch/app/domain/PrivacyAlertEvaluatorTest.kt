package com.cyxwatch.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class PrivacyAlertEvaluatorTest {
    private val now = Instant.parse("2026-06-13T12:00:00Z")
    private val useCase = EvaluatePrivacyAlertsUseCase(cooldown = Duration.ofHours(24))

    @Test
    fun `alerts on high severity network and sensitive permission events`() {
        val result = useCase.evaluate(
            score = PrivacyScore(
                score = 82,
                reasons = listOf(
                    scoreReason(
                        rule = ScoringRule.HighBackgroundNetwork,
                        packageName = "com.example.sync",
                        evidenceEventIds = listOf("network-1"),
                    ),
                    scoreReason(
                        rule = ScoringRule.LowBackgroundNetwork,
                        packageName = "com.example.news",
                        evidenceEventIds = listOf("network-low"),
                    ),
                    scoreReason(
                        rule = ScoringRule.SensitivePermissionAdded,
                        packageName = "com.example.camera",
                        evidenceEventIds = listOf("permission-1"),
                    ),
                ),
            ),
            now = now,
        )

        assertEquals(2, result.alerts.size)
        assertEquals(
            listOf("com.example.sync", "com.example.camera"),
            result.alerts.map { it.packageName },
        )
        assertEquals(0, result.suppressedCount)
    }

    @Test
    fun `suppresses duplicate alerts for the same package and rule within cooldown`() {
        val prior = listOf(
            PrivacyAlert(
                rule = ScoringRule.HighBackgroundNetwork,
                packageName = "com.example.sync",
                message = "High background network observed for com.example.sync in the last 24h (10.00 MB).",
                evidenceEventIds = listOf("network-1"),
                triggerDelta = ScoringRule.HighBackgroundNetwork.delta,
                triggeredAt = now.minus(Duration.ofHours(12)),
            ),
        )
        val result = useCase.evaluate(
            score = PrivacyScore(
                score = 88,
                reasons = listOf(
                    scoreReason(
                        rule = ScoringRule.HighBackgroundNetwork,
                        packageName = "com.example.sync",
                        evidenceEventIds = listOf("network-2"),
                    ),
                ),
            ),
            now = now,
            priorAlerts = prior,
        )

        assertEquals(0, result.alerts.size)
        assertEquals(1, result.suppressedCount)
    }

    @Test
    fun `raises suppressed alert when cooldown has elapsed`() {
        val prior = listOf(
            PrivacyAlert(
                rule = ScoringRule.HighBackgroundNetwork,
                packageName = "com.example.sync",
                message = "High background network observed for com.example.sync in the last 24h (10.00 MB).",
                evidenceEventIds = listOf("network-1"),
                triggerDelta = ScoringRule.HighBackgroundNetwork.delta,
                triggeredAt = now.minus(Duration.ofHours(27)),
            ),
        )

        val result = useCase.evaluate(
            score = PrivacyScore(
                score = 88,
                reasons = listOf(
                    scoreReason(
                        rule = ScoringRule.HighBackgroundNetwork,
                        packageName = "com.example.sync",
                        evidenceEventIds = listOf("network-2"),
                    ),
                ),
            ),
            now = now,
            priorAlerts = prior,
        )

        assertEquals(1, result.alerts.size)
        assertEquals(0, result.suppressedCount)
        assertEquals("com.example.sync", result.alerts[0].packageName)
    }

    @Test
    fun `does not suppress alerts for same package with different rules`() {
        val result = useCase.evaluate(
            score = PrivacyScore(
                score = 80,
                reasons = listOf(
                    ScoreReason(
                        rule = ScoringRule.SensitivePermissionAdded,
                        message = "Sensitive permission added for com.example.mail.",
                        packageName = "com.example.mail",
                        delta = ScoringRule.SensitivePermissionAdded.delta,
                        evidenceEventIds = listOf("permission-1"),
                    ),
                    ScoreReason(
                        rule = ScoringRule.HighBackgroundNetwork,
                        message = "High background network observed for com.example.mail in the last 24h.",
                        packageName = "com.example.mail",
                        delta = ScoringRule.HighBackgroundNetwork.delta,
                        evidenceEventIds = listOf("network-1"),
                    ),
                ),
            ),
            now = now,
            priorAlerts = listOf(
                PrivacyAlert(
                    rule = ScoringRule.SensitivePermissionAdded,
                    packageName = "com.example.mail",
                    message = "Old sensitive permission alert.",
                    evidenceEventIds = listOf("permission-old"),
                    triggerDelta = ScoringRule.SensitivePermissionAdded.delta,
                    triggeredAt = now.minus(Duration.ofHours(12)),
                ),
            ),
        )

        assertEquals(1, result.alerts.size)
        assertEquals(ScoringRule.HighBackgroundNetwork, result.alerts.first().rule)
        assertEquals("com.example.mail", result.alerts.first().packageName)
        assertEquals(1, result.suppressedCount)
    }

    private fun scoreReason(
        rule: ScoringRule,
        packageName: String,
        evidenceEventIds: List<String>,
    ): ScoreReason {
        return ScoreReason(
            rule = rule,
            message = "${rule.description} for $packageName.",
            packageName = packageName,
            delta = rule.delta,
            evidenceEventIds = evidenceEventIds,
        )
    }

}
