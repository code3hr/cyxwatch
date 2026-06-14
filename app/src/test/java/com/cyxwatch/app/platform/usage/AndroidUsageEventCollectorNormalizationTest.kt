package com.cyxwatch.app.platform.usage

import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

private fun sampleEvent(
    packageName: String,
    secondsFromStart: Long,
    eventType: EventType,
    source: String = "UsageStats",
): PrivacyEvent {
    return PrivacyEvent(
        eventId = "sample-$packageName-$secondsFromStart-${eventType.name}",
        timestamp = Instant.ofEpochMilli(secondsFromStart * 1000L),
        packageName = packageName,
        eventType = eventType,
        severity = Severity.LOW,
        source = source,
        title = "sample",
        explanation = "sample",
        evidenceJson = "{}",
    )
}

class AndroidUsageEventCollectorNormalizationTest {
    @Test
    fun `dedupe near duplicates from same package and source`() {
        val result = listOf(
            sampleEvent(packageName = "com.a", secondsFromStart = 0L, eventType = EventType.APP_FOREGROUND),
            sampleEvent(packageName = "com.a", secondsFromStart = 0L, eventType = EventType.APP_FOREGROUND),
            sampleEvent(packageName = "com.a", secondsFromStart = 3L, eventType = EventType.APP_FOREGROUND),
        ).let(UsageEventNormalizer::dedupeNearDuplicates)

        assertEquals(2, result.size)
    }

    @Test
    fun `keeps events outside duplicate window and different sources`() {
        val result = listOf(
            sampleEvent(packageName = "com.a", secondsFromStart = 0L, eventType = EventType.APP_FOREGROUND, source = "UsageStats"),
            sampleEvent(packageName = "com.a", secondsFromStart = 1L, eventType = EventType.APP_FOREGROUND, source = "UsageStatsBackfill"),
            sampleEvent(packageName = "com.a", secondsFromStart = 3L, eventType = EventType.APP_FOREGROUND, source = "UsageStats"),
        ).let(UsageEventNormalizer::dedupeNearDuplicates)

        assertEquals(3, result.size)
    }

    @Test
    fun `inserts background gap backfill for short foreground transition gap`() {
        val events = listOf(
            sampleEvent(packageName = "com.a", secondsFromStart = 0L, eventType = EventType.APP_FOREGROUND),
            sampleEvent(packageName = "com.b", secondsFromStart = 120L, eventType = EventType.APP_FOREGROUND),
        ).let(UsageEventNormalizer::normalize)

        assertEquals(3, events.size)
        assertEquals(EventType.APP_BACKGROUND, events[1].eventType)
        assertEquals("com.a", events[1].packageName)
        assertEquals("UsageStatsBackfill", events[1].source)
    }

    @Test
    fun `does not backfill outside short gap window`() {
        val events = listOf(
            sampleEvent(packageName = "com.a", secondsFromStart = 0L, eventType = EventType.APP_FOREGROUND),
            sampleEvent(packageName = "com.b", secondsFromStart = 1200L, eventType = EventType.APP_FOREGROUND),
        ).let(UsageEventNormalizer::normalize)

        assertEquals(2, events.size)
    }

    @Test
    fun `does not backfill when events are not consecutive foreground transitions`() {
        val events = listOf(
            sampleEvent(packageName = "com.a", secondsFromStart = 0L, eventType = EventType.APP_FOREGROUND),
            sampleEvent(packageName = "com.b", secondsFromStart = 120L, eventType = EventType.SCREEN_STATE),
        ).let(UsageEventNormalizer::normalize)

        assertEquals(2, events.size)
    }
}
