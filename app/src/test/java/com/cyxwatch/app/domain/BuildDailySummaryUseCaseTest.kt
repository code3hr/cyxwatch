package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class BuildDailySummaryUseCaseTest {
    private val now = Instant.parse("2026-06-14T12:00:00Z")
    private val useCase = BuildDailySummaryUseCase(windowDuration = Duration.ofHours(24))

    @Test
    fun `orders top apps by count and ignores blank package names`() {
        val summary = useCase.build(
            score = PrivacyScore(score = 91, reasons = emptyList()),
            events = listOf(
                event("usage-a1", EventType.APP_FOREGROUND, "com.example.alpha"),
                event("usage-a2", EventType.APP_FOREGROUND, "com.example.alpha"),
                event("usage-a3", EventType.APP_FOREGROUND, "com.example.alpha"),
                event("usage-b1", EventType.APP_FOREGROUND, "com.example.bravo"),
                event("usage-b2", EventType.APP_FOREGROUND, "com.example.bravo"),
                event("usage-c1", EventType.APP_FOREGROUND, "com.example.charlie"),
                event("usage-c2", EventType.APP_FOREGROUND, "com.example.charlie"),
                event("usage-d1", EventType.APP_FOREGROUND, "com.example.delta"),
                event("usage-empty", EventType.APP_FOREGROUND, ""),
            ),
            alerts = emptyList(),
            now = now,
        )

        assertEquals(
            listOf("com.example.alpha", "com.example.bravo", "com.example.charlie", "com.example.delta"),
            summary.topApps,
        )
    }

    @Test
    fun `builds summary with event counts and top apps`() {
        val summary = useCase.build(
            score = PrivacyScore(
                score = 82,
                reasons = listOf(
                    ScoreReason(
                        rule = ScoringRule.HighBackgroundNetwork,
                        message = "High background network observed for com.example.sync (50 MB).",
                        packageName = "com.example.sync",
                        delta = ScoringRule.HighBackgroundNetwork.delta,
                        evidenceEventIds = listOf("network-1"),
                    ),
                    ScoreReason(
                        rule = ScoringRule.ScreenOffAppActivity,
                        message = "Screen off activity for com.example.camera (2 observed transition event(s)).",
                        packageName = "com.example.camera",
                        delta = ScoringRule.ScreenOffAppActivity.delta,
                        evidenceEventIds = listOf("usage-1", "usage-2"),
                    ),
                ),
            ),
            events = listOf(
                event("usage-a", EventType.APP_FOREGROUND, "com.example.camera"),
                event("usage-b", EventType.APP_BACKGROUND, "com.example.camera"),
                event("usage-c", EventType.APP_FOREGROUND, "com.example.sync"),
                event("net-1", EventType.NETWORK_USAGE, "com.example.sync"),
                event("inv-1", EventType.PERMISSION_CHANGED, "com.example.chat"),
                event("inv-old", EventType.PERMISSION_CHANGED, "com.example.old", timestamp = now.minus(Duration.ofDays(2))),
            ),
            alerts = listOf(
                alert(
                    rule = ScoringRule.HighBackgroundNetwork,
                    packageName = "com.example.sync",
                    message = "High background network observed for com.example.sync in the last 24h.",
                ),
                alert(
                    rule = ScoringRule.SensitivePermissionAdded,
                    packageName = "com.example.chat",
                    message = "Sensitive permission added for com.example.chat.",
                ),
            ),
            now = now,
        )

        assertEquals(82, summary.score)
        assertEquals(3, summary.usageEventCount)
        assertEquals(1, summary.networkEventCount)
        assertEquals(1, summary.inventoryEventCount)
        assertEquals(2, summary.topAlertCount)
        assertEquals(
            listOf("com.example.camera", "com.example.sync", "com.example.chat"),
            summary.topApps,
        )
    }

    @Test
    fun `filters reasons and alerts to the window`() {
        val summary = useCase.build(
            score = PrivacyScore(
                score = 74,
                reasons = listOf(
                    ScoreReason(
                        rule = ScoringRule.LowBackgroundNetwork,
                        message = "Old evidence should not appear",
                        packageName = "com.example.old",
                        delta = ScoringRule.LowBackgroundNetwork.delta,
                        evidenceEventIds = listOf("old-id"),
                    ),
                    ScoreReason(
                        rule = ScoringRule.ScreenOffAppActivity,
                        message = "In-window evidence should appear",
                        packageName = "com.example.current",
                        delta = ScoringRule.ScreenOffAppActivity.delta,
                        evidenceEventIds = listOf("in-id"),
                    ),
                ),
            ),
            events = listOf(
                event("old-id", EventType.APP_FOREGROUND, "com.example.old", timestamp = now.minus(Duration.ofHours(30))),
                event("in-id", EventType.APP_FOREGROUND, "com.example.current", timestamp = now.minus(Duration.ofHours(1))),
            ),
            alerts = listOf(
                alert(
                    rule = ScoringRule.LowBackgroundNetwork,
                    packageName = "com.example.old",
                    message = "Old alert should not appear.",
                    triggeredAt = now.minus(Duration.ofHours(30)),
                ),
                alert(
                    rule = ScoringRule.ScreenOffAppActivity,
                    packageName = "com.example.current",
                    message = "Current alert should appear.",
                    triggeredAt = now.minus(Duration.ofHours(3)),
                ),
            ),
            now = now,
        )

        assertEquals(1, summary.topAlertCount)
        assertEquals(1, summary.topReasons.size)
        assertEquals("com.example.current", summary.topReasons.first().packageName)
        assertEquals(1, summary.recentAlerts.size)
        assertEquals("com.example.current", summary.recentAlerts.first().packageName)
    }

    @Test
    fun `uses window duration and keeps all alerts in recent list`() {
        val summary = useCase.build(
            score = PrivacyScore(score = 100, reasons = emptyList()),
            events = emptyList(),
            alerts = listOf(
                alert(
                    rule = ScoringRule.SensitivePermissionAdded,
                    packageName = "com.example.photos",
                    message = "Sensitive permissions changed.",
                ),
            ),
            now = now,
        )

        assertEquals(now.minus(Duration.ofHours(24)), summary.windowStart)
        assertEquals(now, summary.windowEnd)
        assertEquals(1, summary.topAlertCount)
        assertEquals(1, summary.recentAlerts.size)
        assertEquals(0, summary.usageEventCount)
        assertEquals(0, summary.networkEventCount)
        assertEquals(0, summary.inventoryEventCount)
        assertEquals(0, summary.topApps.size)
        assertEquals(true, summary.dateLabel.isNotBlank())
    }

    private fun event(
        id: String,
        eventType: EventType,
        packageName: String,
        timestamp: Instant = now,
    ): PrivacyEvent {
        return PrivacyEvent(
            eventId = id,
            timestamp = timestamp,
            packageName = packageName,
            eventType = eventType,
            severity = Severity.LOW,
            source = "test",
            title = "sample",
            explanation = "sample",
            evidenceJson = "{}",
        )
    }

    private fun alert(
        rule: ScoringRule,
        packageName: String,
        message: String,
        triggeredAt: Instant = now,
    ): PrivacyAlert {
        return PrivacyAlert(
            rule = rule,
            packageName = packageName,
            message = message,
            evidenceEventIds = listOf("e1"),
            triggerDelta = rule.delta,
            triggeredAt = triggeredAt,
        )
    }
}
