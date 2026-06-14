package com.cyxwatch.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class CollectionThrottleTest {
    private val now = Instant.parse("2026-06-14T12:00:00Z")
    private val throttle = CollectionThrottle(
        minimumInterval = Duration.ofSeconds(5),
    )

    @Test
    fun `allows first collection and blocks rapid repeat`() {
        val first = throttle.tryCollect("usage", now)
        val second = throttle.tryCollect("usage", now.plusSeconds(2))

        assertEquals(true, first.allowed)
        assertEquals(0, first.retryAfterSeconds)
        assertEquals(false, second.allowed)
        assertEquals(3, second.retryAfterSeconds)
    }

    @Test
    fun `allows collection after cooldown elapses`() {
        throttle.tryCollect("network", now)
        val later = throttle.tryCollect("network", now.plusSeconds(5))

        assertEquals(true, later.allowed)
        assertEquals(0, later.retryAfterSeconds)
    }

    @Test
    fun `keeps per-action windows independent`() {
        val usage = throttle.tryCollect("usage", now)
        val network = throttle.tryCollect("network", now.plusSeconds(2))

        assertEquals(true, usage.allowed)
        assertEquals(true, network.allowed)
    }
}
