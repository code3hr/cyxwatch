package com.cyxwatch.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
    fun usage_access_screen_displays_actions_and_scroll_controls() {
        var openSettingsClicked = false
        var refreshClicked = false

        composeRule.setContent {
            UsageAccessScreen(
                hasUsageAccess = false,
                hasEverDenied = true,
                deniedCount = 1,
                checkCount = 2,
                lastCheckedLabel = "2026-06-19T00:00:00Z",
                onOpenSettingsClick = { openSettingsClicked = true },
                onRefreshClick = { refreshClicked = true },
            )
        }

        composeRule.onNodeWithContentDescription("Open usage access settings").performClick()
        composeRule.onNodeWithContentDescription("Recheck usage access status").performClick()
        composeRule.onNodeWithContentDescription("Scroll to bottom of usage access screen").assertExists()

        assertTrue(openSettingsClicked)
        assertTrue(refreshClicked)
    }
}
