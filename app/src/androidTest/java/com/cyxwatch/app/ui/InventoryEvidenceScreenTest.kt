package com.cyxwatch.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cyxwatch.app.domain.model.AppProfile
import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class InventoryEvidenceScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun back_button_is_reachable_and_events_render() {
        var backClicked = false

        composeRule.setContent {
            InventoryEvidenceScreen(
                profile = AppProfile(
                    packageName = "com.example.app",
                    label = "Example App",
                    versionName = "1.0.0",
                    versionCode = 1L,
                    firstInstallTimeEpochMs = 1L,
                    lastUpdateTimeEpochMs = 2L,
                    isLaunchable = true,
                    permissions = listOf("android.permission.CAMERA"),
                ),
                permission = "android.permission.CAMERA",
                evidenceEvents = listOf(
                    PrivacyEvent(
                        eventId = "evt-1",
                        timestamp = Instant.parse("2026-06-14T10:00:00Z"),
                        packageName = "com.example.app",
                        eventType = EventType.PERMISSION_CHANGED,
                        severity = Severity.LOW,
                        source = "AppInventory",
                        title = "permission evidence",
                        explanation = "camera permission added",
                        evidenceJson = "{}",
                    ),
                ),
                onBack = { backClicked = true },
            )
        }

        composeRule.onNodeWithContentDescription("Back from permission evidence").performClick()
        composeRule.onNodeWithText("permission evidence").assertExists()

        assertTrue(backClicked)
    }

    @Test
    fun shows_scroll_controls_for_long_events() {
        composeRule.setContent {
            InventoryEvidenceScreen(
                profile = AppProfile(
                    packageName = "com.example.app",
                    label = "Example App",
                    versionName = "1.0.0",
                    versionCode = 1L,
                    firstInstallTimeEpochMs = 1L,
                    lastUpdateTimeEpochMs = 2L,
                    isLaunchable = true,
                    permissions = listOf("android.permission.CAMERA"),
                ),
                permission = "android.permission.CAMERA",
                evidenceEvents = (1..18).map { index ->
                    PrivacyEvent(
                        eventId = "evt-$index",
                        timestamp = Instant.parse("2026-06-14T10:%02d:00Z".format(index)),
                        packageName = "com.example.app",
                        eventType = EventType.PERMISSION_CHANGED,
                        severity = Severity.LOW,
                        source = "AppInventory",
                        title = "permission evidence $index",
                        explanation = "camera permission evidence $index",
                        evidenceJson = "{}",
                    )
                },
                onBack = {},
            )
        }

        composeRule.onNodeWithContentDescription("Scroll to bottom of permission evidence").assertExists()
        composeRule.onNodeWithContentDescription("Scroll to bottom of permission evidence").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Scroll to top of permission evidence").assertExists()
    }
}
