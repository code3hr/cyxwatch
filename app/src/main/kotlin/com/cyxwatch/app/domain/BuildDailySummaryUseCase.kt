package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.PrivacyEvent
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class BuildDailySummaryUseCase(
    private val windowDuration: Duration = Duration.ofHours(24),
) {
    fun build(
        score: PrivacyScore,
        events: List<PrivacyEvent>,
        alerts: List<PrivacyAlert>,
        now: Instant = Instant.now(),
    ): DailySummary {
        val windowEnd = now
        val windowStart = now.minus(windowDuration)
        val windowedEvents = events.filter { event ->
            !event.timestamp.isBefore(windowStart) && !event.timestamp.isAfter(windowEnd)
        }
        val windowedEventIds = windowedEvents.map { it.eventId }.toSet()
        val windowedAlerts = alerts.filter { alert ->
            !alert.triggeredAt.isBefore(windowStart) && !alert.triggeredAt.isAfter(windowEnd)
        }
        val windowedReasons = score.reasons.filter { reason ->
            reason.evidenceEventIds.any { eventId ->
                windowedEventIds.contains(eventId)
            }
        }

        return DailySummary(
            dateLabel = formatDateLabel(now),
            generatedAt = now,
            windowStart = windowStart,
            windowEnd = windowEnd,
            score = score.score,
            topReasons = windowedReasons,
            topAlertCount = windowedAlerts.size,
            recentAlerts = windowedAlerts,
            usageEventCount = windowedEvents.count { event ->
                when (event.eventType) {
                    EventType.APP_FOREGROUND,
                    EventType.APP_BACKGROUND,
                    EventType.SCREEN_STATE -> true

                    EventType.NETWORK_USAGE,
                    EventType.PERMISSION_CHANGED -> false
                }
            },
            networkEventCount = windowedEvents.count { it.eventType == EventType.NETWORK_USAGE },
            inventoryEventCount = windowedEvents.count { it.eventType == EventType.PERMISSION_CHANGED },
            topApps = windowedEvents
                .groupBy { it.packageName }
                .filter { it.key.isNotBlank() }
                .toList()
                .sortedWith(
                    compareByDescending<Pair<String, List<PrivacyEvent>>> { (_, packageEvents) -> packageEvents.size }
                        .thenBy { (packageName, _) -> packageName },
                )
                .take(5)
                .map { it.first },
        )
    }

    private fun formatDateLabel(now: Instant): String {
        return DateTimeFormatter
            .ofPattern("MMM d, yyyy", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(now)
    }
}
