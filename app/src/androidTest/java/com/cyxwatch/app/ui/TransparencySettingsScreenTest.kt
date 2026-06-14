package com.cyxwatch.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cyxwatch.app.data.settings.UsageAccessConsentState
import com.cyxwatch.app.domain.RetentionSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransparencySettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun back_and_retention_controls_are_reachable() {
        var backClicked = false
        var retentionDaysSelected = 0
        var vpnModeEnabled = false

        composeRule.setContent {
            TransparencySettingsScreen(
                hasUsageAccess = true,
                consentState = UsageAccessConsentState(
                    hasEverGranted = true,
                    hasEverDenied = false,
                    checkCount = 5,
                    deniedCount = 0,
                    lastCheckedAtEpochMs = 1_700_000_000_000L,
                    lastSettingsOpenedAtEpochMs = 1_700_100_000_000L,
                ),
                lastCheckedLabel = "Jun 14, 2026 12:00",
                lastSettingsOpenedLabel = "Jun 14, 2026 12:01",
                isVpnModeEnabled = false,
                onEnableVpnModeClick = { vpnModeEnabled = true },
                onDisableVpnModeClick = {},
                vpnEnabledAtLabel = "",
                vpnDisabledAtLabel = "",
                retentionSettings = RetentionSettings(retentionDays = 14),
                retentionStatus = "Retention window: 14 days.",
                allowedRetentionDays = listOf(7, 14, 30),
                loadedUsageEventCount = 1,
                loadedInventoryEventCount = 2,
                loadedNetworkEventCount = 3,
                vpnPacketsObserved = 0L,
                vpnBytesObserved = 0L,
                vpnUniqueDestinationCount = 0,
                vpnParsedPacketsObserved = 0L,
                vpnUnparsedPacketsObserved = 0L,
                vpnCaptureMode = "monitor-only",
                vpnForwardingEnabled = false,
                vpnForwardingRequested = false,
                vpnForwardingSupported = false,
                onToggleVpnForwardingModeClick = {},
                onRetentionDaysClick = { retentionDaysSelected = it },
                onPruneNowClick = {},
                onDeleteLoadedEventsClick = {},
                onRefreshVpnDiagnosticsClick = {},
                onBack = { backClicked = true },
            )
        }

        composeRule.onNodeWithContentDescription("Back from privacy settings").performClick()
        composeRule.onNodeWithContentDescription("Enable advanced network visibility").performClick()
        composeRule.onNodeWithContentDescription("Set retention window to 7 days").performClick()
        composeRule.onNodeWithContentDescription("Set retention window to 30 days").performClick()

        assertTrue(backClicked)
        assertTrue(vpnModeEnabled)
        assertEquals(30, retentionDaysSelected)
    }

    @Test
    fun stopping_vpn_visibility_triggers_callback() {
        var vpnModeDisabled = false
        var vpnDiagnosticsRefreshed = false

        composeRule.setContent {
            TransparencySettingsScreen(
                hasUsageAccess = true,
                consentState = UsageAccessConsentState(
                    hasEverGranted = true,
                    hasEverDenied = false,
                    checkCount = 1,
                    deniedCount = 0,
                    lastCheckedAtEpochMs = null,
                    lastSettingsOpenedAtEpochMs = null,
                ),
                lastCheckedLabel = "",
                lastSettingsOpenedLabel = "",
                isVpnModeEnabled = true,
                onEnableVpnModeClick = {},
                onDisableVpnModeClick = { vpnModeDisabled = true },
                vpnEnabledAtLabel = "Jun 14, 2026 12:00",
                vpnDisabledAtLabel = "Jun 14, 2026 11:00",
                retentionSettings = RetentionSettings(retentionDays = 14),
                retentionStatus = "Retention window: 14 days.",
                allowedRetentionDays = listOf(7, 14, 30),
                loadedUsageEventCount = 1,
                loadedInventoryEventCount = 2,
                loadedNetworkEventCount = 3,
                vpnPacketsObserved = 12L,
                vpnBytesObserved = 1280L,
                vpnUniqueDestinationCount = 2,
                vpnParsedPacketsObserved = 11L,
                vpnUnparsedPacketsObserved = 1L,
                vpnCaptureMode = "monitor-only",
                vpnForwardingEnabled = false,
                vpnForwardingRequested = false,
                vpnForwardingSupported = false,
                onToggleVpnForwardingModeClick = {},
                onRetentionDaysClick = {},
                onPruneNowClick = {},
                onDeleteLoadedEventsClick = {},
                onRefreshVpnDiagnosticsClick = { vpnDiagnosticsRefreshed = true },
                onBack = {},
            )
        }

        composeRule.onNodeWithContentDescription("Stop advanced network visibility").performClick()
        composeRule.onNodeWithContentDescription("Refresh VPN visibility diagnostics").performClick()

        assertTrue(vpnModeDisabled)
        assertTrue(vpnDiagnosticsRefreshed)
    }

    @Test
    fun vpn_forwarding_toggle_callback_is_exposed_when_forwarding_is_supported() {
        var requestedForwardingValue = false

        composeRule.setContent {
            TransparencySettingsScreen(
                hasUsageAccess = true,
                consentState = UsageAccessConsentState(
                    hasEverGranted = true,
                    hasEverDenied = false,
                    checkCount = 1,
                    deniedCount = 0,
                    lastCheckedAtEpochMs = null,
                    lastSettingsOpenedAtEpochMs = null,
                ),
                lastCheckedLabel = "",
                lastSettingsOpenedLabel = "",
                isVpnModeEnabled = true,
                onEnableVpnModeClick = {},
                onDisableVpnModeClick = {},
                vpnEnabledAtLabel = "Jun 14, 2026 12:00",
                vpnDisabledAtLabel = "",
                retentionSettings = RetentionSettings(retentionDays = 14),
                retentionStatus = "Retention window: 14 days.",
                allowedRetentionDays = listOf(7, 14, 30),
                loadedUsageEventCount = 1,
                loadedInventoryEventCount = 2,
                loadedNetworkEventCount = 3,
                vpnPacketsObserved = 0L,
                vpnBytesObserved = 0L,
                vpnUniqueDestinationCount = 0,
                vpnParsedPacketsObserved = 0L,
                vpnUnparsedPacketsObserved = 0L,
                vpnCaptureMode = "monitor-only",
                vpnForwardingEnabled = false,
                vpnForwardingRequested = false,
                vpnForwardingSupported = true,
                onToggleVpnForwardingModeClick = { requestedForwardingValue = it },
                onRetentionDaysClick = {},
                onPruneNowClick = {},
                onDeleteLoadedEventsClick = {},
                onRefreshVpnDiagnosticsClick = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithContentDescription("Enable VPN forwarding mode").performClick()

        assertTrue(requestedForwardingValue)
    }

    @Test
    fun forwarding_request_note_is_shown_when_forwarding_is_unsupported() {
        composeRule.setContent {
            TransparencySettingsScreen(
                hasUsageAccess = true,
                consentState = UsageAccessConsentState(
                    hasEverGranted = true,
                    hasEverDenied = false,
                    checkCount = 1,
                    deniedCount = 0,
                    lastCheckedAtEpochMs = null,
                    lastSettingsOpenedAtEpochMs = null,
                ),
                lastCheckedLabel = "",
                lastSettingsOpenedLabel = "",
                isVpnModeEnabled = true,
                onEnableVpnModeClick = {},
                onDisableVpnModeClick = {},
                vpnEnabledAtLabel = "Jun 14, 2026 12:00",
                vpnDisabledAtLabel = "",
                retentionSettings = RetentionSettings(retentionDays = 14),
                retentionStatus = "Retention window: 14 days.",
                allowedRetentionDays = listOf(7, 14, 30),
                loadedUsageEventCount = 1,
                loadedInventoryEventCount = 2,
                loadedNetworkEventCount = 3,
                vpnPacketsObserved = 0L,
                vpnBytesObserved = 0L,
                vpnUniqueDestinationCount = 0,
                vpnParsedPacketsObserved = 0L,
                vpnUnparsedPacketsObserved = 0L,
                vpnCaptureMode = "monitor-only",
                vpnForwardingEnabled = false,
                vpnForwardingRequested = true,
                vpnForwardingSupported = false,
                onToggleVpnForwardingModeClick = {},
                onRetentionDaysClick = {},
                onPruneNowClick = {},
                onDeleteLoadedEventsClick = {},
                onRefreshVpnDiagnosticsClick = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText("Forwarding is not active in this build.").assertExists()
        composeRule.onNodeWithText("Forwarding mode is currently not available in this build.").assertExists()
    }

    @Test
    fun advanced_vpn_visibility_is_explained_as_non_private_mode() {
        composeRule.setContent {
            TransparencySettingsScreen(
                hasUsageAccess = true,
                consentState = UsageAccessConsentState(
                    hasEverGranted = true,
                    hasEverDenied = false,
                    checkCount = 1,
                    deniedCount = 0,
                    lastCheckedAtEpochMs = null,
                    lastSettingsOpenedAtEpochMs = null,
                ),
                lastCheckedLabel = "",
                lastSettingsOpenedLabel = "",
                isVpnModeEnabled = true,
                onEnableVpnModeClick = {},
                onDisableVpnModeClick = {},
                vpnEnabledAtLabel = "Jun 14, 2026 12:00",
                vpnDisabledAtLabel = "",
                retentionSettings = RetentionSettings(retentionDays = 14),
                retentionStatus = "Retention window: 14 days.",
                allowedRetentionDays = listOf(7, 14, 30),
                loadedUsageEventCount = 1,
                loadedInventoryEventCount = 2,
                loadedNetworkEventCount = 3,
                vpnPacketsObserved = 0L,
                vpnBytesObserved = 0L,
                vpnUniqueDestinationCount = 0,
                vpnParsedPacketsObserved = 0L,
                vpnUnparsedPacketsObserved = 0L,
                vpnCaptureMode = "monitor-only",
                vpnForwardingEnabled = false,
                vpnForwardingRequested = false,
                vpnForwardingSupported = false,
                onToggleVpnForwardingModeClick = {},
                onRetentionDaysClick = {},
                onPruneNowClick = {},
                onDeleteLoadedEventsClick = {},
                onRefreshVpnDiagnosticsClick = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText(
            "Advanced network visibility is for local traffic transparency only and is not a private/anonymous VPN.",
        ).assertExists()
        composeRule.onNodeWithText(
            "This mode is for endpoint metadata visibility and does not secure, hide, or reroute your traffic.",
        ).assertExists()
        composeRule.onNodeWithText(
            "Note: packet payloads are never captured in VPN visibility. It is a local monitoring mode, not a private network tunnel.",
        ).assertExists()
    }
}
