package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertTrue
import java.time.Instant

class PrivacyScoreCalculatorTest {
    private val calculator = PrivacyScoreCalculator()

    @Test
    fun `returns evidence-linked reasons for risky permission changes and activity context`() {
        val events = listOf(
            event(
                id = "permission-1",
                packageName = "com.example.camera",
                eventType = EventType.PERMISSION_CHANGED,
                severity = Severity.MEDIUM,
                source = "AppInventory",
                evidenceJson = """
                    {"kind":"permission_delta","addedPermissions":["android.permission.CAMERA"],"removedPermissions":[]}
                """.trimIndent(),
            ),
            event(
                id = "usage-1",
                packageName = "com.example.camera",
                eventType = EventType.APP_FOREGROUND,
                severity = Severity.LOW,
                source = "UsageStats",
                evidenceJson = """
                    {"source":"foreground_transition","screenState":"screen_off","lockState":"keyguard_shown"}
                """.trimIndent(),
            ),
        )

        val result = calculator.calculate(events)

        assertEquals(100 - 15 - 8 - 6, result.score)
        assertEquals(
            listOf(
                ScoringRule.SensitivePermissionAdded,
                ScoringRule.ScreenOffAppActivity,
                ScoringRule.KeyguardAppActivity,
            ),
            result.reasons.map { it.rule },
        )
        assertEquals(listOf("permission-1"), result.reasons[0].evidenceEventIds)
        assertEquals(listOf("usage-1"), result.reasons[1].evidenceEventIds)
        assertEquals(listOf("usage-1"), result.reasons[2].evidenceEventIds)
    }

    @Test
    fun `groups repeated events by package and rule`() {
        val events = listOf(
            event(
                id = "network-1",
                packageName = "com.example.sync",
                eventType = EventType.NETWORK_USAGE,
                severity = Severity.MEDIUM,
                source = "NetworkStats",
            ),
            event(
                id = "network-2",
                packageName = "com.example.sync",
                eventType = EventType.NETWORK_USAGE,
                severity = Severity.MEDIUM,
                source = "NetworkStats",
            ),
        )

        val result = calculator.calculate(events)

        assertEquals(100 - 6, result.score)
        assertEquals(1, result.reasons.size)
        assertEquals(ScoringRule.MediumBackgroundNetwork, result.reasons[0].rule)
        assertEquals(listOf("network-1", "network-2"), result.reasons[0].evidenceEventIds)
    }

    @Test
    fun `uses low network rule for low-volume network evidence`() {
        val result = calculator.calculate(
            listOf(
                event(
                    id = "network-low",
                    packageName = "com.example.sync",
                    eventType = EventType.NETWORK_USAGE,
                    severity = Severity.LOW,
                    source = "NetworkStats",
                    evidenceJson = """
                        {"source":"network_summary","packageName":"com.example.sync","packageBytes":1048576}
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(100 - 2, result.score)
        assertEquals(ScoringRule.LowBackgroundNetwork, result.reasons[0].rule)
        assertEquals(
            "Low background network observed for com.example.sync in the last 24h using 1 observed bucket(s), totalling 1.00 MB.",
            result.reasons[0].message,
        )
    }

    @Test
    fun `uses new app install rule for newly installed apps with sensitive permissions`() {
        val result = calculator.calculate(
            listOf(
                event(
                    id = "install-sensitive",
                    packageName = "com.example.chat",
                    eventType = EventType.PERMISSION_CHANGED,
                    severity = Severity.LOW,
                    source = "AppInventory",
                    evidenceJson = """
                        {"kind":"new_install","packageName":"com.example.chat","permissions":["android.permission.CAMERA","android.permission.INTERNET"]}
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(listOf(ScoringRule.NewAppWithSensitivePermissions), result.reasons.map { it.rule })
        assertEquals(90, result.score)
        assertEquals(
            "New app has sensitive permissions for com.example.chat (android.permission.CAMERA).",
            result.reasons[0].message,
        )
    }

    @Test
    fun `does not penalize permission removals without sensitive additions`() {
        val result = calculator.calculate(
            listOf(
                event(
                    id = "permission-removed-only",
                    packageName = "com.example.camera",
                    eventType = EventType.PERMISSION_CHANGED,
                    severity = Severity.MEDIUM,
                    source = "AppInventory",
                    evidenceJson = """
                        {"kind":"permission_delta","addedPermissions":[],"removedPermissions":["android.permission.CAMERA"]}
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(100, result.score)
        assertTrue(result.reasons.isEmpty())
    }

    @Test
    fun `includes sensitive permission details in permission reason message`() {
        val result = calculator.calculate(
            listOf(
                event(
                    id = "permission-perms",
                    packageName = "com.example.camera",
                    eventType = EventType.PERMISSION_CHANGED,
                    severity = Severity.MEDIUM,
                    source = "AppInventory",
                    evidenceJson = """
                        {"kind":"permission_delta","addedPermissions":["android.permission.CAMERA","android.permission.READ_CONTACTS"],"removedPermissions":[]}
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(ScoringRule.SensitivePermissionAdded, result.reasons[0].rule)
        assertEquals(
            "Sensitive permission added for com.example.camera (android.permission.CAMERA, android.permission.READ_CONTACTS).",
            result.reasons[0].message,
        )
    }

    @Test
    fun `does not penalize inferred usage gap events by default`() {
        val result = calculator.calculate(
            listOf(
                event(
                    id = "gap-1",
                    packageName = "com.example.reader",
                    eventType = EventType.APP_BACKGROUND,
                    severity = Severity.LOW,
                    source = "UsageStatsBackfill",
                    evidenceJson = """{"source":"usage_gap_backfill"}""",
                ),
            ),
        )

        assertEquals(100, result.score)
        assertEquals(emptyList<ScoreReason>(), result.reasons)
    }

    @Test
    fun `uses high network rule when any network evidence is high severity`() {
        val result = calculator.calculate(
            listOf(
                event(
                    id = "network-high",
                    packageName = "com.example.sync",
                    eventType = EventType.NETWORK_USAGE,
                    severity = Severity.HIGH,
                    source = "NetworkStats",
                ),
                event(
                    id = "network-medium",
                    packageName = "com.example.sync",
                    eventType = EventType.NETWORK_USAGE,
                    severity = Severity.MEDIUM,
                    source = "NetworkStats",
                ),
            ),
        )

        assertEquals(100 - 12, result.score)
        assertEquals(ScoringRule.HighBackgroundNetwork, result.reasons[0].rule)
        assertEquals(listOf("network-high", "network-medium"), result.reasons[0].evidenceEventIds)
    }

    @Test
    fun `includes event count in screen-off reason message`() {
        val result = calculator.calculate(
            listOf(
                event(
                    id = "usage-1",
                    packageName = "com.example.reader",
                    eventType = EventType.APP_FOREGROUND,
                    severity = Severity.LOW,
                    source = "UsageStats",
                    evidenceJson = """{"source":"foreground_transition","screenState":"screen_off"}""",
                ),
                event(
                    id = "usage-2",
                    packageName = "com.example.reader",
                    eventType = EventType.APP_FOREGROUND,
                    severity = Severity.LOW,
                    source = "UsageStats",
                    evidenceJson = """{"source":"foreground_transition","screenState":"screen_off"}""",
                ),
            ),
        )

        assertEquals(1, result.reasons.size)
        assertEquals(ScoringRule.ScreenOffAppActivity, result.reasons[0].rule)
        assertEquals(
            "App activity while screen was off for com.example.reader (2 observed transition event(s)).",
            result.reasons[0].message,
        )
    }

    @Test
    fun `clamps score to zero`() {
        val events = (1..10).flatMap { index ->
            listOf(
                event(
                    id = "permission-$index",
                    packageName = "com.example.app$index",
                    eventType = EventType.PERMISSION_CHANGED,
                    severity = Severity.HIGH,
                    source = "AppInventory",
                    evidenceJson = """
                        {"kind":"permission_delta","addedPermissions":["android.permission.RECORD_AUDIO"]}
                    """.trimIndent(),
                ),
                event(
                    id = "network-$index",
                    packageName = "com.example.app$index",
                    eventType = EventType.NETWORK_USAGE,
                    severity = Severity.HIGH,
                    source = "NetworkStats",
                ),
            )
        }

        val result = calculator.calculate(events)

        assertEquals(0, result.score)
    }

    private fun event(
        id: String,
        packageName: String,
        eventType: EventType,
        severity: Severity,
        source: String,
        evidenceJson: String = "{}",
    ): PrivacyEvent {
        return PrivacyEvent(
            eventId = id,
            timestamp = Instant.parse("2026-06-13T10:00:00Z"),
            packageName = packageName,
            eventType = eventType,
            severity = severity,
            source = source,
            title = "sample",
            explanation = "sample",
            evidenceJson = evidenceJson,
        )
    }
}
