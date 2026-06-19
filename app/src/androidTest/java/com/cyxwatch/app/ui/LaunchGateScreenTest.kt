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
class LaunchGateScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun launch_gate_displays_primary_actions_and_callbacks() {
        var startMonitoringClicked = false
        var privacyControlsClicked = false
        var skipClicked = false

        composeRule.setContent {
            LaunchGateScreen(
                onStartMonitoringClick = { startMonitoringClicked = true },
                onOpenPrivacyControlsClick = { privacyControlsClicked = true },
                onBackToDashboardClick = { skipClicked = true },
            )
        }

        composeRule.onNodeWithContentDescription("Start monitoring from launch gate").performClick()
        composeRule.onNodeWithContentDescription("Open privacy controls from launch gate").performClick()
        composeRule.onNodeWithContentDescription("Skip launch gate for now").performClick()

        assertTrue(startMonitoringClicked)
        assertTrue(privacyControlsClicked)
        assertTrue(skipClicked)
        composeRule.onNodeWithContentDescription("Scroll to bottom of launch gate").assertExists()
    }
}
