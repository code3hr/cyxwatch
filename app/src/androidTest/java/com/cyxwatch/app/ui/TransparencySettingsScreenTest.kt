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
                state = makeState(
                    hasUsageAccess = true,
                    consentState = UsageAccessConsentState(
                        hasEverGranted = true,
                        hasEverDenied = false,
                        checkCount = 5,
                        deniedCount = 0,
                        lastCheckedAtEpochMs = 1_700_000_000_000L,
                        lastSettingsOpenedAtEpochMs = 1_700_100_000_000L,
                    ),
                    isVpnModeEnabled = false,
                    lastCheckedLabel = "Jun 14, 2026 12:00",
                    lastSettingsOpenedLabel = "Jun 14, 2026 12:01",
                ),
                onEnableVpnModeClick = { vpnModeEnabled = true },
                onDisableVpnModeClick = {},
                onToggleVpnForwardingModeClick = {},
                onToggleSecureScreenModeClick = {},
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
        composeRule.onNodeWithText("Loaded evidence").assertExists()
        composeRule.onNodeWithText("Usage").assertExists()
        composeRule.onNodeWithText("Inventory").assertExists()
        composeRule.onNodeWithText("Network").assertExists()

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
                state = makeState(
                    hasUsageAccess = true,
                    consentState = UsageAccessConsentState(
                        hasEverGranted = true,
                        hasEverDenied = false,
                        checkCount = 1,
                        deniedCount = 0,
                        lastCheckedAtEpochMs = null,
                        lastSettingsOpenedAtEpochMs = null,
                    ),
                    isVpnModeEnabled = true,
                    vpnEnabledAtLabel = "Jun 14, 2026 12:00",
                    vpnDisabledAtLabel = "Jun 14, 2026 11:00",
                    vpnPacketsObserved = 12L,
                    vpnBytesObserved = 1280L,
                    vpnUniqueDestinationCount = 2,
                    vpnParsedPacketsObserved = 11L,
                    vpnUnparsedPacketsObserved = 1L,
                ),
                onEnableVpnModeClick = {},
                onDisableVpnModeClick = { vpnModeDisabled = true },
                onToggleVpnForwardingModeClick = {},
                onToggleSecureScreenModeClick = {},
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
                state = makeState(
                    hasUsageAccess = true,
                    consentState = UsageAccessConsentState(
                        hasEverGranted = true,
                        hasEverDenied = false,
                        checkCount = 1,
                        deniedCount = 0,
                        lastCheckedAtEpochMs = null,
                        lastSettingsOpenedAtEpochMs = null,
                    ),
                    isVpnModeEnabled = true,
                    vpnEnabledAtLabel = "Jun 14, 2026 12:00",
                    vpnForwardingSupported = true,
                ),
                onEnableVpnModeClick = {},
                onDisableVpnModeClick = {},
                onToggleVpnForwardingModeClick = { requestedForwardingValue = it },
                onToggleSecureScreenModeClick = {},
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
                state = makeState(
                    hasUsageAccess = true,
                    consentState = UsageAccessConsentState(
                        hasEverGranted = true,
                        hasEverDenied = false,
                        checkCount = 1,
                        deniedCount = 0,
                        lastCheckedAtEpochMs = null,
                        lastSettingsOpenedAtEpochMs = null,
                    ),
                    isVpnModeEnabled = true,
                    vpnEnabledAtLabel = "Jun 14, 2026 12:00",
                    vpnForwardingRequested = true,
                    vpnForwardingSupported = false,
                ),
                onEnableVpnModeClick = {},
                onDisableVpnModeClick = {},
                onToggleVpnForwardingModeClick = {},
                onToggleSecureScreenModeClick = {},
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
                state = makeState(
                    hasUsageAccess = true,
                    consentState = UsageAccessConsentState(
                        hasEverGranted = true,
                        hasEverDenied = false,
                        checkCount = 1,
                        deniedCount = 0,
                        lastCheckedAtEpochMs = null,
                        lastSettingsOpenedAtEpochMs = null,
                    ),
                    isVpnModeEnabled = true,
                    vpnEnabledAtLabel = "Jun 14, 2026 12:00",
                ),
                onEnableVpnModeClick = {},
                onDisableVpnModeClick = {},
                onToggleVpnForwardingModeClick = {},
                onToggleSecureScreenModeClick = {},
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
        composeRule.onNodeWithText(
            "This is local monitoring only. It does not provide anonymity or traffic tunneling.",
        ).assertExists()
        composeRule.onNodeWithText("All processing and storage remain on-device. No cloud sync or packet payload collection.").assertExists()
    }

    @Test
    fun long_privacy_settings_expose_scroll_control() {
        composeRule.setContent {
            TransparencySettingsScreen(
                state = makeState(
                    hasUsageAccess = true,
                    consentState = UsageAccessConsentState(
                        hasEverGranted = true,
                        hasEverDenied = false,
                        checkCount = 5,
                        deniedCount = 0,
                        lastCheckedAtEpochMs = 1_700_000_000_000L,
                        lastSettingsOpenedAtEpochMs = 1_700_100_000_000L,
                    ),
                    isVpnModeEnabled = true,
                    lastCheckedLabel = "Jun 14, 2026 12:00",
                    lastSettingsOpenedLabel = "Jun 14, 2026 12:01",
                    vpnEnabledAtLabel = "Jun 14, 2026 12:00",
                    vpnDisabledAtLabel = "Jun 14, 2026 11:00",
                    vpnPacketsObserved = 12L,
                    vpnBytesObserved = 1280L,
                    vpnUniqueDestinationCount = 2,
                    vpnParsedPacketsObserved = 11L,
                    vpnUnparsedPacketsObserved = 1L,
                    vpnForwardingRequested = true,
                ),
                onEnableVpnModeClick = {},
                onDisableVpnModeClick = {},
                onToggleVpnForwardingModeClick = {},
                onToggleSecureScreenModeClick = {},
                onRetentionDaysClick = {},
                onPruneNowClick = {},
                onDeleteLoadedEventsClick = {},
                onRefreshVpnDiagnosticsClick = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithContentDescription("Scroll to bottom of privacy settings").assertExists()
    }

    @Test
    fun secure_screen_mode_toggle_callback_is_exposed() {
        var secureModeToggled = false

        composeRule.setContent {
            TransparencySettingsScreen(
                state = makeState(
                    hasUsageAccess = true,
                    consentState = UsageAccessConsentState(
                        hasEverGranted = true,
                        hasEverDenied = false,
                        checkCount = 1,
                        deniedCount = 0,
                        lastCheckedAtEpochMs = null,
                        lastSettingsOpenedAtEpochMs = null,
                    ),
                    isVpnModeEnabled = false,
                ),
                onEnableVpnModeClick = {},
                onDisableVpnModeClick = {},
                onToggleVpnForwardingModeClick = {},
                onToggleSecureScreenModeClick = { secureModeToggled = true },
                onRetentionDaysClick = {},
                onPruneNowClick = {},
                onDeleteLoadedEventsClick = {},
                onRefreshVpnDiagnosticsClick = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText("Secure screen mode is disabled").assertExists()
        composeRule.onNodeWithContentDescription("Enable secure screen mode").performClick()

        assertTrue(secureModeToggled)
    }

    private fun makeState(
        hasUsageAccess: Boolean,
        consentState: UsageAccessConsentState,
        isVpnModeEnabled: Boolean,
        lastCheckedLabel: String = "",
        lastSettingsOpenedLabel: String = "",
        vpnEnabledAtLabel: String = "",
        vpnDisabledAtLabel: String = "",
        retentionDays: Int = 14,
        retentionStatus: String = "Retention window: $retentionDays days.",
        allowedRetentionDays: List<Int> = listOf(7, 14, 30),
        loadedUsageEventCount: Int = 1,
        loadedInventoryEventCount: Int = 2,
        loadedNetworkEventCount: Int = 3,
        vpnPacketsObserved: Long = 0L,
        vpnBytesObserved: Long = 0L,
        vpnUniqueDestinationCount: Int = 0,
        vpnParsedPacketsObserved: Long = 0L,
        vpnUnparsedPacketsObserved: Long = 0L,
        vpnCaptureMode: String = "monitor-only",
        vpnForwardingEnabled: Boolean = false,
        vpnForwardingRequested: Boolean = false,
        vpnForwardingSupported: Boolean = false,
        isSecureScreenModeEnabled: Boolean = false,
    ) = TransparencySettingsUiState(
        hasUsageAccess = hasUsageAccess,
        consentState = consentState,
        lastCheckedLabel = lastCheckedLabel,
        lastSettingsOpenedLabel = lastSettingsOpenedLabel,
        isVpnModeEnabled = isVpnModeEnabled,
        vpnEnabledAtLabel = vpnEnabledAtLabel,
        vpnDisabledAtLabel = vpnDisabledAtLabel,
        retentionSettings = RetentionSettings(retentionDays = retentionDays),
        retentionStatus = retentionStatus,
        allowedRetentionDays = allowedRetentionDays,
        loadedUsageEventCount = loadedUsageEventCount,
        loadedInventoryEventCount = loadedInventoryEventCount,
        loadedNetworkEventCount = loadedNetworkEventCount,
        vpnPacketsObserved = vpnPacketsObserved,
        vpnBytesObserved = vpnBytesObserved,
        vpnUniqueDestinationCount = vpnUniqueDestinationCount,
        vpnParsedPacketsObserved = vpnParsedPacketsObserved,
        vpnUnparsedPacketsObserved = vpnUnparsedPacketsObserved,
        vpnCaptureMode = vpnCaptureMode,
        vpnForwardingEnabled = vpnForwardingEnabled,
        vpnForwardingRequested = vpnForwardingRequested,
        vpnForwardingSupported = vpnForwardingSupported,
        isSecureScreenModeEnabled = isSecureScreenModeEnabled,
    )
}
