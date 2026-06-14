package com.cyxwatch.app.domain.model

import java.time.Instant

data class PrivacyEvent(
    val eventId: String,
    val timestamp: Instant,
    val packageName: String,
    val eventType: EventType,
    val severity: Severity,
    val source: String,
    val title: String,
    val explanation: String,
    val evidenceJson: String,
)

enum class EventType {
    APP_FOREGROUND,
    APP_BACKGROUND,
    SCREEN_STATE,
    NETWORK_USAGE,
    PERMISSION_CHANGED,
}

enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
}
