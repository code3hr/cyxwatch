package com.cyxwatch.app.platform.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.cyxwatch.app.domain.UsageEventCollector
import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import java.time.Instant

class AndroidUsageEventCollector(
    private val context: Context,
) : UsageEventCollector {
    override fun collectUsageEvents(): List<PrivacyEvent> {
        val now = System.currentTimeMillis()
        val windowStart = now - MILLISECONDS_IN_24_HOURS
        return collectUsageEventsBetween(windowStart, now)
    }

    internal fun collectUsageEventsBetween(startEpochMs: Long, endEpochMs: Long): List<PrivacyEvent> {
        if (endEpochMs <= startEpochMs) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = try {
            usageStatsManager.queryEvents(startEpochMs, endEpochMs)
        } catch (_: SecurityException) {
            return emptyList()
        }

        val event = UsageEvents.Event()
        val result = mutableListOf<PrivacyEvent>()
        var screenState = "screen_unknown"
        var lockState = "lock_unknown"
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    result += buildTransitionEvent(
                        event = event,
                        stateLabel = "foreground",
                        eventType = EventType.APP_FOREGROUND,
                        severity = Severity.LOW,
                        source = "UsageStats",
                        evidenceSource = "foreground_transition",
                        screenState = screenState,
                        lockState = lockState,
                    )
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    result += buildTransitionEvent(
                        event = event,
                        stateLabel = "background",
                        eventType = EventType.APP_BACKGROUND,
                        severity = Severity.LOW,
                        source = "UsageStats",
                        evidenceSource = "background_transition",
                        screenState = screenState,
                        lockState = lockState,
                    )
                }
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    screenState = "screen_on"
                    result += buildTransitionEvent(
                        event = event,
                        stateLabel = "screen_on",
                        eventType = EventType.SCREEN_STATE,
                        severity = Severity.LOW,
                        source = "ScreenState",
                        evidenceSource = "screen_interactive",
                    )
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    screenState = "screen_off"
                    result += buildTransitionEvent(
                        event = event,
                        stateLabel = "screen_off",
                        eventType = EventType.SCREEN_STATE,
                        severity = Severity.MEDIUM,
                        source = "ScreenState",
                        evidenceSource = "screen_non_interactive",
                    )
                }
                UsageEvents.Event.KEYGUARD_SHOWN -> {
                    lockState = "keyguard_shown"
                    result += buildTransitionEvent(
                        event = event,
                        stateLabel = "keyguard_shown",
                        eventType = EventType.SCREEN_STATE,
                        severity = Severity.MEDIUM,
                        source = "KeyguardState",
                        evidenceSource = "keyguard_shown",
                    )
                }
                UsageEvents.Event.KEYGUARD_HIDDEN -> {
                    lockState = "keyguard_hidden"
                    result += buildTransitionEvent(
                        event = event,
                        stateLabel = "keyguard_hidden",
                        eventType = EventType.SCREEN_STATE,
                        severity = Severity.LOW,
                        source = "KeyguardState",
                        evidenceSource = "keyguard_hidden",
                    )
                }
            }
        }

        return result
            .sortedBy { it.timestamp }
            .let(UsageEventNormalizer::normalize)
    }

    private fun buildTransitionEvent(
        event: UsageEvents.Event,
        stateLabel: String,
        eventType: EventType,
        severity: Severity,
        source: String,
        evidenceSource: String,
        screenState: String? = null,
        lockState: String? = null,
    ): PrivacyEvent {
        val timestamp = Instant.ofEpochMilli(event.timeStamp)
        val packageName = if (event.packageName.isBlank()) {
            "system"
        } else {
            event.packageName
        }
        val packageInText = event.packageName.ifBlank { source }

        return PrivacyEvent(
            eventId = eventKey(packageName, event.timeStamp, eventType, event.eventType),
            timestamp = timestamp,
            packageName = packageName,
            eventType = eventType,
            severity = severity,
            source = source,
            title = "${stateLabel.replace("_", " ").replaceFirstChar { it.uppercase() }} transition",
            explanation = buildString {
                append("$packageInText in $stateLabel state.")
                if (screenState != null && lockState != null) {
                    append(" Screen state: $screenState; lock state: $lockState.")
                }
            },
            evidenceJson = buildString {
                append("{")
                append("\"source\":\"$evidenceSource\"")
                append(",\"packageName\":\"")
                append(event.packageName.jsonEscape())
                append("\"")
                append(",\"className\":\"")
                append(event.className.jsonEscape())
                append("\"")
                append(",\"state\":\"$stateLabel\"")
                append(",\"rawEventType\":")
                append(event.eventType)
                if (screenState != null) {
                    append(",\"screenState\":\"")
                    append(screenState.jsonEscape())
                    append("\"")
                }
                if (lockState != null) {
                    append(",\"lockState\":\"")
                    append(lockState.jsonEscape())
                    append("\"")
                }
                append("}")
            },
        )
    }

    private fun eventKey(
        packageName: String,
        timestamp: Long,
        eventType: EventType,
        rawEventType: Int,
    ): String {
        return "usage-$packageName-$timestamp-${eventType.name}-$rawEventType"
    }

    companion object {
        private const val MILLISECONDS_IN_24_HOURS = 24L * 60L * 60L * 1000L
    }
}

private fun String.jsonEscape(): String {
    return buildString {
        this@jsonEscape.forEach { ch ->
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch < ' ') {
                        append(String.format("\\u%04x", ch.code))
                    } else {
                        append(ch)
                    }
                }
            }
        }
    }
}
