package com.cyxwatch.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cyxwatch.app.domain.DailySummary
import com.cyxwatch.app.domain.PrivacyAlert
import com.cyxwatch.app.domain.ScoreReason
import com.cyxwatch.app.domain.ScoringRule
import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class DailySummaryScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shows_empty_summary_state_and_navigates_back() {
        var backClicked = false

        composeRule.setContent {
            DailySummaryScreen(
                summary = DailySummary(
                    dateLabel = "Jun 14, 2026",
                    generatedAt = Instant.parse("2026-06-14T12:00:00Z"),
                    windowStart = Instant.parse("2026-06-13T12:00:00Z"),
                    windowEnd = Instant.parse("2026-06-14T12:00:00Z"),
                    score = 100,
                    topReasons = emptyList(),
                    topAlertCount = 0,
                    recentAlerts = emptyList(),
                    usageEventCount = 0,
                    networkEventCount = 0,
                    inventoryEventCount = 0,
                    topApps = emptyList(),
                ),
                scoringEvents = emptyList(),
                onBack = { backClicked = true },
                onOpenReason = {},
                onOpenAlert = {},
                onOpenAlertProfile = {},
                onOpenTopApp = {},
            )
        }

        composeRule.onNodeWithText("No risk reasons for the current evidence window.").assertExists()
        composeRule.onNodeWithText("No alerts in this window.").assertExists()
        composeRule.onNodeWithText("No app activity is currently loaded.").assertExists()
        composeRule.onNodeWithContentDescription("Back to dashboard from daily summary").assertExists()
        composeRule.onNodeWithText("Back").performClick()

        assertTrue(backClicked)
    }

    @Test
    fun renders_app_links_with_enabled_and_disabled_states() {
        composeRule.setContent {
            DailySummaryScreen(
                summary = DailySummary(
                    dateLabel = "Jun 14, 2026",
                    generatedAt = Instant.parse("2026-06-14T12:00:00Z"),
                    windowStart = Instant.parse("2026-06-13T12:00:00Z"),
                    windowEnd = Instant.parse("2026-06-14T12:00:00Z"),
                    score = 88,
                    topReasons = listOf(
                        ScoreReason(
                            rule = ScoringRule.SensitivePermissionAdded,
                            message = "Sensitive permission(s) added for com.example.focus: android.permission.CAMERA",
                            packageName = "com.example.focus",
                            delta = ScoringRule.SensitivePermissionAdded.delta,
                            evidenceEventIds = listOf("reason-1"),
                        ),
                    ),
                    topAlertCount = 2,
                    recentAlerts = listOf(
                        PrivacyAlert(
                            rule = ScoringRule.SensitivePermissionAdded,
                            packageName = "com.example.focus",
                            message = "Sensitive permission(s) added for com.example.focus in the last 24h.",
                            evidenceEventIds = listOf("reason-1"),
                            triggerDelta = ScoringRule.SensitivePermissionAdded.delta,
                            triggeredAt = Instant.parse("2026-06-14T08:00:00Z"),
                        ),
                        PrivacyAlert(
                            rule = ScoringRule.ScreenOffAppActivity,
                            packageName = "com.example.focus",
                            message = "App activity while screen was off for com.example.focus in the last 24h.",
                            evidenceEventIds = listOf("reason-1"),
                            triggerDelta = ScoringRule.ScreenOffAppActivity.delta,
                            triggeredAt = Instant.parse("2026-06-14T08:00:00Z"),
                        ),
                    ),
                    usageEventCount = 1,
                    networkEventCount = 0,
                    inventoryEventCount = 0,
                    topApps = listOf("com.example.focus", "com.example.missing"),
                ),
                scoringEvents = listOf(
                    PrivacyEvent(
                        eventId = "reason-1",
                        timestamp = Instant.parse("2026-06-14T09:00:00Z"),
                        packageName = "com.example.focus",
                        eventType = EventType.APP_FOREGROUND,
                        severity = Severity.LOW,
                        source = "UsageStats",
                        title = "sample",
                        explanation = "sample",
                        evidenceJson = "{}",
                    ),
                ),
                onBack = {},
                onOpenReason = {},
                onOpenAlert = {},
                onOpenAlertProfile = {},
                onOpenTopApp = {},
                appLabelsByPackageName = mapOf(
                    "com.example.focus" to "Focus App",
                    "com.example.missing" to "Missing App",
                ),
            )
        }

        composeRule.onNodeWithText("Review permission evidence").assertExists()
        composeRule.onNodeWithText("Open supporting evidence").assertExists()
        composeRule.onNodeWithText("App: Focus App (com.example.focus)", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Sensitive permissions: Camera", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Permission warning").assertExists()
        composeRule.onNodeWithContentDescription("Open score reason evidence for com.example.focus").assertExists()
        composeRule.onNodeWithContentDescription("Open alert evidence for com.example.focus").assertExists()
        composeRule.onNodeWithContentDescription("Open app profile from alert com.example.focus").assertExists()
        composeRule.onNodeWithContentDescription("Open app profile for com.example.focus").assertExists()
        composeRule.onNodeWithContentDescription("Open app profile for com.example.missing").assertExists()
        composeRule.onNode(hasText("com.example.focus", substring = true)).assertIsEnabled()
        composeRule.onNode(hasText("com.example.missing", substring = true)).assertIsNotEnabled()
    }

    @Test
    fun shows_scroll_down_button_when_summary_is_long() {
        composeRule.setContent {
            DailySummaryScreen(
                summary = DailySummary(
                    dateLabel = "Jun 14, 2026",
                    generatedAt = Instant.parse("2026-06-14T12:00:00Z"),
                    windowStart = Instant.parse("2026-06-13T12:00:00Z"),
                    windowEnd = Instant.parse("2026-06-14T12:00:00Z"),
                    score = 88,
                    topReasons = (1..16).map {
                        ScoreReason(
                            rule = ScoringRule.ScreenOffAppActivity,
                            message = "App activity while screen was off for com.example.focus (2 observed transition event(s)).",
                            packageName = "com.example.focus",
                            delta = ScoringRule.ScreenOffAppActivity.delta,
                            evidenceEventIds = listOf("reason-$it"),
                        )
                    },
                    topAlertCount = 12,
                    recentAlerts = (1..12).map {
                        PrivacyAlert(
                            rule = ScoringRule.ScreenOffAppActivity,
                            packageName = "com.example.focus",
                            message = "App activity while screen was off for com.example.focus in the last 24h.",
                            evidenceEventIds = listOf("reason-$it"),
                            triggerDelta = ScoringRule.ScreenOffAppActivity.delta,
                            triggeredAt = Instant.parse("2026-06-14T08:${it.toString().padStart(2, '0')}:00Z"),
                        )
                    },
                    usageEventCount = 1,
                    networkEventCount = 0,
                    inventoryEventCount = 0,
                    topApps = (1..16).map { "com.example.focus" },
                ),
                scoringEvents = listOf(
                    PrivacyEvent(
                        eventId = "reason-1",
                        timestamp = Instant.parse("2026-06-14T09:00:00Z"),
                        packageName = "com.example.focus",
                        eventType = EventType.APP_FOREGROUND,
                        severity = Severity.LOW,
                        source = "UsageStats",
                        title = "sample",
                        explanation = "sample",
                        evidenceJson = "{}",
                    ),
                ),
                onBack = {},
                onOpenReason = {},
                onOpenAlert = {},
                onOpenAlertProfile = {},
                onOpenTopApp = {},
                appLabelsByPackageName = mapOf("com.example.focus" to "Focus App"),
            )
        }

        composeRule.onNodeWithContentDescription("Scroll to bottom of report").assertExists()
    }
}
