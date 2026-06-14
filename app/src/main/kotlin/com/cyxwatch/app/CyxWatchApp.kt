package com.cyxwatch.app

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cyxwatch.app.data.inventory.SharedPrefsAppInventorySnapshotRepository
import com.cyxwatch.app.data.network.SharedPrefsNetworkUsageTotalsRepository
import com.cyxwatch.app.data.settings.RetentionSettingsRepository
import com.cyxwatch.app.data.settings.VpnModeSettingsRepository
import com.cyxwatch.app.data.settings.UsageAccessConsentRepository
import com.cyxwatch.app.domain.BuildInventoryChangeEventsUseCase
import com.cyxwatch.app.domain.BuildDailySummaryUseCase
import com.cyxwatch.app.domain.CollectionThrottle
import com.cyxwatch.app.domain.CollectNetworkUsageEventsUseCase
import com.cyxwatch.app.domain.CollectUsageEventsUseCase
import com.cyxwatch.app.domain.DetectAppInventoryChangesUseCase
import com.cyxwatch.app.domain.EvaluatePrivacyAlertsUseCase
import com.cyxwatch.app.domain.PrivacyAlert
import com.cyxwatch.app.domain.DailySummary
import com.cyxwatch.app.domain.PrivacyScore
import com.cyxwatch.app.domain.PrivacyScoreCalculator
import com.cyxwatch.app.domain.ReadInstalledAppInventoryUseCase
import com.cyxwatch.app.domain.RefreshInstalledAppInventoryUseCase
import com.cyxwatch.app.domain.RetentionPolicy
import com.cyxwatch.app.domain.RetentionSettings
import com.cyxwatch.app.domain.ScoreReason
import com.cyxwatch.app.domain.UsageCollectionResult
import com.cyxwatch.app.domain.model.AppProfile
import com.cyxwatch.app.domain.model.AppInventoryChange
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.platform.inventory.AndroidAppInventoryRepository
import com.cyxwatch.app.platform.network.AndroidNetworkUsageCollector
import com.cyxwatch.app.platform.network.CyxWatchVpnService
import com.cyxwatch.app.platform.network.AndroidVpnModeNetworkUsageCollector
import com.cyxwatch.app.platform.network.VpnModeTrafficStore
import com.cyxwatch.app.platform.notifications.CyxWatchNotifier
import com.cyxwatch.app.platform.permissions.UsageAccessPermissionStateProvider
import com.cyxwatch.app.platform.permissions.hasUsageAccess as hasUsageAccessGranted
import com.cyxwatch.app.platform.permissions.openUsageAccessSettingsIntent
import com.cyxwatch.app.platform.usage.AndroidUsageEventCollector
import com.cyxwatch.app.ui.AppProfileScreen
import com.cyxwatch.app.ui.DailySummaryScreen
import com.cyxwatch.app.ui.InventoryEvidenceScreen
import com.cyxwatch.app.ui.ScoreEvidenceScreen
import com.cyxwatch.app.ui.TransparencySettingsScreen
import com.cyxwatch.app.ui.UsageAccessScreen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyxWatchApp() {
    val context = LocalContext.current
    val consentRepository = remember { UsageAccessConsentRepository(context) }
    val vpnModeSettingsRepository = remember { VpnModeSettingsRepository(context) }
    val retentionSettingsRepository = remember { RetentionSettingsRepository(context) }
    val usagePermissionStateProvider = remember { UsageAccessPermissionStateProvider(context) }
    val usageAccessCollectorUseCase = remember {
        CollectUsageEventsUseCase(
            permissionStateProvider = usagePermissionStateProvider,
            usageEventCollector = AndroidUsageEventCollector(context),
        )
    }
    val collectNetworkUsageUseCase = remember {
        val baseNetworkUsageCollector = AndroidNetworkUsageCollector(context)
        val vpnNetworkUsageCollector = AndroidVpnModeNetworkUsageCollector(baseNetworkUsageCollector)
        CollectNetworkUsageEventsUseCase(
            permissionStateProvider = usagePermissionStateProvider,
            basicNetworkUsageCollector = baseNetworkUsageCollector,
            vpnModeNetworkUsageCollector = vpnNetworkUsageCollector,
            networkUsageTotalsRepository = SharedPrefsNetworkUsageTotalsRepository(context),
        )
    }
    val installedAppInventoryUseCase = remember {
        ReadInstalledAppInventoryUseCase(
            appInventoryRepository = AndroidAppInventoryRepository(context),
        )
    }
    val inventorySnapshotRepository = remember { SharedPrefsAppInventorySnapshotRepository(context) }
    val detectAppInventoryChangesUseCase = remember { DetectAppInventoryChangesUseCase() }
    val buildInventoryChangeEventsUseCase = remember { BuildInventoryChangeEventsUseCase() }
    val buildDailySummaryUseCase = remember { BuildDailySummaryUseCase() }
    val privacyScoreCalculator = remember { PrivacyScoreCalculator() }
    val evaluatePrivacyAlertsUseCase = remember { EvaluatePrivacyAlertsUseCase() }
    val retentionPolicy = remember { RetentionPolicy() }
    val collectionThrottle = remember { CollectionThrottle() }
    val refreshAppInventoryUseCase = remember {
        RefreshInstalledAppInventoryUseCase(
            readInstalledAppInventoryUseCase = installedAppInventoryUseCase,
            snapshotRepository = inventorySnapshotRepository,
            detectAppInventoryChangesUseCase = detectAppInventoryChangesUseCase,
            buildInventoryChangeEventsUseCase = buildInventoryChangeEventsUseCase,
        )
    }
    val permissionWarningNotifier = remember { CyxWatchNotifier(context) }

    var hasUsageAccess by remember { mutableStateOf(hasUsageAccessGranted(context)) }
    var consentState by remember { mutableStateOf(consentRepository.readState()) }
    var collectStatus by remember { mutableStateOf("No data collected yet.") }
    var networkStatus by remember { mutableStateOf("No network data collected yet.") }
    var inventoryStatus by remember { mutableStateOf("No app inventory loaded.") }
    var inventoryChangeStatus by remember { mutableStateOf("No inventory deltas yet.") }
    var retentionSettings by remember { mutableStateOf(retentionSettingsRepository.readSettings()) }
    var retentionStatus by remember { mutableStateOf("Retention window: ${retentionSettings.retentionDays} days.") }
    var vpnModeState by remember { mutableStateOf(vpnModeSettingsRepository.readState()) }
    var lastUsageEvents by remember { mutableStateOf<List<PrivacyEvent>>(emptyList()) }
    var lastNetworkEvents by remember { mutableStateOf<List<PrivacyEvent>>(emptyList()) }
    var lastInventoryProfiles by remember { mutableStateOf<List<AppProfile>>(emptyList()) }
    var lastInventoryEvents by remember { mutableStateOf<List<PrivacyEvent>>(emptyList()) }
    var selectedProfile by remember { mutableStateOf<AppProfile?>(null) }
    var selectedPermissionForEvidence by remember { mutableStateOf<String?>(null) }
    var selectedScoreReason by remember { mutableStateOf<ScoreReason?>(null) }
    var selectedDailySummary by remember { mutableStateOf<DailySummary?>(null) }
    var isTransparencySettingsOpen by remember { mutableStateOf(false) }
    var activeAlerts by remember { mutableStateOf<List<PrivacyAlert>>(emptyList()) }
    var suppressedAlertCount by remember { mutableStateOf(0) }
    var vpnTrafficTotals by remember { mutableStateOf(VpnModeTrafficStore.shared.snapshotTotals()) }
    val scoringEvents = lastUsageEvents + lastNetworkEvents + lastInventoryEvents
    val privacyScore = privacyScoreCalculator.calculate(scoringEvents)
    val appLabelsByPackageName = lastInventoryProfiles
        .associate { it.packageName to it.label }

    fun refreshAlerts() {
        val now = Instant.now()
        val currentScoringEvents = lastUsageEvents + lastNetworkEvents + lastInventoryEvents
        val currentScore = privacyScoreCalculator.calculate(currentScoringEvents)
        val evaluation = evaluatePrivacyAlertsUseCase.evaluate(
            score = currentScore,
            now = now,
            priorAlerts = activeAlerts,
        )
        val newAlerts = evaluation.alerts
        newAlerts
            .filter { it.rule.isSensitivePermissionWarning() }
            .forEach { alert ->
                permissionWarningNotifier.postPermissionWarning(
                    alert = alert,
                    appDisplayName = appLabelsByPackageName[alert.packageName] ?: alert.packageName,
                )
            }
        activeAlerts = (activeAlerts + newAlerts)
            .sortedByDescending { it.triggeredAt }
            .take(25)
        suppressedAlertCount = evaluation.suppressedCount
    }

    fun clearAlertHistory() {
        activeAlerts = emptyList()
        suppressedAlertCount = 0
    }

    fun applyRetentionDays(days: Int) {
        retentionSettings = retentionSettingsRepository.writeRetentionDays(days)
        val pruneResult = pruneLoadedEvents(
            usageEvents = lastUsageEvents,
            inventoryEvents = lastInventoryEvents,
            networkEvents = lastNetworkEvents,
            retentionSettings = retentionSettings,
            retentionPolicy = retentionPolicy,
        )
        lastUsageEvents = pruneResult.usageEvents
        lastInventoryEvents = pruneResult.inventoryEvents
        lastNetworkEvents = pruneResult.networkEvents
        retentionStatus = "Retention window: ${retentionSettings.retentionDays} days. Pruned ${pruneResult.prunedCount} loaded event(s)."
        refreshAlerts()
    }

    fun pruneLoadedEvidenceNow() {
        val pruneResult = pruneLoadedEvents(
            usageEvents = lastUsageEvents,
            inventoryEvents = lastInventoryEvents,
            networkEvents = lastNetworkEvents,
            retentionSettings = retentionSettings,
            retentionPolicy = retentionPolicy,
        )
        lastUsageEvents = pruneResult.usageEvents
        lastInventoryEvents = pruneResult.inventoryEvents
        lastNetworkEvents = pruneResult.networkEvents
        retentionStatus = "Pruned ${pruneResult.prunedCount} loaded event(s) older than ${retentionSettings.retentionDays} days."
        refreshAlerts()
    }

    fun deleteLoadedEvidence() {
        val deletedCount = lastUsageEvents.size + lastInventoryEvents.size + lastNetworkEvents.size
        lastUsageEvents = emptyList()
        lastInventoryEvents = emptyList()
        lastNetworkEvents = emptyList()
        selectedScoreReason = null
        retentionStatus = "Deleted $deletedCount loaded evidence event(s)."
        clearAlertHistory()
    }

    fun enableVpnMode() {
        val now = System.currentTimeMillis()
        try {
            CyxWatchVpnService.start(context)
            vpnModeState = vpnModeSettingsRepository.setEnabled(isEnabled = true, changedAtEpochMs = now)
            vpnTrafficTotals = VpnModeTrafficStore.shared.snapshotTotals()
        } catch (_: Exception) {
            vpnModeState = vpnModeSettingsRepository.setEnabled(isEnabled = false, changedAtEpochMs = now)
        }
    }

    val vpnConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            enableVpnMode()
        } else {
            vpnModeState = vpnModeSettingsRepository.readState()
        }
    }

    fun requestVpnModeEnable() {
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent == null) {
            enableVpnMode()
        } else {
            vpnConsentLauncher.launch(prepareIntent)
        }
    }

    fun disableVpnMode() {
        val now = System.currentTimeMillis()
        CyxWatchVpnService.stop(context)
        vpnModeState = vpnModeSettingsRepository.setEnabled(
            isEnabled = false,
            changedAtEpochMs = now,
        )
        vpnTrafficTotals = VpnModeTrafficStore.shared.snapshotTotals()
    }

    fun setVpnForwardingEnabled(enabled: Boolean) {
        val wasVpnModeActive = vpnModeState.isEnabled
        vpnModeState = vpnModeSettingsRepository.setForwardingEnabled(enabled)
        if (wasVpnModeActive) {
            CyxWatchVpnService.start(context)
        }
        vpnTrafficTotals = VpnModeTrafficStore.shared.snapshotTotals()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(topBar = { TopAppBar(title = { Text("CyxWatch") }) }) { contentPadding ->
                if (!hasUsageAccess) {
                    UsageAccessScreen(
                        hasUsageAccess = hasUsageAccess,
                        hasEverDenied = consentState.hasEverDenied,
                        deniedCount = consentState.deniedCount,
                        checkCount = consentState.checkCount,
                        lastCheckedLabel = formatTimestamp(consentState.lastCheckedAtEpochMs),
                        onOpenSettingsClick = {
                            consentRepository.recordSettingsOpened(System.currentTimeMillis())
                            context.startActivity(openUsageAccessSettingsIntent())
                        },
                        onRefreshClick = {
                            hasUsageAccess = hasUsageAccessGranted(context)
                            consentRepository.recordCheckResult(hasUsageAccess, System.currentTimeMillis())
                            consentState = consentRepository.readState()
                        },
                    )
                } else if (
                    selectedProfile == null &&
                    selectedScoreReason == null &&
                    selectedDailySummary == null &&
                    !isTransparencySettingsOpen
                ) {
                    DashboardShell(
                        modifier = Modifier.padding(contentPadding),
                        collectionStatus = collectStatus,
                        onCollectEventsClick = {
                            val now = Instant.now()
                            val throttleDecision = collectionThrottle.tryCollect("collect_usage", now)
                            if (!throttleDecision.allowed) {
                                collectStatus = "Usage collection is throttled. Try again in ${throttleDecision.retryAfterSeconds} second(s)."
                                return@DashboardShell
                            }
                            when (val result = usageAccessCollectorUseCase.collectLast24hUsageEvents()) {
                                is UsageCollectionResult.Success -> {
                                    val retainedEvents = retentionPolicy.pruneEvents(
                                        events = result.events,
                                        retentionDays = retentionSettings.retentionDays,
                                        now = now,
                                    )
                                    lastUsageEvents = retainedEvents
                                    val prunedCount = result.events.size - retainedEvents.size
                                    collectStatus = if (retainedEvents.isEmpty()) {
                                        "No events found for the last 24h."
                                    } else {
                                        "Collected ${retainedEvents.size} usage event(s)."
                                    }
                                    retentionStatus = if (prunedCount > 0) {
                                        "Retention window: ${retentionSettings.retentionDays} days. Pruned $prunedCount usage event(s)."
                                    } else {
                                        "Retention window: ${retentionSettings.retentionDays} days."
                                    }
                                    refreshAlerts()
                                }
                                UsageCollectionResult.PermissionMissing -> {
                                    lastUsageEvents = emptyList()
                                    hasUsageAccess = false
                                    consentRepository.recordCheckResult(false, System.currentTimeMillis())
                                    consentState = consentRepository.readState()
                                    collectStatus = "Usage access is not currently granted."
                                }
                            }
                        },
                        networkStatus = networkStatus,
                        networkEvents = lastNetworkEvents,
                        onCollectNetworkEventsClick = {
                            val now = Instant.now()
                            val throttleDecision = collectionThrottle.tryCollect("collect_network", now)
                            if (!throttleDecision.allowed) {
                                networkStatus = "Network collection is throttled. Try again in ${throttleDecision.retryAfterSeconds} second(s)."
                                return@DashboardShell
                            }

                            when (val result = collectNetworkUsageUseCase.collectLast24hNetworkUsageEvents(
                                isVpnModeEnabled = vpnModeState.isEnabled,
                            )) {
                                is UsageCollectionResult.Success -> {
                                    if (vpnModeState.isEnabled) {
                                        vpnTrafficTotals = VpnModeTrafficStore.shared.snapshotTotals()
                                    }
                                    val retainedEvents = retentionPolicy.pruneEvents(
                                        events = result.events,
                                        retentionDays = retentionSettings.retentionDays,
                                        now = now,
                                    )
                                    lastNetworkEvents = retainedEvents
                                    val prunedCount = result.events.size - retainedEvents.size
                                    networkStatus = if (retainedEvents.isEmpty()) {
                                        "No network traffic was detected in the last 24h."
                                    } else {
                                        "Collected ${retainedEvents.size} network usage event(s)."
                                    }
                                    retentionStatus = if (prunedCount > 0) {
                                        "Retention window: ${retentionSettings.retentionDays} days. Pruned $prunedCount network event(s)."
                                    } else {
                                        "Retention window: ${retentionSettings.retentionDays} days."
                                    }
                                    refreshAlerts()
                                }

                                UsageCollectionResult.PermissionMissing -> {
                                    lastNetworkEvents = emptyList()
                                    hasUsageAccess = false
                                    consentRepository.recordCheckResult(false, System.currentTimeMillis())
                                    consentState = consentRepository.readState()
                                    networkStatus = "Usage access is not currently granted."
                                }
                            }
                        },
                        inventoryStatus = inventoryStatus,
                        inventoryChangeStatus = inventoryChangeStatus,
                        retentionSettings = retentionSettings,
                        retentionStatus = retentionStatus,
                        hasInventory = lastInventoryProfiles.isNotEmpty(),
                        usageEvents = lastUsageEvents,
                        privacyScore = privacyScore,
                        allowedRetentionDays = retentionPolicy.allowedRetentionDays(),
                        onCollectInventoryClick = {
                            val now = Instant.now()
                            val throttleDecision = collectionThrottle.tryCollect("collect_inventory", now)
                            if (!throttleDecision.allowed) {
                                inventoryStatus = "Inventory refresh is throttled. Try again in ${throttleDecision.retryAfterSeconds} second(s)."
                                return@DashboardShell
                            }
                            val refreshResult = refreshAppInventoryUseCase.refresh()
                            val profiles = refreshResult.currentProfiles
                            val inventoryChanges = refreshResult.inventoryChanges
                            val events = refreshResult.inventoryEvents
                            val launchableCount = profiles.count { it.isLaunchable }
                            lastInventoryProfiles = profiles
                            val retainedEvents = retentionPolicy.pruneEvents(
                                events = events,
                                retentionDays = retentionSettings.retentionDays,
                                now = now,
                            )
                            val prunedCount = events.size - retainedEvents.size
                            lastInventoryEvents = retainedEvents
                            inventoryStatus = if (profiles.isEmpty()) {
                                "No installed app profiles found."
                            } else {
                                "Loaded ${profiles.size} app profiles (${launchableCount} launchable)."
                            }
                            inventoryChangeStatus = if (inventoryChanges.isEmpty()) {
                                "No install or permission deltas since last refresh."
                            } else {
                                "Detected ${inventoryChanges.size} inventory change(s). "
                                    .plus(describeInventoryChanges(inventoryChanges))
                                    .plus(" | Generated ${retainedEvents.size} retained evidence event(s).")
                            }
                            retentionStatus = if (prunedCount > 0) {
                                "Retention window: ${retentionSettings.retentionDays} days. Pruned $prunedCount inventory event(s)."
                            } else {
                                "Retention window: ${retentionSettings.retentionDays} days."
                            }
                            refreshAlerts()
                        },
                        alerts = activeAlerts,
                        suppressedAlertCount = suppressedAlertCount,
                        onClearAlertHistoryClick = ::clearAlertHistory,
                        isVpnModeEnabled = vpnModeState.isEnabled,
                        onEnableVpnModeClick = ::requestVpnModeEnable,
                        onDisableVpnModeClick = ::disableVpnMode,
                        onOpenLatestProfileClick = {
                            selectedProfile = lastInventoryProfiles.firstOrNull()
                            selectedPermissionForEvidence = null
                        },
                        onOpenScoreReasonClick = { reason ->
                            selectedScoreReason = reason
                        },
                        onOpenAlertClick = { alert ->
                            selectedScoreReason = ScoreReason(
                                rule = alert.rule,
                                message = alert.message,
                                packageName = alert.packageName,
                                delta = alert.triggerDelta,
                                evidenceEventIds = alert.evidenceEventIds,
                            )
                        },
                        onOpenDailySummaryClick = {
                            selectedDailySummary = buildDailySummaryUseCase.build(
                                score = privacyScore,
                                events = scoringEvents,
                                alerts = activeAlerts,
                                now = Instant.now(),
                            )
                        },
                        onRetentionDaysClick = ::applyRetentionDays,
                        onPruneNowClick = ::pruneLoadedEvidenceNow,
                        onDeleteLoadedEventsClick = ::deleteLoadedEvidence,
                        onOpenTransparencySettingsClick = {
                            vpnTrafficTotals = VpnModeTrafficStore.shared.snapshotTotals()
                            isTransparencySettingsOpen = true
                        },
                        onOpenAlertProfileClick = { packageName ->
                            val matchingProfile = lastInventoryProfiles.firstOrNull { it.packageName == packageName }
                            if (matchingProfile != null) {
                                selectedProfile = matchingProfile
                                selectedPermissionForEvidence = null
                                selectedDailySummary = null
                            }
                        },
                        )
                } else if (isTransparencySettingsOpen) {
                    TransparencySettingsScreen(
                        hasUsageAccess = hasUsageAccess,
                        consentState = consentState,
                        lastCheckedLabel = formatTimestamp(consentState.lastCheckedAtEpochMs),
                        lastSettingsOpenedLabel = formatTimestamp(consentState.lastSettingsOpenedAtEpochMs),
                        isVpnModeEnabled = vpnModeState.isEnabled,
                        onEnableVpnModeClick = ::requestVpnModeEnable,
                        onDisableVpnModeClick = ::disableVpnMode,
                        vpnEnabledAtLabel = formatTimestamp(vpnModeState.lastEnabledAtEpochMs),
                        vpnDisabledAtLabel = formatTimestamp(vpnModeState.lastDisabledAtEpochMs),
                        retentionSettings = retentionSettings,
                        retentionStatus = retentionStatus,
                        allowedRetentionDays = retentionPolicy.allowedRetentionDays(),
                        loadedUsageEventCount = lastUsageEvents.size,
                        loadedInventoryEventCount = lastInventoryEvents.size,
                        loadedNetworkEventCount = lastNetworkEvents.size,
                        vpnPacketsObserved = vpnTrafficTotals.totalPackets,
                        vpnBytesObserved = vpnTrafficTotals.totalBytes,
                        vpnUniqueDestinationCount = vpnTrafficTotals.uniqueDestinationCount,
                        vpnParsedPacketsObserved = vpnTrafficTotals.parsedPackets,
                        vpnUnparsedPacketsObserved = vpnTrafficTotals.unparsedPackets,
                        vpnCaptureMode = vpnTrafficTotals.captureMode,
                        vpnForwardingEnabled = vpnTrafficTotals.forwardingEnabled,
                        onRefreshVpnDiagnosticsClick = { vpnTrafficTotals = VpnModeTrafficStore.shared.snapshotTotals() },
                        vpnForwardingRequested = vpnModeState.isForwardingEnabled,
                        vpnForwardingSupported = CyxWatchVpnService.isForwardingModeSupported(),
                        onToggleVpnForwardingModeClick = ::setVpnForwardingEnabled,
                        onRetentionDaysClick = ::applyRetentionDays,
                        onPruneNowClick = ::pruneLoadedEvidenceNow,
                        onDeleteLoadedEventsClick = ::deleteLoadedEvidence,
                        onBack = { isTransparencySettingsOpen = false },
                    )
                } else if (selectedScoreReason != null) {
                    val selectedReasonValue = selectedScoreReason!!
                    ScoreEvidenceScreen(
                        reason = selectedReasonValue,
                        evidenceEvents = evidenceEventsForScoreReason(
                            reason = selectedReasonValue,
                            events = scoringEvents,
                        ),
                        onBack = { selectedScoreReason = null },
                        appLabelsByPackageName = appLabelsByPackageName,
                    )
                } else if (selectedDailySummary != null) {
                    val selectedSummaryValue = selectedDailySummary!!
                    DailySummaryScreen(
                        summary = selectedSummaryValue,
                        scoringEvents = scoringEvents,
                        onBack = { selectedDailySummary = null },
                        onOpenReason = { reason ->
                            selectedScoreReason = reason
                        },
                        onOpenAlert = { alert ->
                            selectedScoreReason = ScoreReason(
                                rule = alert.rule,
                                message = alert.message,
                                packageName = alert.packageName,
                                delta = alert.triggerDelta,
                                evidenceEventIds = alert.evidenceEventIds,
                            )
                        },
                        onOpenAlertProfile = { packageName ->
                            val matchingProfile = lastInventoryProfiles.firstOrNull { it.packageName == packageName }
                            if (matchingProfile != null) {
                                selectedProfile = matchingProfile
                                selectedPermissionForEvidence = null
                                selectedDailySummary = null
                            }
                        },
                        onOpenTopApp = { packageName ->
                            val matchingProfile = lastInventoryProfiles.firstOrNull { it.packageName == packageName }
                            if (matchingProfile != null) {
                                selectedProfile = matchingProfile
                                selectedPermissionForEvidence = null
                                selectedDailySummary = null
                            }
                        },
                        appLabelsByPackageName = appLabelsByPackageName,
                    )
                } else {
                    val selectedProfileValue = selectedProfile!!
                    val selectedPermissionValue = selectedPermissionForEvidence
                    if (selectedPermissionValue == null) {
                        AppProfileScreen(
                            profile = selectedProfileValue,
                            onBack = {
                                selectedProfile = null
                                selectedPermissionForEvidence = null
                            },
                            onSensitivePermissionClick = { permission ->
                                selectedPermissionForEvidence = permission
                            },
                        )
                    } else {
                        InventoryEvidenceScreen(
                            profile = selectedProfileValue,
                            permission = selectedPermissionValue,
                            evidenceEvents = evidenceEventsForPermission(
                                profile = selectedProfileValue,
                                permission = selectedPermissionValue,
                                events = lastInventoryEvents,
                            ),
                            onBack = { selectedPermissionForEvidence = null },
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return ""
    return try {
        val instant = Instant.ofEpochMilli(timestamp)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        ""
    }
}

@Composable
private fun DashboardShell(
    modifier: Modifier = Modifier,
    collectionStatus: String,
    onCollectEventsClick: () -> Unit,
    networkStatus: String,
    networkEvents: List<PrivacyEvent>,
    onCollectNetworkEventsClick: () -> Unit,
    inventoryStatus: String,
    inventoryChangeStatus: String,
    retentionSettings: RetentionSettings,
    retentionStatus: String,
    hasInventory: Boolean,
    usageEvents: List<PrivacyEvent>,
    privacyScore: PrivacyScore,
    allowedRetentionDays: List<Int>,
    onCollectInventoryClick: () -> Unit,
    onOpenLatestProfileClick: () -> Unit,
    onOpenScoreReasonClick: (ScoreReason) -> Unit,
    onOpenAlertClick: (PrivacyAlert) -> Unit,
    onOpenDailySummaryClick: () -> Unit,
    onRetentionDaysClick: (Int) -> Unit,
    onPruneNowClick: () -> Unit,
    onDeleteLoadedEventsClick: () -> Unit,
    alerts: List<PrivacyAlert>,
    suppressedAlertCount: Int,
    onClearAlertHistoryClick: () -> Unit,
    onOpenTransparencySettingsClick: () -> Unit,
    isVpnModeEnabled: Boolean,
    onEnableVpnModeClick: () -> Unit,
    onDisableVpnModeClick: () -> Unit,
    onOpenAlertProfileClick: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Today summary")
        Text("Local-only mode. Usage access is enabled.")
        Button(
            onClick = onOpenDailySummaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Open daily summary screen" },
        ) {
            Text("Open daily summary")
        }
        Button(
            onClick = onOpenTransparencySettingsClick,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Open privacy settings screen" },
        ) {
            Text("Open privacy settings")
        }
        Text(collectionStatus, style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = onCollectEventsClick,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Collect usage timeline events for last 24 hours" },
        ) {
            Text("Collect last 24h events")
        }
        Text(networkStatus, style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = onCollectNetworkEventsClick,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Collect network usage events for last 24 hours" },
        ) {
            Text("Collect last 24h network usage")
        }
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = if (isVpnModeEnabled) {
                "Network visibility: VPN mode is active."
            } else {
                "Network visibility: basic mode."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            if (isVpnModeEnabled) {
                "VPN mode is local traffic visibility only; it is not a private VPN service."
            } else {
                "Basic mode uses app-level counters; advanced mode adds endpoint metadata without changing privacy guarantees."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        if (isVpnModeEnabled) {
            Button(
                onClick = onDisableVpnModeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Stop advanced network visibility" },
            ) {
                Text("Stop advanced network visibility")
            }
        } else {
            Button(
                onClick = onEnableVpnModeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Enable advanced network visibility" },
            ) {
                Text("Enable advanced network visibility")
            }
        }
        Text(inventoryStatus, style = MaterialTheme.typography.bodySmall)
        Text(inventoryChangeStatus, style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = onCollectInventoryClick,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Refresh installed app inventory" },
        ) {
            Text("Refresh app inventory")
        }
        if (hasInventory) {
            Button(
                onClick = onOpenLatestProfileClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Open latest loaded app profile" },
            ) {
                Text("Open latest app profile")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Retention", style = MaterialTheme.typography.titleMedium)
                Text(retentionStatus, style = MaterialTheme.typography.bodySmall)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allowedRetentionDays.forEach { days ->
                        Button(
                            onClick = { onRetentionDaysClick(days) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "$days day retention option" },
                        ) {
                            val selected = if (retentionSettings.retentionDays == days) "Current" else "Set"
                            Text("$selected: $days days")
                        }
                    }
                }
                Button(
                    onClick = onPruneNowClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Prune loaded evidence now" },
                ) {
                    Text("Prune loaded evidence")
                }
                Button(
                    onClick = onDeleteLoadedEventsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Delete all loaded evidence" },
                ) {
                    Text("Delete loaded evidence")
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Privacy score", style = MaterialTheme.typography.titleMedium)
                Text("${privacyScore.score}/100", style = MaterialTheme.typography.headlineSmall)
                if (privacyScore.reasons.isEmpty()) {
                    Text("No scoring reasons from loaded evidence yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Top reasons", style = MaterialTheme.typography.titleSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        privacyScore.reasons.take(3).forEach { reason ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(reason.message, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "-${reason.delta} | ${reason.evidenceEventIds.size} evidence event(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Button(
                                    onClick = { onOpenScoreReasonClick(reason) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics {
                                            contentDescription = "Open score evidence for ${reason.packageName}"
                                        },
                                ) {
                                    Text("Open evidence")
                                }
                            }
                        }
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Alerts", style = MaterialTheme.typography.titleMedium)
                if (alerts.isEmpty()) {
                    Text("No alerts yet for the current evidence window.", style = MaterialTheme.typography.bodySmall)
                } else {
                    alerts
                        .take(5)
                        .forEach { alert ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(alert.message, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "Triggered: ${formatAlertTimestamp(alert.triggeredAt)} | ${alert.triggerDelta} points",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                OutlinedButton(
                                    onClick = { onOpenAlertClick(alert) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics {
                                            contentDescription = "Open alert evidence for ${alert.packageName}"
                                        },
                                ) {
                                    Text("Open supporting evidence")
                                }
                                if (alert.rule.isSensitivePermissionWarning()) {
                                    OutlinedButton(
                                        onClick = { onOpenAlertProfileClick(alert.packageName) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .semantics {
                                                contentDescription = "Open app profile from alert ${alert.packageName}"
                                            },
                                    ) {
                                        Text("Open app profile")
                                    }
                                }
                            }
                        }
                    if (suppressedAlertCount > 0) {
                        Text(
                            "$suppressedAlertCount alert(s) were suppressed to enforce cooldown.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Button(
                    onClick = onClearAlertHistoryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Clear alert history" },
                ) {
                    Text("Clear alert history")
                }
            }
        }
        Text("Recent usage timeline", style = MaterialTheme.typography.titleMedium)
        if (usageEvents.isEmpty()) {
            Text("No timeline events collected yet.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(usageEvents) { event ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "${formatUsageEventTimestamp(event.timestamp)} | ${event.source} | ${event.packageName}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(event.explanation, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Text("Recent network usage", style = MaterialTheme.typography.titleMedium)
        if (networkEvents.isEmpty()) {
            Text("No network usage collected yet.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(networkEvents) { event ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "${formatUsageEventTimestamp(event.timestamp)} | ${event.packageName}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(event.explanation, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
    }
}

private fun formatUsageEventTimestamp(timestamp: Instant): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        formatter.format(timestamp)
    } catch (_: Exception) {
        ""
    }
}

private fun formatAlertTimestamp(timestamp: Instant): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(timestamp)
    } catch (_: Exception) {
        ""
    }
}

private fun describeInventoryChanges(changes: List<AppInventoryChange>): String {
    return if (changes.isEmpty()) {
        "No changes."
    } else {
        changes.joinToString("; ") { change ->
            when (change) {
                is AppInventoryChange.NewInstall -> "${change.packageName} installed"
                is AppInventoryChange.PermissionDelta -> {
                    val details = mutableListOf<String>()
                    if (change.addedPermissions.isNotEmpty()) {
                        details.add("added ${change.addedPermissions.size} permission(s)")
                    }
                    if (change.removedPermissions.isNotEmpty()) {
                        details.add("removed ${change.removedPermissions.size} permission(s)")
                    }
                    "${change.packageName} (${details.joinToString(", ")})"
                }
            }
        }
    }
}

private fun evidenceEventsForPermission(
    profile: AppProfile,
    permission: String,
    events: List<PrivacyEvent>,
): List<PrivacyEvent> {
    return events.filter { event ->
        event.packageName == profile.packageName &&
            event.source == "AppInventory" &&
            event.evidenceJson.contains("\"$permission\"")
    }
}

private fun evidenceEventsForScoreReason(
    reason: ScoreReason,
    events: List<PrivacyEvent>,
): List<PrivacyEvent> {
    val evidenceIds = reason.evidenceEventIds.toSet()
    return events
        .filter { event -> event.eventId in evidenceIds }
        .sortedBy { it.timestamp }
}

private data class PruneLoadedEventsResult(
    val usageEvents: List<PrivacyEvent>,
    val inventoryEvents: List<PrivacyEvent>,
    val networkEvents: List<PrivacyEvent>,
    val prunedCount: Int,
)

private fun pruneLoadedEvents(
    usageEvents: List<PrivacyEvent>,
    inventoryEvents: List<PrivacyEvent>,
    networkEvents: List<PrivacyEvent>,
    retentionSettings: RetentionSettings,
    retentionPolicy: RetentionPolicy,
): PruneLoadedEventsResult {
    val now = Instant.now()
    val retainedUsageEvents = retentionPolicy.pruneEvents(
        events = usageEvents,
        retentionDays = retentionSettings.retentionDays,
        now = now,
    )
    val retainedInventoryEvents = retentionPolicy.pruneEvents(
        events = inventoryEvents,
        retentionDays = retentionSettings.retentionDays,
        now = now,
    )
    val retainedNetworkEvents = retentionPolicy.pruneEvents(
        events = networkEvents,
        retentionDays = retentionSettings.retentionDays,
        now = now,
    )
    return PruneLoadedEventsResult(
        usageEvents = retainedUsageEvents,
        inventoryEvents = retainedInventoryEvents,
        networkEvents = retainedNetworkEvents,
        prunedCount = (usageEvents.size - retainedUsageEvents.size) +
            (inventoryEvents.size - retainedInventoryEvents.size) +
            (networkEvents.size - retainedNetworkEvents.size),
    )
}
