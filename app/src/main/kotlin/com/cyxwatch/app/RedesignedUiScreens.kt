package com.cyxwatch.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyxwatch.app.domain.PrivacyAlert
import com.cyxwatch.app.domain.PrivacyScore
import com.cyxwatch.app.domain.ScoreReason
import com.cyxwatch.app.domain.RetentionSettings
import com.cyxwatch.app.domain.ScoringRule
import com.cyxwatch.app.domain.model.AppProfile
import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.PrivacyEvent
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

private object UiTokens {
    val BgPrimary = Color(0xFF0D1117)
    val BgSecondary = Color(0xFF161B22)
    val BgTertiary = Color(0xFF1C2333)
    val BgDanger = Color(0xFF1A0A0A)
    val AccentBlue = Color(0xFF2563EB)
    val AccentBlueLight = Color(0xFF3B82F6)
    val AccentCyan = Color(0xFF38BDF8)
    val StatusSafe = Color(0xFF22C55E)
    val StatusLow = Color(0xFF3B82F6)
    val StatusMedium = Color(0xFFF59E0B)
    val StatusHigh = Color(0xFFEF4444)
    val TextPrimary = Color(0xFFF0F6FF)
    val TextSecondary = Color(0xFF8B949E)
    val TextMuted = Color(0xFF4B5563)
    val Border = Color(0xFF21262D)
    val NavBg = Color(0xFF0D1117)
}

private const val CHEVRON_RIGHT = "\u203A"
private const val CHEVRON_DOWN = "\u25BE"

enum class MainTab {
    Dashboard,
    Alerts,
    Monitor,
    Reports,
    Settings,
}

enum class AlertSeverityFilter(val label: String) {
    All("All"),
    High("High"),
    Medium("Medium"),
    Low("Low"),
}

enum class MonitorStatusFilter(val label: String) {
    All("All"),
    Running("Running"),
    Background("Background"),
}

enum class ReportsPeriod(val label: String) {
    Daily("Daily"),
    Weekly("Weekly"),
    Monthly("Monthly"),
}

private enum class ThreatSeverity(
    val label: String,
    val dot: Color,
    val bg: Color,
    val border: Color,
    val iconBg: Color,
    val icon: String,
) {
    High("High", UiTokens.StatusHigh, Color(0xFF3D0A0A), UiTokens.StatusHigh, Color(0xFF5A1A1A), "\u26A0"),
    Medium("Medium", UiTokens.StatusMedium, Color(0xFF3D2208), UiTokens.StatusMedium, Color(0xFF512E0A), "\u21BB"),
    Low("Low", UiTokens.StatusLow, Color(0xFF0A2438), UiTokens.StatusLow, Color(0xFF163B57), "\uD83D\uDCC4"),
    Safe("Safe", UiTokens.StatusSafe, Color(0xFF0A2818), UiTokens.StatusSafe, Color(0xFF10462A), "\u2714"),
}

private data class ThreatRenderData(
    val title: String,
    val subtitle: String,
    val severity: ThreatSeverity,
    val icon: String,
)

data class SuspiciousAppSummary(
    val packageName: String,
    val appName: String,
    val reportCount: Int,
    val latestTimestampLabel: String,
    val topReason: String,
    val source: String = "Local observations",
)

private fun alertSeverity(alert: PrivacyAlert): ThreatSeverity = when {
    alert.rule.isCriticalWarning() || alert.rule == ScoringRule.SensitivePermissionAdded -> ThreatSeverity.High
    alert.rule == ScoringRule.MediumBackgroundNetwork || alert.rule == ScoringRule.ScreenOffAppActivity -> ThreatSeverity.Medium
    else -> ThreatSeverity.Low
}

private fun alertDetails(alert: PrivacyAlert): ThreatRenderData = when (alert.rule) {
    ScoringRule.HighBackgroundNetwork -> ThreatRenderData(
        title = "Background data access",
        subtitle = "Suspicious network activity detected",
        severity = ThreatSeverity.High,
        icon = "\u26A0",
    )
    ScoringRule.MediumBackgroundNetwork -> ThreatRenderData(
        title = "Hidden foreground service",
        subtitle = "App is running a hidden service",
        severity = ThreatSeverity.Medium,
        icon = "\u21BB",
    )
    ScoringRule.LowBackgroundNetwork -> ThreatRenderData(
        title = "Background data access",
        subtitle = "Background network activity detected",
        severity = ThreatSeverity.Low,
        icon = "\uD83D\uDCC4",
    )
    ScoringRule.NewAppWithSensitivePermissions -> ThreatRenderData(
        title = "Excessive permissions",
        subtitle = "App has sensitive permission changes",
        severity = ThreatSeverity.High,
        icon = "\u26A0",
    )
    ScoringRule.SensitivePermissionAdded -> ThreatRenderData(
        title = "Excessive permissions",
        subtitle = "App requested sensitive permissions",
        severity = ThreatSeverity.High,
        icon = "\u26A0",
    )
    ScoringRule.ScreenOffAppActivity -> ThreatRenderData(
        title = "Screen off activity",
        subtitle = "App was active while screen was off",
        severity = ThreatSeverity.Medium,
        icon = "\uD83D\uDCA4",
    )
    ScoringRule.KeyguardAppActivity -> ThreatRenderData(
        title = "Auto-start detected",
        subtitle = "App added behavior while locked",
        severity = ThreatSeverity.Low,
        icon = "\uD83D\uDCD0",
    )
}

private fun threatDetailsForRule(rule: ScoringRule): ThreatRenderData = when (rule) {
    ScoringRule.HighBackgroundNetwork -> ThreatRenderData(
        title = "Background data access",
        subtitle = "Suspicious network activity detected",
        severity = ThreatSeverity.High,
        icon = "\u26A0",
    )
    ScoringRule.MediumBackgroundNetwork -> ThreatRenderData(
        title = "Hidden foreground service",
        subtitle = "App is running a hidden service",
        severity = ThreatSeverity.Medium,
        icon = "\u21BB",
    )
    ScoringRule.LowBackgroundNetwork -> ThreatRenderData(
        title = "Background data access",
        subtitle = "Background network activity detected",
        severity = ThreatSeverity.Low,
        icon = "\uD83D\uDCC4",
    )
    ScoringRule.NewAppWithSensitivePermissions -> ThreatRenderData(
        title = "Excessive permissions",
        subtitle = "App has sensitive permission changes",
        severity = ThreatSeverity.High,
        icon = "\u26A0",
    )
    ScoringRule.SensitivePermissionAdded -> ThreatRenderData(
        title = "Excessive permissions",
        subtitle = "App requested sensitive permissions",
        severity = ThreatSeverity.High,
        icon = "\u26A0",
    )
    ScoringRule.ScreenOffAppActivity -> ThreatRenderData(
        title = "Screen off activity",
        subtitle = "App was active while screen was off",
        severity = ThreatSeverity.Medium,
        icon = "\uD83D\uDCA4",
    )
    ScoringRule.KeyguardAppActivity -> ThreatRenderData(
        title = "Auto-start detected",
        subtitle = "App added behavior while locked",
        severity = ThreatSeverity.Low,
        icon = "\uD83D\uDCD0",
    )
}

private fun reasonCategory(rule: ScoringRule): String = when (rule) {
    ScoringRule.HighBackgroundNetwork,
    ScoringRule.MediumBackgroundNetwork,
    ScoringRule.LowBackgroundNetwork -> "Network Risk"
    ScoringRule.NewAppWithSensitivePermissions,
    ScoringRule.SensitivePermissionAdded -> "Permission Risk"
    ScoringRule.ScreenOffAppActivity,
    ScoringRule.KeyguardAppActivity -> "Behavior Risk"
}

private fun recommendationForRule(rule: ScoringRule): String = when (rule) {
    ScoringRule.HighBackgroundNetwork,
    ScoringRule.MediumBackgroundNetwork,
    ScoringRule.LowBackgroundNetwork -> "Restrict background data and review app permissions."
    ScoringRule.NewAppWithSensitivePermissions,
    ScoringRule.SensitivePermissionAdded -> "Review and revoke sensitive permissions."
    ScoringRule.ScreenOffAppActivity,
    ScoringRule.KeyguardAppActivity -> "Disable background auto-start and screen overlays."
}

private fun appLabelForPackage(packageName: String): String =
    packageName.substringAfterLast(".").ifBlank { packageName }

private fun appLabel(profile: AppProfile): String = profile.label.ifBlank { appLabelForPackage(profile.packageName) }

private fun formatTimeAgo(time: Instant): String {
    val now = Instant.now()
    val duration = Duration.between(time, now)
    return when {
        duration.toMinutes() < 1 -> "now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
        duration.toHours() < 24 -> "${duration.toHours()}h ago"
        else -> "${duration.toDays()}d ago"
    }
}

private fun formatEvidenceTimestamp(timestamp: Instant): String = DateTimeFormatter
    .ofPattern("MMM d, yyyy h:mm a")
    .withZone(ZoneId.systemDefault())
    .format(timestamp)

private fun formatDateHeader(time: Instant): String {
    val local = time.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now(ZoneId.systemDefault())
    return when (local) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> DateTimeFormatter.ofPattern("MMM d").format(local)
    }
}

private fun formatByteCount(byteCount: Long): String {
    if (byteCount < 1024) return "$byteCount B"
    val kb = byteCount / 1024.0
    if (kb < 1024) return "${"%.2f".format(kb)} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${"%.2f".format(mb)} MB"
    val gb = mb / 1024.0
    return "${"%.2f".format(gb)} GB"
}

private fun formatElapsedTimeFrom(now: Instant, from: Instant): String {
    val duration = Duration.between(from, now)
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60
    return if (duration.toHours() > 0) "%02d:%02d:%02d".format(
        duration.toHours(),
        minutes,
        seconds,
    ) else "%02d:%02d".format(minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyxWatchTopBar(
    activeTab: MainTab,
    onAlertTabSelected: () -> Unit,
    onSettingsTabSelected: () -> Unit,
    onMenuClicked: () -> Unit = {},
    onSearchClicked: () -> Unit = {},
    onFilterClicked: () -> Unit = {},
    onCalendarClicked: () -> Unit = {},
) {
    val title = when (activeTab) {
        MainTab.Dashboard -> "Dashboard"
        MainTab.Alerts -> "Alerts"
        MainTab.Monitor -> "Monitor"
        MainTab.Reports -> "Reports"
        MainTab.Settings -> "Settings"
    }

    val actions = when (activeTab) {
        MainTab.Dashboard -> listOf(BarAction("\uD83D\uDD14", "Open alerts", onAlertTabSelected))
        MainTab.Alerts -> listOf(BarAction("\u2261", "Open filters", onFilterClicked))
        MainTab.Monitor -> listOf(
            BarAction("\uD83D\uDD0D", "Search", onSearchClicked),
            BarAction("\u22EE", "Open options", onSettingsTabSelected),
        )
        MainTab.Reports -> listOf(BarAction("\uD83D\uDCC5", "Open date selector", onCalendarClicked))
        MainTab.Settings -> emptyList()
    }

    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = UiTokens.TextPrimary,
                modifier = Modifier.padding(start = if (activeTab == MainTab.Settings) 0.dp else 4.dp),
            )
        },
        navigationIcon = {
            if (activeTab != MainTab.Settings) {
                ActionGlyph(
                    glyph = "\u2630",
                    a11yDescription = "Open menu",
                    onClick = onMenuClicked,
                )
            }
        },
        actions = {
            actions.forEach { action ->
                ActionGlyph(
                    glyph = action.glyph,
                    a11yDescription = action.description,
                    onClick = action.onClick,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = UiTokens.BgPrimary,
            titleContentColor = UiTokens.TextPrimary,
            navigationIconContentColor = UiTokens.TextSecondary,
            actionIconContentColor = UiTokens.TextSecondary,
        ),
    )
}

private data class BarAction(
    val glyph: String,
    val description: String,
    val onClick: () -> Unit,
)

@Composable
private fun ActionGlyph(
    glyph: String,
    a11yDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Text(
            glyph,
            color = UiTokens.TextSecondary,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
            maxLines = 1,
            modifier = Modifier.semantics { contentDescription = a11yDescription },
        )
    }
}

@Composable
fun CyxWatchBottomNav(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, UiTokens.Border),
        color = UiTokens.NavBg,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val textColor = if (isSelected) UiTokens.AccentBlueLight else UiTokens.TextSecondary
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(tabIcon(tab), color = textColor, style = MaterialTheme.typography.labelSmall)
                    Text(
                        tab.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                    )
                }
            }
        }
    }
}

private fun tabIcon(tab: MainTab): String = when (tab) {
    MainTab.Dashboard -> "\u2302"
    MainTab.Alerts -> "\u26A0"
    MainTab.Monitor -> "\uD83D\uDCBB"
    MainTab.Reports -> "\uD83D\uDCCA"
    MainTab.Settings -> "\u2699"
}

@Composable
fun RedesignedDashboardScreen(
    modifier: Modifier = Modifier,
    activeAlerts: List<PrivacyAlert>,
    suppressedAlertCount: Int,
    lastUsageEvents: List<PrivacyEvent>,
    lastNetworkEvents: List<PrivacyEvent>,
    lastInventoryProfiles: List<AppProfile>,
    privacyScore: PrivacyScore,
    retentionStatus: String,
    vpnModeEnabled: Boolean,
    runtimeIntegrityNotice: String? = null,
    isCollectingUsage: Boolean,
    isCollectingNetwork: Boolean,
    isCollectingInventory: Boolean,
    collectStatus: String,
    networkStatus: String,
    inventoryStatus: String,
    inventoryChangeStatus: String,
    retentionSettings: RetentionSettings,
    allowedRetentionDays: List<Int>,
    onCollectUsageClick: () -> Unit,
    onCollectNetworkClick: () -> Unit,
    onCollectInventoryClick: () -> Unit,
    onOpenProfile: (AppProfile) -> Unit,
    onOpenAlert: (PrivacyAlert) -> Unit,
    onOpenDailySummary: () -> Unit,
    onOpenAllAlerts: () -> Unit,
    onOpenMonitoredApps: () -> Unit,
    onOpenTransparencySettings: () -> Unit,
    onClearAlertHistoryClick: () -> Unit = {},
) {
    val reasonsByPackage = privacyScore.reasons.groupBy { it.packageName }

    val monitoredApps = lastInventoryProfiles.map { profile ->
        val latestEvent = lastUsageEvents
            .filter { it.packageName == profile.packageName }
            .maxByOrNull { it.timestamp }
        val statusLine = when (latestEvent?.eventType) {
            EventType.APP_FOREGROUND -> "Foreground running"
            EventType.APP_BACKGROUND -> "Background - ${formatElapsedTimeFrom(Instant.now(), latestEvent.timestamp)}"
            EventType.NETWORK_USAGE -> "Background network"
            EventType.SCREEN_STATE -> "Screen event"
            EventType.PERMISSION_CHANGED -> "Permissions updated"
            else -> "No events"
        }
        val isRunning = latestEvent?.eventType == EventType.APP_FOREGROUND
        val reasons = reasonsByPackage[profile.packageName].orEmpty()
        val severity = when {
            reasons.any { it.rule.isCriticalWarning() } -> ThreatSeverity.High
            reasons.any { it.rule == ScoringRule.MediumBackgroundNetwork || it.rule == ScoringRule.ScreenOffAppActivity } -> ThreatSeverity.Medium
            reasons.isNotEmpty() -> ThreatSeverity.Low
            else -> ThreatSeverity.Safe
        }
        MonitoredAppDisplay(profile, appLabel(profile), statusLine, isRunning, severity)
    }

    val threatsDetected = activeAlerts.size
    val suspiciousEvents = privacyScore.reasons.size
    val appsMonitored = lastInventoryProfiles.size
    val recentAlerts = activeAlerts.sortedByDescending { it.triggeredAt }.take(3)

    val networkEventsIn24h = lastNetworkEvents.size
    val usageEventsIn24h = lastUsageEvents.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!runtimeIntegrityNotice.isNullOrBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1C0A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5A2E0A)),
            ) {
                Text(
                    runtimeIntegrityNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFD8B0),
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        if (suppressedAlertCount > 0) {
            Text(
                "Suppressed alerts: $suppressedAlertCount",
                color = UiTokens.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2218)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A4731)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(UiTokens.StatusSafe.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "\uD83D\uDEE1",
                        color = UiTokens.StatusSafe,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Your device is protected", color = UiTokens.StatusSafe, style = MaterialTheme.typography.titleSmall)
                    Text("Monitoring is active", style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(UiTokens.StatusSafe, CircleShape),
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Security Overview", style = MaterialTheme.typography.titleMedium, color = UiTokens.TextPrimary)
                    Text("Last 24 hours", color = UiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    DashboardMetric(
                        "Threats",
                        threatsDetected.toString(),
                        UiTokens.StatusHigh,
                        "\u26A0",
                        Modifier.weight(1f),
                    )
                    DashboardMetric(
                        "Suspicious",
                        suspiciousEvents.toString(),
                        UiTokens.StatusMedium,
                        "\uD83D\uDCCA",
                        Modifier.weight(1f),
                    )
                    DashboardMetric(
                        "Apps",
                        appsMonitored.toString(),
                        UiTokens.AccentBlueLight,
                        "\uD83D\uDCC2",
                        Modifier.weight(1f),
                    )
                    DashboardMetric(
                        "Realtime",
                        if (vpnModeEnabled) "On" else "Off",
                        UiTokens.StatusSafe,
                        "\uD83D\uDEE1",
                        Modifier.weight(1f),
                    )
                }
            }
        }

        SectionTitle("Recent Alerts", "View all") { onOpenAllAlerts() }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            if (recentAlerts.isEmpty()) {
                EmptyState("No alerts yet")
            } else {
                Column {
                    recentAlerts.forEachIndexed { index, alert ->
                        DashboardAlertRow(alert = alert) { onOpenAlert(alert) }
                        if (index != recentAlerts.lastIndex) {
                            HorizontalDivider(color = UiTokens.Border, modifier = Modifier.padding(start = 62.dp))
                        }
                    }
                }
            }
        }

        SectionTitle("Monitored Apps", "View all") { onOpenMonitoredApps() }
        if (monitoredApps.isEmpty()) {
            EmptyState("No apps monitored")
        } else {
            val app = monitoredApps.first()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProfile(app.profile) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppInitialIcon(app.label)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(app.label, style = MaterialTheme.typography.titleSmall, color = UiTokens.TextPrimary)
                        Text(app.statusLine, style = MaterialTheme.typography.bodySmall, color = UiTokens.AccentBlueLight)
                    }
                    ThreatPill(label = app.severity.label, severity = app.severity)
                    Text(CHEVRON_RIGHT, color = UiTokens.TextMuted)
                }
            }
        }

        SectionTitle("Data collection")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DashboardCollectionAction(
                        label = if (isCollectingUsage) "Collecting..." else "Collect usage",
                        enabled = !isCollectingUsage,
                        onClick = onCollectUsageClick,
                        modifier = Modifier.weight(1f),
                    )
                    DashboardCollectionAction(
                        label = if (isCollectingNetwork) "Collecting..." else "Collect network",
                        enabled = !isCollectingNetwork,
                        onClick = onCollectNetworkClick,
                        modifier = Modifier.weight(1f),
                    )
                    DashboardCollectionAction(
                        label = if (isCollectingInventory) "Collecting..." else "Collect apps",
                        enabled = !isCollectingInventory,
                        onClick = onCollectInventoryClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                DashboardStatusLine("Collection status", collectStatus)
                if (networkStatus.isNotBlank()) {
                    DashboardStatusLine("Network", networkStatus)
                }
                if (inventoryStatus.isNotBlank()) {
                    DashboardStatusLine("Apps", inventoryStatus)
                }
                if (retentionStatus.isNotBlank()) {
                    DashboardStatusLine("Retention", retentionStatus)
                }
                if (inventoryChangeStatus.isNotBlank()) {
                    DashboardStatusLine("Inventory", inventoryChangeStatus, compact = true)
                }
                if (usageEventsIn24h > 0 || networkEventsIn24h > 0) {
                    DashboardStatusLine(
                        "Last 24h",
                        "$usageEventsIn24h usage event(s), $networkEventsIn24h network event(s)",
                        compact = true,
                    )
                }
                if (allowedRetentionDays.isNotEmpty()) {
                    val selectedRetention = retentionSettings.retentionDays
                    val min = allowedRetentionDays.minOrNull() ?: selectedRetention
                    val max = allowedRetentionDays.maxOrNull() ?: selectedRetention
                    DashboardStatusLine("Retention window", "$selectedRetention days (allowed $min-$max)")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Daily summary", style = MaterialTheme.typography.labelSmall, color = UiTokens.AccentBlueLight, modifier = Modifier.clickable { onOpenDailySummary() })
                    Text("Advanced settings", style = MaterialTheme.typography.labelSmall, color = UiTokens.AccentBlueLight, modifier = Modifier.clickable { onOpenTransparencySettings() })
                    Text("Clear alerts", style = MaterialTheme.typography.labelSmall, color = UiTokens.StatusMedium, modifier = Modifier.clickable { onClearAlertHistoryClick() })
                }
            }
        }
    }
}

private @Composable
fun DashboardMetric(
    label: String,
    value: String,
    color: Color,
    iconHint: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Text(
            iconHint,
            color = color,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = UiTokens.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = UiTokens.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DashboardCollectionAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DashboardStatusLine(
    label: String,
    value: String,
    compact: Boolean = false,
) {
    val lines = if (compact) 2 else 1
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            "$label:",
            color = UiTokens.TextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(104.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = UiTokens.TextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private @Composable
fun DashboardAlertRow(
    alert: PrivacyAlert,
    onOpenAlert: () -> Unit,
) {
    val data = alertDetails(alert)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenAlert() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeverityDot(color = data.severity.dot)
        ThreatIconBox(data.severity.iconBg, data.severity.dot, data.icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(data.title, style = MaterialTheme.typography.titleSmall, color = UiTokens.TextPrimary)
            Text(data.subtitle, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(formatTimeAgo(alert.triggeredAt), style = MaterialTheme.typography.labelSmall, color = UiTokens.TextSecondary)
        }
        Text(CHEVRON_RIGHT, color = UiTokens.TextSecondary)
    }
}

private @Composable
fun SeverityDot(color: Color, size: Int = 8) {
    Box(
        modifier = Modifier.size(size.dp).background(color, CircleShape),
    )
}

private @Composable
fun ThreatPill(label: String, severity: ThreatSeverity) {
    Text(
        label,
        color = severity.dot,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        modifier = Modifier
            .background(severity.bg, RoundedCornerShape(999.dp))
            .border(1.dp, severity.border, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

private data class MonitoredAppDisplay(
    val profile: AppProfile,
    val label: String,
    val statusLine: String,
    val isRunning: Boolean,
    val severity: ThreatSeverity,
)

@Composable
fun AlertsScreen(
    modifier: Modifier = Modifier,
    alerts: List<PrivacyAlert>,
    activeFilter: AlertSeverityFilter,
    onFilterSelected: (AlertSeverityFilter) -> Unit,
    onAlertClick: (PrivacyAlert) -> Unit,
) {
    val filtered = alerts.filter {
        when (activeFilter) {
            AlertSeverityFilter.All -> true
            AlertSeverityFilter.High -> alertSeverity(it) == ThreatSeverity.High
            AlertSeverityFilter.Medium -> alertSeverity(it) == ThreatSeverity.Medium
            AlertSeverityFilter.Low -> alertSeverity(it) == ThreatSeverity.Low
        }
    }.sortedByDescending { it.triggeredAt }

    val grouped = filtered.groupBy { alert -> formatDateHeader(alert.triggeredAt) }.toList().sortedByDescending {
        when (it.first) {
            "Today" -> Long.MAX_VALUE
            "Yesterday" -> Long.MAX_VALUE - 1
            else -> it.first.hashCode().toLong()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AlertSeverityFilter.values()) { filter ->
                val selected = filter == activeFilter
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) UiTokens.AccentBlue else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UiTokens.AccentBlue else UiTokens.Border),
                    modifier = Modifier.clickable { onFilterSelected(filter) },
                ) {
                    Text(
                        filter.label,
                        color = if (selected) Color.White else UiTokens.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyState("No ${activeFilter.label.lowercase()} alerts")
            return@Column
        }

        grouped.forEach { (dateLabel, alertsForDate) ->
            Text(dateLabel, style = MaterialTheme.typography.labelLarge, color = UiTokens.TextMuted)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
            ) {
                Column {
                    alertsForDate.forEachIndexed { index, alert ->
                        AlertCardRow(alert = alert, onOpen = onAlertClick)
                        if (index != alertsForDate.lastIndex) {
                            HorizontalDivider(color = UiTokens.Border, modifier = Modifier.padding(start = 62.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCardRow(
    alert: PrivacyAlert,
    onOpen: (PrivacyAlert) -> Unit,
) {
    val info = alertDetails(alert)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(alert) }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeverityDot(color = info.severity.dot)
        ThreatIconBox(info.severity.iconBg, info.severity.dot, info.icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(info.title, style = MaterialTheme.typography.titleSmall, color = UiTokens.TextPrimary)
            Text(info.subtitle, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(formatTimeAgo(alert.triggeredAt), style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary)
            ThreatPill(label = info.severity.label, severity = info.severity)
        }
        Text(CHEVRON_RIGHT, color = UiTokens.TextSecondary)
    }
}

@Composable
private fun ThreatIconBox(background: Color, iconColor: Color, icon: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(background, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, color = iconColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MonitorScreen(
    modifier: Modifier = Modifier,
    appProfiles: List<AppProfile>,
    appLabelsByPackageName: Map<String, String>,
    riskByApp: Map<String, List<ScoreReason>>,
    usageEvents: List<PrivacyEvent>,
    activeFilter: MonitorStatusFilter,
    onFilterSelected: (MonitorStatusFilter) -> Unit,
    onAppClick: (AppProfile) -> Unit,
) {
    val entries = appProfiles.map { profile ->
        val latestEvent = usageEvents
            .filter { it.packageName == profile.packageName }
            .maxByOrNull { it.timestamp }
        val reasons = riskByApp[profile.packageName].orEmpty()
        val severity = when {
            reasons.any { it.rule.isCriticalWarning() } -> ThreatSeverity.High
            reasons.any { it.rule == ScoringRule.MediumBackgroundNetwork || it.rule == ScoringRule.KeyguardAppActivity } -> ThreatSeverity.Medium
            reasons.isNotEmpty() -> ThreatSeverity.Low
            else -> ThreatSeverity.Safe
        }
        val isRunning = latestEvent?.eventType == EventType.APP_FOREGROUND
        val statusText = when (latestEvent?.eventType) {
            EventType.APP_FOREGROUND -> "Foreground"
            EventType.APP_BACKGROUND -> "Background for ${formatElapsedTimeFrom(Instant.now(), latestEvent.timestamp)}"
            EventType.NETWORK_USAGE -> "Network activity"
            EventType.SCREEN_STATE -> "Screen state"
            EventType.PERMISSION_CHANGED -> "Permissions changed"
            null -> "No events"
        }
        MonitorRow(profile, appLabelsByPackageName[profile.packageName] ?: appLabel(profile), statusText, isRunning, severity)
    }

    val running = entries.count { it.isRunning }
    val background = entries.size - running
    val highRisk = entries.count { it.severity == ThreatSeverity.High }
    val filtered = when (activeFilter) {
        MonitorStatusFilter.All -> entries
        MonitorStatusFilter.Running -> entries.filter { it.isRunning }
        MonitorStatusFilter.Background -> entries.filterNot { it.isRunning }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MonitorStatusFilter.values()) { filter ->
                val selected = filter == activeFilter
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) UiTokens.AccentBlue else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UiTokens.AccentBlue else UiTokens.Border),
                    modifier = Modifier.clickable { onFilterSelected(filter) },
                ) {
                    Text(
                        filter.label,
                        color = if (selected) Color.White else UiTokens.TextSecondary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MonitorStatCell("Total", entries.size.toString(), UiTokens.AccentBlueLight, "\uD83D\uDCC2", Modifier.weight(1f))
                MonitorStatCell("Running", running.toString(), UiTokens.StatusMedium, "\u25B6", Modifier.weight(1f))
                MonitorStatCell("Background", background.toString(), UiTokens.StatusMedium, "\u21BB", Modifier.weight(1f))
                MonitorStatCell("High Risk", highRisk.toString(), UiTokens.StatusHigh, "\u26A0", Modifier.weight(1f))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(UiTokens.AccentBlueLight, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("\uD83D\uDCF1", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        "CyxWatch is monitoring apps for suspicious behavior in real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UiTokens.TextSecondary,
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            val emptyMessage = when (activeFilter) {
                MonitorStatusFilter.All -> "No monitored apps yet"
                MonitorStatusFilter.Running -> "No running apps"
                MonitorStatusFilter.Background -> "No background apps"
            }
            EmptyState(emptyMessage)
            return@Column
        }

        if (activeFilter == MonitorStatusFilter.All || activeFilter == MonitorStatusFilter.Running) {
            SectionTitle(title = "Running (${running})")
            AppSection(entries = filtered.filter { it.isRunning }, onOpen = onAppClick)
        }
        if (activeFilter == MonitorStatusFilter.All || activeFilter == MonitorStatusFilter.Background) {
            SectionTitle(title = "Background (${background})")
            AppSection(entries = filtered.filterNot { it.isRunning }, onOpen = onAppClick)
        }
    }
}

private data class MonitorRow(
    val profile: AppProfile,
    val label: String,
    val statusText: String,
    val isRunning: Boolean,
    val severity: ThreatSeverity,
)

@Composable
private fun AppSection(entries: List<MonitorRow>, onOpen: (AppProfile) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
        border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
    ) {
        Column {
            entries.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(row.profile) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppInitialIcon(row.label)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(row.label, style = MaterialTheme.typography.titleSmall, color = UiTokens.TextPrimary)
                        Text(row.statusText, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary)
                    }
                    ThreatPill(row.severity.label, row.severity)
                    Text(CHEVRON_RIGHT, color = UiTokens.TextMuted)
                }
                if (index != entries.lastIndex) {
                    HorizontalDivider(color = UiTokens.Border, modifier = Modifier.padding(start = 58.dp))
                }
            }
        }
    }
}

@Composable
private fun MonitorStatCell(
    label: String,
    value: String,
    iconColor: Color,
    icon: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(icon, style = MaterialTheme.typography.labelLarge, color = iconColor)
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = UiTokens.TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = UiTokens.TextSecondary)
    }
}

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    reportsPeriod: ReportsPeriod,
    onPeriodChanged: (ReportsPeriod) -> Unit,
    alerts: List<PrivacyAlert>,
    onThreatClick: (PrivacyAlert) -> Unit,
    privacyScore: PrivacyScore,
    onOpenAlertScreen: () -> Unit,
) {
    val sortedAlerts = alerts.sortedByDescending { it.triggeredAt }
    val threatCount = sortedAlerts.size
    val suspiciousCount = sortedAlerts.sumOf { max(1, kotlin.math.abs(it.triggerDelta)) }
    val appCount = sortedAlerts.map { it.packageName }.distinct().size
    val scoreLabel = "${privacyScore.score}%"
    val topCategories = listOf(
        Triple("Privacy Risk", "60%", Color(0xFFEF4444)),
        Triple("Data Leak", "25%", Color(0xFFF59E0B)),
        Triple("System Risk", "15%", Color(0xFFEAB308)),
    )

    val trendPoints = when (reportsPeriod) {
        ReportsPeriod.Daily -> listOf(1f, 2f, 0f, 6f, 5f, 4f, 3f)
        ReportsPeriod.Weekly -> listOf(2f, 3f, 4f, 1f, 5f, 2f, 4f)
        ReportsPeriod.Monthly -> listOf(3f, 1f, 6f, 4f, 5f, 2f, 7f)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ReportsPeriod.values()) { period ->
                val selected = period == reportsPeriod
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) UiTokens.AccentBlue else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UiTokens.AccentBlue else UiTokens.Border),
                    modifier = Modifier.clickable { onPeriodChanged(period) },
                ) {
                    Text(
                        period.label,
                        color = if (selected) Color.White else UiTokens.TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Summary", style = MaterialTheme.typography.titleMedium, color = UiTokens.TextPrimary)
                    Text("May 12, 2024", style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                DashboardMetric("Threats", threatCount.toString(), UiTokens.StatusHigh, "\u26A0", Modifier.weight(1f))
                DashboardMetric("Suspicious", suspiciousCount.toString(), UiTokens.StatusMedium, "\uD83D\uDCCA", Modifier.weight(1f))
                DashboardMetric("Apps", appCount.toString(), UiTokens.AccentBlueLight, "\uD83D\uDCC2", Modifier.weight(1f))
                DashboardMetric("Score", scoreLabel, UiTokens.StatusSafe, "\uD83D\uDCB0", Modifier.weight(1f))
            }
        }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Threats over time", style = MaterialTheme.typography.titleSmall, color = UiTokens.TextPrimary)
                    Surface(
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
                    ) {
                        Text(
                            "All Threats $CHEVRON_DOWN",
                            color = UiTokens.TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                ReportsLineChart(points = trendPoints, color = UiTokens.StatusHigh)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("12AM", "6AM", "12PM", "6PM", "12AM").forEach {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = UiTokens.TextMuted)
                    }
                }
            }
        }

        Text("Top Risk Categories", style = MaterialTheme.typography.titleSmall, color = UiTokens.TextPrimary)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ReportsDonutChart(topCategories)
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    topCategories.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
            .background(item.third, RoundedCornerShape(50)),
            )
            Text(item.first, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextPrimary)
        }
        Text(item.second, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextPrimary)
        }
    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recent Threats", style = MaterialTheme.typography.titleSmall, color = UiTokens.TextPrimary)
            Text(
                "View all $CHEVRON_RIGHT",
                color = UiTokens.AccentBlueLight,
                modifier = Modifier.clickable { onOpenAlertScreen() },
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            if (sortedAlerts.isEmpty()) {
                EmptyState("No threats yet")
            } else {
                Column {
                    sortedAlerts.take(4).forEachIndexed { index, alert ->
                        val info = alertDetails(alert)
                        ReportThreatRow(alert = alert, data = info, onOpen = onThreatClick)
                        if (index != minOf(3, sortedAlerts.lastIndex)) {
                            HorizontalDivider(color = UiTokens.Border, modifier = Modifier.padding(start = 62.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsLineChart(points: List<Float>, color: Color) {
    val values = points.ifEmpty { listOf(0f, 0f, 0f, 0f, 0f) }
    val max = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
    ) {
        val chartHeight = size.height - 16f
        val chartWidth = size.width - 16f
        val yStep = chartHeight / max
        val xStep = chartWidth / (values.size - 1).coerceAtLeast(1)
        val linePoints = values.mapIndexed { index, value ->
            Offset(8f + index * xStep, 8f + (max - value) * yStep)
        }

        for (i in 0..4) {
            val y = 8f + i * (chartHeight / 4f)
            drawLine(
                color = UiTokens.Border,
                start = Offset(8f, y),
                end = Offset(size.width - 8f, y),
                strokeWidth = 1f,
            )
        }

        val fillPath = Path().apply {
            moveTo(linePoints.first().x, size.height - 8f)
            linePoints.forEach { point -> lineTo(point.x, point.y) }
            lineTo(linePoints.last().x, size.height - 8f)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = size.height,
            ),
        )

        values.indices.zipWithNext().forEach { (index, next) ->
            drawLine(
                color = color,
                start = linePoints[index],
                end = linePoints[next],
                strokeWidth = 2.4f,
                cap = StrokeCap.Round,
            )
        }
        values.indices.forEach { index ->
            drawCircle(color = color, radius = 4f, center = linePoints[index])
        }
    }
}

@Composable
private fun ReportsDonutChart(items: List<Triple<String, String, Color>>) {
    val total = items.sumOf { it.second.trimEnd('%').toDoubleOrNull() ?: 0.0 }.coerceAtLeast(1.0)
    Canvas(modifier = Modifier.size(108.dp)) {
        val strokePx = 18.dp.toPx()
        val safeSize = size.width.coerceAtMost(size.height) - strokePx
        val topLeft = Offset(
            x = (size.width - safeSize) / 2f + strokePx / 2f,
            y = (size.height - safeSize) / 2f + strokePx / 2f,
        )
        var start = -90f
        items.forEach { item ->
            val value = item.second.trimEnd('%').toFloatOrNull() ?: 0f
            val angle = ((value / total) * 360.0).toFloat()
            drawArc(
                color = item.third,
                startAngle = start,
                sweepAngle = angle - 1.5f,
                useCenter = false,
                topLeft = topLeft,
                size = androidx.compose.ui.geometry.Size(safeSize, safeSize),
                style = Stroke(width = strokePx),
            )
            start += angle
        }
    }
}

@Composable
private fun ReportThreatRow(alert: PrivacyAlert, data: ThreatRenderData, onOpen: (PrivacyAlert) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(alert) }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThreatIconBox(data.severity.iconBg, data.severity.dot, data.icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(data.title, style = MaterialTheme.typography.titleSmall, color = UiTokens.TextPrimary)
            Text(data.subtitle, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(formatTimeAgo(alert.triggeredAt), style = MaterialTheme.typography.labelSmall, color = UiTokens.TextSecondary)
            ThreatPill(label = data.severity.label, severity = data.severity)
            Text(CHEVRON_RIGHT, color = UiTokens.TextSecondary)
        }
    }
}

@Composable
fun ThreatDetailsScreen(
    modifier: Modifier = Modifier,
    reason: ScoreReason,
    evidenceEvents: List<PrivacyEvent>,
    appLabelsByPackageName: Map<String, String> = emptyMap(),
    onBack: () -> Unit,
    onRestrictApp: () -> Unit = {},
    onIgnore: () -> Unit = {},
) {
    var confirmRestrict by remember { mutableStateOf(false) }
    var confirmIgnore by remember { mutableStateOf(false) }
    val detailData = threatDetailsForRule(reason.rule)
    val appName = appLabelsByPackageName[reason.packageName] ?: appLabelForPackage(reason.packageName)
    val detectedAt = evidenceEvents.maxByOrNull { it.timestamp }?.timestamp ?: Instant.now()

    val metadataRows = listOf(
        "App" to appName,
        "Package" to reason.packageName,
        "Category" to reasonCategory(reason.rule),
        "Details" to (reason.message.ifBlank { "No additional details available." }),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "←",
                style = MaterialTheme.typography.titleLarge,
                color = UiTokens.TextSecondary,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp),
            )
            Text(
                "Threat Details",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = UiTokens.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(" ", style = MaterialTheme.typography.titleLarge, color = UiTokens.TextPrimary)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(UiTokens.BgDanger, RoundedCornerShape(36.dp))
                    .border(1.dp, detailData.severity.border, RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center,
            ) {
                ThreatIconBox(detailData.severity.iconBg, detailData.severity.dot, detailData.icon)
            }
            Text(detailData.title, style = MaterialTheme.typography.titleLarge, color = UiTokens.TextPrimary)
            ThreatPill(label = "${detailData.severity.label} Risk", severity = detailData.severity)
            Text(
                formatEvidenceTimestamp(detectedAt),
                style = MaterialTheme.typography.bodySmall,
                color = UiTokens.TextSecondary,
            )
        }

        Text("Description", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = UiTokens.TextMuted))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            Text(
                detailData.subtitle + if (detailData.subtitle != reason.message) "\n${reason.message}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = UiTokens.TextSecondary,
                modifier = Modifier.padding(12.dp),
            )
        }

        Text(
            "Metadata",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = UiTokens.TextMuted),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            Column {
                metadataRows.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            entry.first,
                            style = MaterialTheme.typography.bodySmall,
                            color = UiTokens.TextSecondary,
                            modifier = Modifier.width(90.dp),
                        )
                        Text(
                            entry.second,
                            style = MaterialTheme.typography.bodyMedium,
                            color = UiTokens.TextPrimary,
                        )
                    }
                    if (index != metadataRows.lastIndex) {
                        HorizontalDivider(color = UiTokens.Border, modifier = Modifier.padding(start = 102.dp))
                    }
                }
            }
        }

        Text(
            "Recommendation",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = UiTokens.TextMuted),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            Text(
                recommendationForRule(reason.rule),
                style = MaterialTheme.typography.bodySmall,
                color = UiTokens.TextSecondary,
                modifier = Modifier.padding(12.dp),
            )
        }

        Button(
            onClick = { confirmRestrict = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = UiTokens.StatusHigh,
                contentColor = Color.White,
            ),
        ) {
            Text(
                "Restrict App",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            "Ignore",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = UiTokens.TextSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { confirmIgnore = true }
                .padding(vertical = 4.dp),
        )
    }

    if (confirmRestrict) {
        AlertDialog(
            onDismissRequest = { confirmRestrict = false },
            title = { Text("Restrict app") },
            text = {
                Text("Restrict background data for $appName?")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestrict = false
                    onRestrictApp()
                }) {
                    Text("Restrict")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestrict = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (confirmIgnore) {
        AlertDialog(
            onDismissRequest = { confirmIgnore = false },
            title = { Text("Ignore threat") },
            text = {
                Text("Ignore and clear this threat from the list?")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmIgnore = false
                    onIgnore()
                }) {
                    Text("Ignore")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmIgnore = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    realTimeProtectionEnabled: Boolean,
    notificationsEnabled: Boolean,
    theme: String,
    language: String,
    scanSchedule: String,
    onRealtimeProtectionToggled: (Boolean) -> Unit,
    onNotificationsToggled: (Boolean) -> Unit,
    onIgnoreList: () -> Unit,
    onScanScheduleClicked: () -> Unit,
    onThemeClicked: () -> Unit,
    onLanguageClicked: () -> Unit,
    onDataUsageClicked: () -> Unit,
    onSuspiciousAppsClicked: () -> Unit,
    onAboutClicked: () -> Unit,
    onPrivacyPolicyClicked: () -> Unit,
    onTermsClicked: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = UiTokens.TextPrimary,
            )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(com.cyxwatch.app.R.drawable.cyxwatch_logo_new),
                        contentDescription = "CyxWatch logo",
                        modifier = Modifier.size(28.dp),
                    )
                    Column {
                        Text("Cyx", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = UiTokens.TextPrimary)
                        Text("Watch", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = UiTokens.AccentBlueLight)
                    }
                }
            }

        SettingsSection("PROTECTION")
        SettingsCard {
            SettingsToggleRow(
                icon = "\uD83D\uDEE1",
                iconBg = Color(0xFF0D3320),
                iconTint = UiTokens.StatusSafe,
                title = "Real-time Protection",
                subtitle = "Monitor apps and detect suspicious activity",
                checked = realTimeProtectionEnabled,
                onToggle = onRealtimeProtectionToggled,
            )
            SettingsToggleRow(
                icon = "\uD83D\uDD14",
                iconBg = Color(0xFF1A0D40),
                iconTint = Color(0xFFA78BFA),
                title = "Notifications",
                subtitle = "Get alerts about threats and activities",
                checked = notificationsEnabled,
                onToggle = onNotificationsToggled,
            )
            SettingsActionRow(
                icon = "\u26D4",
                iconBg = Color(0xFF2D1A08),
                iconTint = Color(0xFFF97316),
                title = "Ignore List",
                subtitle = "Manage apps and services to ignore",
                onClick = onIgnoreList,
            )
            SettingsActionRow(
                icon = "\u23F2",
                iconBg = Color(0xFF0A1A3D),
                iconTint = UiTokens.AccentBlueLight,
                title = "Scan Schedule",
                subtitle = "Set automatic scan frequency",
                value = scanSchedule,
                onClick = onScanScheduleClicked,
            )
        }

        SettingsSection("PREFERENCES")
        SettingsCard {
            SettingsActionRow(
                icon = "\uD83C\uDFA8",
                iconBg = Color(0xFF2D0D40),
                iconTint = Color(0xFFA78BFA),
                title = "Theme",
                subtitle = "Choose your preferred theme",
                value = theme,
                onClick = onThemeClicked,
            )
            SettingsActionRow(
                icon = "\uD83C\uDF10",
                iconBg = Color(0xFF0A2D3D),
                iconTint = Color(0xFF22D3EE),
                title = "Language",
                subtitle = "Select app language",
                value = language,
                onClick = onLanguageClicked,
            )
            SettingsActionRow(
                icon = "\uD83D\uDCCA",
                iconBg = Color(0xFF0D3320),
                iconTint = UiTokens.StatusSafe,
                title = "Data Usage",
                subtitle = "Manage monitoring and data usage",
                onClick = onDataUsageClicked,
            )
        }

        SettingsSection("ABOUT")
        SettingsCard {
            SettingsActionRow(
                icon = "\u2139",
                iconBg = Color(0xFF0A1A3D),
                iconTint = UiTokens.AccentBlueLight,
                title = "About CyxWatch",
                subtitle = "Version 1.0.0",
                onClick = onAboutClicked,
            )
            SettingsActionRow(
                icon = "\uD83D\uDEAB",
                iconBg = Color(0xFF0D3320),
                iconTint = UiTokens.StatusSafe,
                title = "Privacy Policy",
                subtitle = "How we handle your data",
                onClick = onPrivacyPolicyClicked,
            )
            SettingsActionRow(
                icon = "\uD83D\uDCDD",
                iconBg = Color(0xFF2D2208),
                iconTint = Color(0xFFEAB308),
                title = "Terms of Service",
                subtitle = "Read our terms and conditions",
                onClick = onTermsClicked,
            )
            SettingsActionRow(
                icon = "\uD83E\uDDF0",
                iconBg = Color(0xFF2D2208),
                iconTint = Color(0xFF93C5FD),
                title = "Suspicious App Reports",
                subtitle = "See apps frequently reported by the community",
                onClick = onSuspiciousAppsClicked,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0808)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D1010)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF2D0A0A), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("X", color = UiTokens.StatusHigh)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Log Out", style = MaterialTheme.typography.titleSmall, color = UiTokens.StatusHigh, fontWeight = FontWeight.SemiBold)
                    Text("Sign out from your account", color = UiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Text("\u203A", color = UiTokens.StatusHigh)
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = UiTokens.TextPrimary)
        if (action != null && onAction != null) {
            Text(
                action,
                style = MaterialTheme.typography.bodySmall,
                color = UiTokens.AccentBlueLight,
                modifier = Modifier.clickable { onAction() },
            )
        }
    }
}

@Composable
private fun SettingsSection(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = UiTokens.TextMuted,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
        border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsDetailContainer(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "\u2039",
                color = UiTokens.TextSecondary,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.clickable { onBack() },
            )
            Column {
                Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = UiTokens.TextPrimary)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary)
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = UiTokens.BgSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiTokens.Border),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ScanScheduleSettingsScreen(
    selectedSchedule: String,
    onBack: () -> Unit,
    onSelectionChanged: (String) -> Unit,
) {
    val options = listOf("Every 6h", "Every 12h", "Every 24h", "Manual")
    SettingsDetailContainer(
        title = "Scan Schedule",
        subtitle = "Choose how often background checks run.",
        onBack = onBack,
    ) {
        settingsChoiceRows(
            title = "Scan frequency",
            options = options,
            selectedOption = selectedSchedule,
            onOptionSelected = onSelectionChanged,
        )
    }
}

@Composable
fun ThemeSettingsScreen(
    selectedTheme: String,
    onBack: () -> Unit,
    onSelectionChanged: (String) -> Unit,
) {
    val options = listOf("Dark", "Light", "System")
    SettingsDetailContainer(
        title = "Theme",
        subtitle = "Select the preferred visual theme.",
        onBack = onBack,
    ) {
        settingsChoiceRows(
            title = "Theme",
            options = options,
            selectedOption = selectedTheme,
            onOptionSelected = onSelectionChanged,
        )
    }
}

@Composable
fun LanguageSettingsScreen(
    selectedLanguage: String,
    onBack: () -> Unit,
    onSelectionChanged: (String) -> Unit,
) {
    val options = listOf("English", "Spanish", "French", "German", "Japanese")
    SettingsDetailContainer(
        title = "Language",
        subtitle = "Pick interface language.",
        onBack = onBack,
    ) {
        settingsChoiceRows(
            title = "Language",
            options = options,
            selectedOption = selectedLanguage,
            onOptionSelected = onSelectionChanged,
        )
    }
}

@Composable
fun IgnoreListSettingsScreen(
    ignoredPackages: List<String>,
    availableProfiles: List<AppProfile>,
    onBack: () -> Unit,
    onAddPackage: (String) -> Unit,
    onRemovePackage: (String) -> Unit,
) {
    var packageInput by remember { mutableStateOf("") }
    SettingsDetailContainer(
        title = "Ignore List",
        subtitle = "Apps you add here are excluded from alert cards.",
        onBack = onBack,
    ) {
        Text(
            "Manage ignored packages",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = UiTokens.TextMuted,
        )

        OutlinedTextField(
            value = packageInput,
            onValueChange = { packageInput = it },
            label = { Text("Package name") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("com.example.app") },
            singleLine = true,
        )
        Button(
            onClick = {
                onAddPackage(packageInput.trim())
                packageInput = ""
            },
            enabled = packageInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add to Ignore List")
        }

        Text(
            "Ignored apps",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = UiTokens.TextMuted,
        )
        if (ignoredPackages.isEmpty()) {
            EmptyState("No ignored apps. Add an app package name to hide it from lists.")
        } else {
            ignoredPackages.forEach { packageName ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = UiTokens.TextPrimary,
                        )
                        Text(
                            "Ignored",
                            style = MaterialTheme.typography.bodySmall,
                            color = UiTokens.TextSecondary,
                        )
                    }
                    OutlinedButton(onClick = { onRemovePackage(packageName) }) {
                        Text("Remove")
                    }
                }
                HorizontalDivider(color = UiTokens.Border)
            }
        }

        if (availableProfiles.isNotEmpty()) {
            Text(
                "Quick add from installed apps",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = UiTokens.TextMuted,
            )
            val quickAddCandidates = availableProfiles
                .filter { profile -> !ignoredPackages.contains(profile.packageName) }
                .take(5)
            quickAddCandidates.forEach { profile ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppInitialIcon(profile.label.ifBlank { profile.packageName })
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(profile.label.ifBlank { profile.packageName }, style = MaterialTheme.typography.bodyMedium, color = UiTokens.TextPrimary)
                        Text(profile.packageName, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary)
                    }
                    TextButton(onClick = { onAddPackage(profile.packageName) }) {
                        Text("Add")
                    }
                }
                HorizontalDivider(color = UiTokens.Border)
            }
        }
    }
}

@Composable
fun DataUsageSettingsScreen(
    hasUsageAccess: Boolean,
    retentionDays: Int,
    usageEventCount: Int,
    inventoryEventCount: Int,
    networkEventCount: Int,
    activeAlertCount: Int,
    ignoredPackageCount: Int,
    onBack: () -> Unit,
) {
    SettingsDetailContainer(
        title = "Data Usage",
        subtitle = "How monitoring data is kept and why.",
        onBack = onBack,
    ) {
        val statusText = if (hasUsageAccess) "Usage access granted" else "Usage access required"
        Text("Data model", style = MaterialTheme.typography.labelSmall, color = UiTokens.TextMuted)
        settingsStatRows(
            listOf(
                "Retention window" to "$retentionDays days",
                "Usage events" to usageEventCount.toString(),
                "Inventory events" to inventoryEventCount.toString(),
                "Network events" to networkEventCount.toString(),
                "Active alerts" to activeAlertCount.toString(),
                "Ignored apps" to ignoredPackageCount.toString(),
                "Source" to statusText,
            ),
        )

        Text(
            "All evidence and settings data remains on-device by design. " +
                "CyxWatch does not upload event streams or telemetry to a remote service.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Advanced VPN visibility is endpoint metadata only; it never stores packet payloads.",
            style = MaterialTheme.typography.bodySmall,
            color = UiTokens.TextSecondary,
        )
    }
}

@Composable
fun InfoSettingsScreen(
    title: String,
    body: String,
    onBack: () -> Unit,
) {
    SettingsDetailContainer(
        title = title,
        onBack = onBack,
    ) {
        Text(body, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary)
    }
}

@Composable
fun SuspiciousAppReportScreen(
    reports: List<SuspiciousAppSummary>,
    onBack: () -> Unit,
) {
    SettingsDetailContainer(
        title = "Suspicious App Reports",
        subtitle = "Apps frequently flagged by local and community analysis.",
        onBack = onBack,
    ) {
        if (reports.isEmpty()) {
            EmptyState("No frequent suspicious activity yet. Run monitoring and collect events to build this list.")
        } else {
            reports.forEach { report ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(report.appName, style = MaterialTheme.typography.titleSmall, color = UiTokens.TextPrimary)
                            Text(report.packageName, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary)
                            Text(report.topReason, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary)
                            Text("Source: ${report.source}", style = MaterialTheme.typography.bodySmall, color = UiTokens.TextMuted)
                        }
                        Text("${report.reportCount}", color = UiTokens.AccentBlueLight)
                    }
                    Text(
                        "Last seen: ${report.latestTimestampLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = UiTokens.TextMuted,
                    )
                    HorizontalDivider(color = UiTokens.Border)
                }
            }
        }
    }
}

@Composable
private fun settingsChoiceRows(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = UiTokens.TextMuted)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(option, style = MaterialTheme.typography.bodyMedium, color = UiTokens.TextPrimary)
                if (option == selectedOption) {
                    Text("\u2713", color = UiTokens.StatusSafe)
                }
            }
            HorizontalDivider(color = UiTokens.Border)
        }
    }
}

@Composable
private fun settingsStatRows(stats: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(pair.first, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary)
                Text(pair.second, style = MaterialTheme.typography.bodyMedium, color = UiTokens.TextPrimary)
            }
            HorizontalDivider(color = UiTokens.Border)
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: String,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    value: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBg, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = iconTint, fontWeight = FontWeight.Medium)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = UiTokens.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary)
        }
        value?.let {
            Text(it, color = UiTokens.AccentBlueLight)
        }
        Text(CHEVRON_RIGHT, color = UiTokens.TextSecondary)
    }
    HorizontalDivider(color = UiTokens.Border)
}

@Composable
private fun SettingsToggleRow(
    icon: String,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBg, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = iconTint, fontWeight = FontWeight.Medium)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = UiTokens.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = UiTokens.TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
    HorizontalDivider(color = UiTokens.Border)
}

@Composable
private fun AppInitialIcon(label: String) {
    val letter = label.firstOrNull()?.uppercaseChar() ?: '?'
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(UiTokens.BgTertiary, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter.toString(), color = UiTokens.AccentBlueLight, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = UiTokens.TextSecondary)
    }
}
