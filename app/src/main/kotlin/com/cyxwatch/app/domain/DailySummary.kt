package com.cyxwatch.app.domain

import java.time.Instant
import java.util.Locale

data class DailySummary(
    val dateLabel: String,
    val generatedAt: Instant,
    val windowStart: Instant,
    val windowEnd: Instant,
    val score: Int,
    val topReasons: List<ScoreReason>,
    val topAlertCount: Int,
    val recentAlerts: List<PrivacyAlert>,
    val usageEventCount: Int,
    val networkEventCount: Int,
    val inventoryEventCount: Int,
    val topApps: List<String>,
)

