package com.cyxwatch.app

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
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
import com.cyxwatch.app.platform.network.VpnTrafficDestination
import com.cyxwatch.app.platform.network.VpnTrafficTotals
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyxWatchApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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
    var isCollectingUsage by remember { mutableStateOf(false) }
    var isCollectingNetwork by remember { mutableStateOf(false) }
    var isCollectingInventory by remember { mutableStateOf(false) }
    var activeAlerts by remember { mutableStateOf<List<PrivacyAlert>>(emptyList()) }
    var suppressedAlertCount by remember { mutableStateOf(0) }
    var vpnTrafficTotals by remember { mutableStateOf(VpnModeTrafficStore.shared.snapshotTotals()) }
    var liveVpnTrafficTotals by remember { mutableStateOf(vpnTrafficTotals) }
    var liveVpnDestinations by remember { mutableStateOf<List<VpnTrafficDestination>>(emptyList()) }
    var liveVpnThroughputSamples by remember { mutableStateOf<List<Long>>(emptyList()) }
    var lastObservedVpnBytes by remember { mutableLongStateOf(0L) }
    var dashboardLastUpdated by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastUsageAccessWarningAt by remember { mutableLongStateOf(0L) }
    val scoringEvents = lastUsageEvents + lastNetworkEvents + lastInventoryEvents
    val privacyScore = privacyScoreCalculator.calculate(scoringEvents)
    val appLabelsByPackageName = lastInventoryProfiles
        .associate { it.packageName to it.label }

    fun maybeNotifyUsageAccessMissing() {
        val now = System.currentTimeMillis()
        if (now - lastUsageAccessWarningAt < 180_000L) {
            return
        }
        permissionWarningNotifier.postUsageAccessWarning()
        lastUsageAccessWarningAt = now
    }

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

    fun refreshVpnLiveTelemetry() {
        if (vpnModeState.isEnabled) {
            val totals = VpnModeTrafficStore.shared.snapshotTotals()
            val destinationSnapshot = VpnModeTrafficStore.shared.snapshotTopDestinations()
            val currentBytes = totals.totalBytes
            val bytesDelta = when {
                lastObservedVpnBytes == 0L -> 0L
                currentBytes >= lastObservedVpnBytes -> currentBytes - lastObservedVpnBytes
                else -> currentBytes
            }
            liveVpnTrafficTotals = totals
            liveVpnDestinations = destinationSnapshot
            liveVpnThroughputSamples = (liveVpnThroughputSamples + bytesDelta).takeLast(30)
            lastObservedVpnBytes = currentBytes
        } else {
            liveVpnTrafficTotals = VpnModeTrafficStore.shared.snapshotTotals()
            liveVpnDestinations = VpnModeTrafficStore.shared.snapshotTopDestinations()
            liveVpnThroughputSamples = emptyList()
            lastObservedVpnBytes = 0L
        }
        dashboardLastUpdated = System.currentTimeMillis()
    }

    fun syncVpnStateTotals() {
        vpnTrafficTotals = VpnModeTrafficStore.shared.snapshotTotals()
        refreshVpnLiveTelemetry()
    }

    fun enableVpnMode() {
        val now = System.currentTimeMillis()
        try {
            CyxWatchVpnService.start(context)
            vpnModeState = vpnModeSettingsRepository.setEnabled(isEnabled = true, changedAtEpochMs = now)
            syncVpnStateTotals()
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
        syncVpnStateTotals()
    }

    fun setVpnForwardingEnabled(enabled: Boolean) {
        val wasVpnModeActive = vpnModeState.isEnabled
        vpnModeState = vpnModeSettingsRepository.setForwardingEnabled(enabled)
        if (wasVpnModeActive) {
            CyxWatchVpnService.start(context)
        }
        syncVpnStateTotals()
    }

    LaunchedEffect(vpnModeState.isEnabled) {
        refreshVpnLiveTelemetry()
        while (isActive && vpnModeState.isEnabled) {
            delay(800)
            refreshVpnLiveTelemetry()
        }
    }

    val appColorScheme = darkColorScheme(
        primary = Color(0xFF4E8BFF),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF18345F),
        onPrimaryContainer = Color(0xFFDDE8FF),
        secondary = Color(0xFF61B4FF),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF85DEC1),
        onTertiary = Color(0xFF04281D),
        background = Color(0xFF070B16),
        onBackground = Color(0xFFF2F5FF),
        surface = Color(0xFF0E1528),
        onSurface = Color(0xFFE7ECFF),
        surfaceVariant = Color(0xFF232F4A),
        onSurfaceVariant = Color(0xFFB5BED8),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFFFFFFFF),
        outline = Color(0xFF7F8FB2),
    )

    MaterialTheme(colorScheme = appColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    modifier = Modifier.size(28.dp),
                                    painter = painterResource(R.drawable.ic_cyxwatch_logo),
                                    contentDescription = "CyxWatch logo",
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Column {
                                    Text("CyxWatch", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = if (vpnModeState.isEnabled) "Advanced visibility active" else "Observability ready",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        },
                    )
                },
            ) { contentPadding ->
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
                            if (isCollectingUsage) return@DashboardShell
                            val now = Instant.now()
                            val throttleDecision = collectionThrottle.tryCollect("collect_usage", now)
                            if (!throttleDecision.allowed) {
                                collectStatus = "Usage collection is throttled. Try again in ${throttleDecision.retryAfterSeconds} second(s)."
                                return@DashboardShell
                            }
                            isCollectingUsage = true
                            collectStatus = "Collecting usage timeline..."
                            coroutineScope.launch {
                                try {
                                    when (val result = withContext(Dispatchers.IO) {
                                        usageAccessCollectorUseCase.collectLast24hUsageEvents()
                                    }) {
                                        is UsageCollectionResult.Success -> {
                                            val retainedEvents = withContext(Dispatchers.Default) {
                                                retentionPolicy.pruneEvents(
                                                    events = result.events,
                                                    retentionDays = retentionSettings.retentionDays,
                                                    now = Instant.now(),
                                                )
                                            }
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
                                            maybeNotifyUsageAccessMissing()
                                            collectStatus = "Usage access is not currently granted."
                                        }
                                    }
                                } catch (exception: Exception) {
                                    collectStatus = "Usage collection failed: ${exception.localizedMessage ?: "Unknown error"}"
                                } finally {
                                    isCollectingUsage = false
                                }
                            }
                        },
                        networkStatus = networkStatus,
                        networkEvents = lastNetworkEvents,
                        onCollectNetworkEventsClick = {
                            if (isCollectingNetwork) return@DashboardShell
                            val now = Instant.now()
                            val throttleDecision = collectionThrottle.tryCollect("collect_network", now)
                            if (!throttleDecision.allowed) {
                                networkStatus = "Network collection is throttled. Try again in ${throttleDecision.retryAfterSeconds} second(s)."
                                return@DashboardShell
                            }
                            isCollectingNetwork = true
                            networkStatus = "Collecting network usage..."
                            coroutineScope.launch {
                                try {
                                    when (val result = withContext(Dispatchers.IO) {
                                        collectNetworkUsageUseCase.collectLast24hNetworkUsageEvents(
                                            isVpnModeEnabled = vpnModeState.isEnabled,
                                        )
                                    }) {
                                        is UsageCollectionResult.Success -> {
                                            if (vpnModeState.isEnabled) {
                                                syncVpnStateTotals()
                                            }
                                            val retainedEvents = withContext(Dispatchers.Default) {
                                                retentionPolicy.pruneEvents(
                                                    events = result.events,
                                                    retentionDays = retentionSettings.retentionDays,
                                                    now = Instant.now(),
                                                )
                                            }
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
                                            maybeNotifyUsageAccessMissing()
                                            networkStatus = "Usage access is not currently granted."
                                        }
                                    }
                                } catch (exception: Exception) {
                                    networkStatus = "Network collection failed: ${exception.localizedMessage ?: "Unknown error"}"
                                } finally {
                                    isCollectingNetwork = false
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
                        isCollectingInventory = isCollectingInventory,
                        onCollectInventoryClick = {
                            val now = Instant.now()
                            val throttleDecision = collectionThrottle.tryCollect("collect_inventory", now)
                            if (!throttleDecision.allowed) {
                                inventoryStatus = "Inventory refresh is throttled. Try again in ${throttleDecision.retryAfterSeconds} second(s)."
                                return@DashboardShell
                            }
                            isCollectingInventory = true
                            coroutineScope.launch {
                                try {
                                    val refreshResult = withContext(Dispatchers.IO) {
                                        refreshAppInventoryUseCase.refresh()
                                    }
                                    val profiles = refreshResult.currentProfiles
                                    val inventoryChanges = refreshResult.inventoryChanges
                                    val events = refreshResult.inventoryEvents
                                    val launchableCount = profiles.count { it.isLaunchable }
                                    val retainedEvents = withContext(Dispatchers.Default) {
                                        retentionPolicy.pruneEvents(
                                            events = events,
                                            retentionDays = retentionSettings.retentionDays,
                                            now = now,
                                        )
                                    }
                                    val prunedCount = events.size - retainedEvents.size
                                    lastInventoryProfiles = profiles
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
                                } catch (exception: Exception) {
                                    inventoryStatus = "Inventory refresh failed: ${exception.localizedMessage ?: "Unknown error"}"
                                    inventoryChangeStatus = "Try again from dashboard."
                                } finally {
                                    isCollectingInventory = false
                                }
                            }
                        },
                        alerts = activeAlerts,
                        suppressedAlertCount = suppressedAlertCount,
                        onClearAlertHistoryClick = ::clearAlertHistory,
                        isVpnModeEnabled = vpnModeState.isEnabled,
                        onEnableVpnModeClick = ::requestVpnModeEnable,
                        onDisableVpnModeClick = ::disableVpnMode,
                        liveVpnThroughputSamples = liveVpnThroughputSamples,
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
                        isCollectingUsage = isCollectingUsage,
                        isCollectingNetwork = isCollectingNetwork,
                        onRetentionDaysClick = ::applyRetentionDays,
                        onPruneNowClick = ::pruneLoadedEvidenceNow,
                        onDeleteLoadedEventsClick = ::deleteLoadedEvidence,
                        liveVpnTrafficTotals = liveVpnTrafficTotals,
                        liveVpnTopDestinations = liveVpnDestinations,
                        liveMetricsLastUpdatedLabel = formatTimestamp(dashboardLastUpdated),
                        onOpenTransparencySettingsClick = {
                            syncVpnStateTotals()
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
                        onRefreshVpnDiagnosticsClick = { syncVpnStateTotals() },
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
    isCollectingUsage: Boolean,
    isCollectingNetwork: Boolean,
    isCollectingInventory: Boolean,
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
    liveVpnTrafficTotals: VpnTrafficTotals,
    liveVpnTopDestinations: List<VpnTrafficDestination>,
    liveMetricsLastUpdatedLabel: String,
    onEnableVpnModeClick: () -> Unit,
    onDisableVpnModeClick: () -> Unit,
    onOpenAlertProfileClick: (String) -> Unit,
    liveVpnThroughputSamples: List<Long>,
) {
    val dashboardScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val canScrollToBottom by remember { derivedStateOf { dashboardScrollState.value < dashboardScrollState.maxValue } }
    val canScrollToTop by remember { derivedStateOf { dashboardScrollState.value > 0 } }
    val scoreBadge = scoreBadgeForPrivacyScore(privacyScore.score)
    val topSignals = privacyScore.reasons.size
    val topSignalsText = if (topSignals > 0) {
        privacyScore.reasons.take(3).joinToString("  |  ") { it.packageName }
    } else {
        "No active signal sources"
    }
    val riskSummary = if (topSignals > 0) {
        "${privacyScore.score}/100 score"
    } else {
        "No signal yet"
    }
    val averageThroughputBytes = if (liveVpnThroughputSamples.isNotEmpty()) {
        liveVpnThroughputSamples.sum() / liveVpnThroughputSamples.size
    } else {
        0L
    }
    val peakThroughputBytes = liveVpnThroughputSamples.maxOrNull() ?: 0L

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
            .fillMaxSize()
            .verticalScroll(dashboardScrollState)
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                    shape = MaterialTheme.shapes.small,
                                ),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        ) {
                            Image(
                                modifier = Modifier.padding(6.dp),
                                painter = painterResource(R.drawable.ic_cyxwatch_logo),
                                contentDescription = "CyxWatch logo",
                            )
                        }
                        Column {
                            Text("CyxWatch Monitor", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Signal-first local observability.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (scoreBadge.label) {
                            "HEALTHY" -> Color(0xFF153822)
                            "WATCH" -> Color(0xFF352600)
                            else -> Color(0xFF3F1616)
                        },
                    ) {
                        Text(
                            scoreBadge.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = scoreBadge.textColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Text(riskSummary, style = MaterialTheme.typography.titleMedium)
                Text(topSignalsText, style = MaterialTheme.typography.bodySmall)
                Text(
                    if (isVpnModeEnabled) {
                        "Advanced visibility is active; endpoint metadata only. No payload capture."
                    } else {
                        "Basic visibility mode active with local collection and daily aggregated totals."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MonitorMetricTile(
                        modifier = Modifier.weight(1f),
                        label = "Privacy score",
                        value = "${privacyScore.score}",
                        hint = "out of 100",
                        valueColor = scoreBadge.textColor,
                    )
                    MonitorMetricTile(
                        modifier = Modifier.weight(1f),
                        label = "Risk reasons",
                        value = privacyScore.reasons.size.toString(),
                        hint = "active signals",
                        valueColor = MaterialTheme.colorScheme.onSurface,
                    )
                    MonitorMetricTile(
                        modifier = Modifier.weight(1f),
                        label = "Active alerts",
                        value = alerts.size.toString(),
                        hint = "open now",
                        valueColor = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onOpenDailySummaryClick,
                        modifier = Modifier.weight(1f).semantics { contentDescription = "Open daily summary screen" },
                    ) {
                        Text("Daily summary")
                    }
                    FilledTonalButton(
                        onClick = onOpenTransparencySettingsClick,
                        modifier = Modifier.weight(1f).semantics { contentDescription = "Open privacy settings screen" },
                    ) {
                        Text("Privacy settings")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Live observability", style = MaterialTheme.typography.titleMedium)
                    Surface(
                        color = if (isVpnModeEnabled) {
                            Color(0xFF184A22)
                        } else {
                            Color(0xFF213A5E)
                        },
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            if (isVpnModeEnabled) "ADVANCED" else "BASIC",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isVpnModeEnabled) {
                                Color(0xFF8CFFAF)
                            } else {
                                Color(0xFF95A9DD)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                if (isVpnModeEnabled) {
                    Text("Last updated: $liveMetricsLastUpdatedLabel", style = MaterialTheme.typography.bodySmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MonitorMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Packets",
                            value = liveVpnTrafficTotals.totalPackets.toString(),
                            hint = "total",
                            valueColor = MaterialTheme.colorScheme.primary,
                        )
                        MonitorMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Bytes",
                            value = formatByteCount(liveVpnTrafficTotals.totalBytes),
                            hint = "flowed total",
                            valueColor = MaterialTheme.colorScheme.secondary,
                        )
                        MonitorMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Destinations",
                            value = liveVpnTrafficTotals.uniqueDestinationCount.toString(),
                            hint = "observed",
                            valueColor = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MonitorMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Parsed",
                            value = liveVpnTrafficTotals.parsedPackets.toString(),
                            hint = "understood",
                            valueColor = Color(0xFF78D5FF),
                        )
                        MonitorMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Unparsed",
                            value = liveVpnTrafficTotals.unparsedPackets.toString(),
                            hint = "opaque",
                            valueColor = Color(0xFFFFA857),
                        )
                    }
                    if (liveVpnThroughputSamples.isNotEmpty()) {
                        Text(
                            "Throughput stream (last 30 samples): avg ${formatByteCount(averageThroughputBytes)}/s | peak ${formatByteCount(peakThroughputBytes)}/s",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        ThroughputSparkline(
                            values = liveVpnThroughputSamples,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                        )
                    } else {
                        Text(
                            "No live samples yet; keep network activity running to populate stream.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (liveVpnTopDestinations.isNotEmpty()) {
                        Text("Top destination endpoints", style = MaterialTheme.typography.titleSmall)
                        DestinationUsageBars(destinations = liveVpnTopDestinations)
                    }
                    FilledTonalButton(
                        onClick = onDisableVpnModeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Stop advanced network visibility" },
                    ) {
                        Text("Pause advanced visibility")
                    }
                } else {
                    Text(
                        "Enable advanced visibility to see endpoint metadata and live flow counters.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = onEnableVpnModeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Enable advanced network visibility" },
                    ) {
                        Text("Enable advanced visibility")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Collect windows", style = MaterialTheme.typography.titleMedium)
                Text(collectionStatus, style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(
                    onClick = onCollectEventsClick,
                    enabled = !isCollectingUsage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Collect usage timeline events for last 24 hours" },
                ) {
                    Text(if (isCollectingUsage) "Collecting last 24h usage events..." else "Collect last 24h usage events")
                }
                Text(networkStatus, style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(
                    onClick = onCollectNetworkEventsClick,
                    enabled = !isCollectingNetwork,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Collect network usage events for last 24 hours" },
                ) {
                    Text(if (isCollectingNetwork) "Collecting last 24h network usage..." else "Collect last 24h network usage")
                }
                Text(inventoryStatus, style = MaterialTheme.typography.bodySmall)
                Text(inventoryChangeStatus, style = MaterialTheme.typography.bodySmall)
                Button(
                    enabled = !isCollectingInventory,
                    onClick = onCollectInventoryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Refresh installed app inventory" },
                ) {
                    Text(if (isCollectingInventory) "Refreshing app inventory..." else "Refresh app inventory")
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
    if (dashboardScrollState.maxValue > 0) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                if (canScrollToTop) {
                    OutlinedButton(
                        enabled = canScrollToTop,
                        onClick = {
                            coroutineScope.launch {
                                dashboardScrollState.animateScrollTo(0)
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = "Scroll to top of dashboard" },
                    ) {
                        Text("Top")
                    }
                }
                if (canScrollToBottom) {
                    Button(
                        enabled = canScrollToBottom,
                        onClick = {
                            coroutineScope.launch {
                                dashboardScrollState.animateScrollTo(dashboardScrollState.maxValue)
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = "Scroll to bottom of dashboard" },
                    ) {
                        Text("Latest")
                    }
                }
            }
        }
    }
}

private data class ScoreBadgeVisual(
    val label: String,
    val textColor: Color,
)

private fun scoreBadgeForPrivacyScore(score: Int): ScoreBadgeVisual {
    return when {
        score >= 85 -> ScoreBadgeVisual(
            label = "HEALTHY",
            textColor = Color(0xFF4ED97A),
        )
        score >= 65 -> ScoreBadgeVisual(
            label = "WATCH",
            textColor = Color(0xFFFFD166),
        )
        else -> ScoreBadgeVisual(
            label = "RISK",
            textColor = Color(0xFFFF6B6B),
        )
    }
}

@Composable
private fun ThroughputSparkline(
    values: List<Long>,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) {
        Text("No live throughput samples yet.", style = MaterialTheme.typography.bodySmall)
        return
    }
    val plottedValues = values.takeLast(30)
    val maxValue = (plottedValues.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
    val topPadding = 8f
    val bottomPadding = 8f
    val sparklineStartColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.32f)
    val sparklineEndColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
    val sparklineLineColor = MaterialTheme.colorScheme.secondary
    val sparklinePointColor = MaterialTheme.colorScheme.onSecondary
    Canvas(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)),
    ) {
        val sampleCount = plottedValues.size
        if (sampleCount == 0) return@Canvas
        val graphHeight = size.height - topPadding - bottomPadding
        val graphWidth = size.width
        val plotStepX = if (sampleCount <= 1) 0f else graphWidth / (sampleCount - 1).toFloat()

        val points = plottedValues.mapIndexed { index, value ->
            val x = if (sampleCount <= 1) graphWidth / 2f else index * plotStepX
            val normalizedValue = value.toFloat() / maxValue
            val y = topPadding + graphHeight - (normalizedValue * graphHeight)
            Offset(x, y)
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, size.height - bottomPadding)
            points.forEach { point ->
                lineTo(point.x, point.y)
            }
            lineTo(points.last().x, size.height - bottomPadding)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    sparklineStartColor,
                    sparklineEndColor,
                ),
            ),
        )
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point ->
                lineTo(point.x, point.y)
            }
        }
        drawPath(
            path = linePath,
            color = sparklineLineColor,
            style = Stroke(width = 2.4f, cap = StrokeCap.Round),
        )

        points.forEach { point ->
            drawCircle(
                color = sparklinePointColor,
                radius = 2.5f,
                center = point,
            )
        }
    }
}

@Composable
private fun DestinationUsageBars(
    destinations: List<VpnTrafficDestination>,
) {
    if (destinations.isEmpty()) {
        Text(
            "Destination metadata not yet available.",
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }

    val topRows = destinations
        .sortedByDescending { it.bytes }
        .take(4)
    val maxBytes = (topRows.maxOfOrNull { it.bytes } ?: 0L).coerceAtLeast(1L).toFloat()
    val barTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val barFillColor = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        topRows.forEach { destination ->
            val destinationName = "${destination.destinationIp}:${destination.destinationPort ?: "*"} (${destination.protocol})"
            val ratio = destination.bytes.toFloat() / maxBytes
            val percent = (ratio * 100).toInt()
            Text(
                "$destinationName  -  ${formatByteCount(destination.bytes)}  ($percent%)",
                style = MaterialTheme.typography.bodySmall,
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            ) {
                val trackHeight = size.height
                val barHeight = 6f
                val trackWidth = size.width
                drawRect(
                    color = barTrackColor,
                    topLeft = Offset(0f, (trackHeight - barHeight) / 2f),
                    size = Size(trackWidth, barHeight),
                )
                drawRect(
                    color = barFillColor,
                    topLeft = Offset(0f, (trackHeight - barHeight) / 2f),
                    size = Size(trackWidth * ratio, barHeight),
                )
            }
            Text(
                "${destination.packetCount} packets observed",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MonitorMetricTile(
    label: String,
    value: String,
    hint: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
                maxLines = 1,
            )
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(hint, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatByteCount(byteCount: Long): String {
    if (byteCount < 1024) {
        return "$byteCount B"
    }
    val kib = byteCount / 1024.0
    if (kib < 1024) {
        return "${"%.2f".format(kib)} KB"
    }
    val mib = kib / 1024.0
    if (mib < 1024) {
        return "${"%.2f".format(mib)} MB"
    }
    val gib = mib / 1024.0
    return "${"%.2f".format(gib)} GB"
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
