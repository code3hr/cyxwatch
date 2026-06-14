package com.cyxwatch.app.platform.usage

import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import java.time.Instant
import kotlin.math.abs

internal object UsageEventNormalizer {
    private const val DUPLICATE_WINDOW_MS = 1000L
    private const val GAP_BACKFILL_MIN_MS = 10_000L
    private const val GAP_BACKFILL_MAX_MS = 3L * 60L * 1000L

    fun normalize(events: List<PrivacyEvent>): List<PrivacyEvent> {
        return backfillShortGaps(dedupeNearDuplicates(events))
    }

    fun dedupeNearDuplicates(events: List<PrivacyEvent>): List<PrivacyEvent> {
        if (events.isEmpty()) return events

        val collapsed = mutableListOf<PrivacyEvent>()
        for (event in events.sortedBy { it.timestamp }) {
            val previous = collapsed.lastOrNull()
            if (previous != null && isLikelyDuplicate(previous, event)) {
                continue
            }
            collapsed.add(event)
        }

        return collapsed.distinctBy { it.eventId }
    }

    fun backfillShortGaps(events: List<PrivacyEvent>): List<PrivacyEvent> {
        if (events.size < 2) {
            return events
        }

        val withBackfills = mutableListOf<PrivacyEvent>()
        val sortedEvents = events.sortedBy { it.timestamp }
        sortedEvents.forEachIndexed { index, currentEvent ->
            val previousEvent = if (index == 0) null else sortedEvents[index - 1]
            if (previousEvent != null) {
                buildGapBackfill(previousEvent, currentEvent)?.let(withBackfills::add)
            }
            withBackfills.add(currentEvent)
        }

        return withBackfills
    }

    private fun isLikelyDuplicate(
        previous: PrivacyEvent,
        current: PrivacyEvent,
    ): Boolean {
        if (previous.packageName != current.packageName) return false
        if (previous.eventType != current.eventType) return false
        if (previous.source != current.source) return false
        val deltaMs = abs(current.timestamp.toEpochMilli() - previous.timestamp.toEpochMilli())
        return deltaMs <= DUPLICATE_WINDOW_MS
    }

    private fun buildGapBackfill(
        previous: PrivacyEvent,
        current: PrivacyEvent,
    ): PrivacyEvent? {
        val previousMs = previous.timestamp.toEpochMilli()
        val currentMs = current.timestamp.toEpochMilli()
        val gapMs = abs(currentMs - previousMs)
        if (gapMs < GAP_BACKFILL_MIN_MS || gapMs > GAP_BACKFILL_MAX_MS) {
            return null
        }

        if (previous.eventType != EventType.APP_FOREGROUND || current.eventType != EventType.APP_FOREGROUND) {
            return null
        }

        if (previous.packageName == current.packageName) {
            return null
        }

        val midTimestamp = previousMs + (gapMs / 2L)
        return PrivacyEvent(
            eventId = eventKey(
                packageName = previous.packageName,
                timestamp = midTimestamp,
                eventType = EventType.APP_BACKGROUND,
                rawEventType = -2,
            ),
            timestamp = Instant.ofEpochMilli(midTimestamp),
            packageName = previous.packageName,
            eventType = EventType.APP_BACKGROUND,
            severity = Severity.LOW,
            source = "UsageStatsBackfill",
            title = "Inferred background transition",
            explanation = "Assuming ${previous.packageName} moved to background during a short query gap.",
            evidenceJson = buildString {
                append("{")
                append("\"source\":\"usage_gap_backfill\",")
                append("\"packageName\":\"")
                append(previous.packageName.jsonEscape())
                append("\",\"fromTimestamp\":")
                append(previousMs)
                append(",\"toTimestamp\":")
                append(currentMs)
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
