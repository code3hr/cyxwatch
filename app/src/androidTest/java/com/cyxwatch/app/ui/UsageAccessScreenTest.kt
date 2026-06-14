package com.cyxwatch.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageAccessScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shows_denied_history_and_triggers_callbacks() {
        var openSettingsClicked = false
        var refreshClicked = false

        composeRule.setContent {
            UsageAccessScreen(
                hasUsageAccess = false,
                hasEverDenied = true,
                deniedCount = 2,
                checkCount = 3,
                lastCheckedLabel = "Jun 14, 2026 12:00",
                onOpenSettingsClick = { openSettingsClicked = true },
                onRefreshClick = { refreshClicked = true },
            )
        }

        composeRule.onNodeWithText("Recent check history: denied 2 time(s), opened 3 time(s).").assertExists()
        composeRule.onNodeWithText("Last checked: Jun 14, 2026 12:00").assertExists()
        composeRule.onNodeWithContentDescription("Open usage access settings").assertExists()
        composeRule.onNodeWithContentDescription("Open usage access settings").performClick()
        composeRule.onNodeWithContentDescription("Recheck usage access status").assertExists()
        composeRule.onNodeWithContentDescription("Recheck usage access status").performClick()

        assertTrue(openSettingsClicked)
        assertTrue(refreshClicked)
    }
}
