package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RetentionPolicyTest {
    private val policy = RetentionPolicy()

    @Test
    fun `keeps events inside selected retention window`() {
        val now = Instant.parse("2026-06-13T12:00:00Z")
        val events = listOf(
            event(id = "old", timestamp = Instant.parse("2026-05-13T12:00:00Z")),
            event(id = "recent", timestamp = Instant.parse("2026-06-10T12:00:00Z")),
        )

        val result = policy.pruneEvents(
            events = events,
            retentionDays = 7,
            now = now,
        )

        assertEquals(listOf("recent"), result.map { it.eventId })
    }

    @Test
    fun `keeps cutoff boundary event`() {
        val now = Instant.parse("2026-06-13T12:00:00Z")
        val events = listOf(
            event(id = "boundary", timestamp = Instant.parse("2026-06-06T12:00:00Z")),
        )

        val result = policy.pruneEvents(
            events = events,
            retentionDays = 7,
            now = now,
        )

        assertEquals(listOf("boundary"), result.map { it.eventId })
    }

    @Test
    fun `normalizes unsupported retention values to default`() {
        assertEquals(14, policy.normalizeRetentionDays(3))
        assertEquals(14, policy.normalizeRetentionDays(365))
    }

    @Test
    fun `returns supported retention values in ascending order`() {
        assertEquals(listOf(7, 14, 30), policy.allowedRetentionDays())
    }

    private fun event(
        id: String,
        timestamp: Instant,
    ): PrivacyEvent {
        return PrivacyEvent(
            eventId = id,
            timestamp = timestamp,
            packageName = "com.example.app",
            eventType = EventType.APP_FOREGROUND,
            severity = Severity.LOW,
            source = "test",
            title = "sample",
            explanation = "sample",
            evidenceJson = "{}",
        )
    }
}

