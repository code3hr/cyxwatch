package com.cyxwatch.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cyxwatch.app.domain.model.AppProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppProfileScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun back_button_and_sensitive_permission_chip_are_discoverable() {
        var backClicked = false
        var clickedPermission: String? = null

        composeRule.setContent {
            AppProfileScreen(
                profile = AppProfile(
                    packageName = "com.example.app",
                    label = "Example App",
                    versionName = "1.0.0",
                    versionCode = 1L,
                    firstInstallTimeEpochMs = 1_700_000_000_000L,
                    lastUpdateTimeEpochMs = 1_700_100_000_000L,
                    isLaunchable = true,
                    permissions = listOf("android.permission.CAMERA", "android.permission.INTERNET"),
                ),
                onBack = { backClicked = true },
                onSensitivePermissionClick = { clickedPermission = it },
            )
        }

        composeRule.onNodeWithContentDescription("Back to previous app profile screen").performClick()
        composeRule.onNodeWithContentDescription("Open evidence for Camera permission").performClick()

        assertTrue(backClicked)
        assertEquals("android.permission.CAMERA", clickedPermission)
    }
}
