package com.cyxwatch.app.domain

import java.time.Duration
import java.time.Instant

data class CollectionThrottleDecision(
    val allowed: Boolean,
    val retryAfterSeconds: Long,
)

class CollectionThrottle(
    private val minimumInterval: Duration = Duration.ofSeconds(8),
) {
    private val lastCollectedAt = mutableMapOf<String, Instant>()

    fun tryCollect(actionKey: String, now: Instant = Instant.now()): CollectionThrottleDecision {
        val last = lastCollectedAt[actionKey]
        if (last != null) {
            val nextAllowedAt = last.plus(minimumInterval)
            if (now.isBefore(nextAllowedAt)) {
                return CollectionThrottleDecision(
                    allowed = false,
                    retryAfterSeconds = Duration.between(now, nextAllowedAt).seconds,
                )
            }
        }

        lastCollectedAt[actionKey] = now
        return CollectionThrottleDecision(
            allowed = true,
            retryAfterSeconds = 0,
        )
    }
}
