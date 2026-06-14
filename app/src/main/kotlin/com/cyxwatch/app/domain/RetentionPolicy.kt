package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.PrivacyEvent
import java.time.Instant

private const val DEFAULT_RETENTION_DAYS = 14
private val ALLOWED_RETENTION_DAYS = setOf(7, 14, 30)

data class RetentionSettings(
    val retentionDays: Int = DEFAULT_RETENTION_DAYS,
) {
    init {
        require(retentionDays in ALLOWED_RETENTION_DAYS) {
            "retentionDays must be one of $ALLOWED_RETENTION_DAYS"
        }
    }
}

class RetentionPolicy {
    fun allowedRetentionDays(): List<Int> {
        return ALLOWED_RETENTION_DAYS.sorted()
    }

    fun normalizeRetentionDays(days: Int): Int {
        return if (days in ALLOWED_RETENTION_DAYS) {
            days
        } else {
            DEFAULT_RETENTION_DAYS
        }
    }

    fun pruneEvents(
        events: List<PrivacyEvent>,
        retentionDays: Int,
        now: Instant,
    ): List<PrivacyEvent> {
        val normalizedDays = normalizeRetentionDays(retentionDays)
        val cutoff = now.minusSeconds(normalizedDays * SECONDS_PER_DAY)
        return events
            .filter { event -> !event.timestamp.isBefore(cutoff) }
            .sortedBy { it.timestamp }
    }

    companion object {
        private const val SECONDS_PER_DAY = 24L * 60L * 60L
    }
}

