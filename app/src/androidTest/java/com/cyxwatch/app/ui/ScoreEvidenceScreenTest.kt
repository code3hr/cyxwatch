package com.cyxwatch.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class ScoreEvidenceScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun back_button_is_reachable_and_evidence_renders() {
        var backClicked = false

        composeRule.setContent {
            ScoreEvidenceScreen(
                reason = ScoreReason(
                    rule = ScoringRule.ScreenOffAppActivity,
                    message = "App used while screen off",
                    packageName = "com.example.app",
                    delta = ScoringRule.ScreenOffAppActivity.delta,
                    evidenceEventIds = listOf("evt-1"),
                ),
                evidenceEvents = listOf(
                    PrivacyEvent(
                        eventId = "evt-1",
                        timestamp = Instant.parse("2026-06-14T09:00:00Z"),
                        packageName = "com.example.app",
                        eventType = EventType.APP_FOREGROUND,
                        severity = Severity.LOW,
                        source = "UsageStats",
                        title = "screen off usage",
                        explanation = "sample explanation",
                        evidenceJson = "{}",
                    ),
                ),
                onBack = { backClicked = true },
                appLabelsByPackageName = mapOf("com.example.app" to "Example App"),
            )
        }

        composeRule.onNodeWithContentDescription("Back from score evidence").performClick()
        composeRule.onNodeWithText("App: Example App (com.example.app)").assertExists()
        composeRule.onNodeWithText("sample explanation").assertExists()

        assertTrue(backClicked)
    }

    @Test
    fun shows_scroll_controls_for_long_evidence() {
        composeRule.setContent {
            ScoreEvidenceScreen(
                reason = ScoreReason(
                    rule = ScoringRule.ScreenOffAppActivity,
                    message = "app active while screen was off",
                    packageName = "com.example.app",
                    delta = ScoringRule.ScreenOffAppActivity.delta,
                    evidenceEventIds = (1..18).map { "evt-$it" },
                ),
                evidenceEvents = (1..18).map { index ->
                    PrivacyEvent(
                        eventId = "evt-$index",
                        timestamp = Instant.parse("2026-06-14T09:%02d:00Z".format(index)),
                        packageName = "com.example.app",
                        eventType = EventType.APP_FOREGROUND,
                        severity = Severity.LOW,
                        source = "UsageStats",
                        title = "event $index",
                        explanation = "event explanation $index",
                        evidenceJson = "{}",
                    )
                },
                onBack = {},
                appLabelsByPackageName = mapOf("com.example.app" to "Example App"),
            )
        }

        composeRule.onNodeWithContentDescription("Scroll to bottom of score evidence").assertExists()
        composeRule.onNodeWithContentDescription("Scroll to bottom of score evidence").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Scroll to top of score evidence").assertExists()
    }
}
