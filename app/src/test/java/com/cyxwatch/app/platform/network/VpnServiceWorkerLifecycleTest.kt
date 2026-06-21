package com.cyxwatch.app.platform.network

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnServiceWorkerLifecycleTest {
    private fun buildSleepingWorker(name: String, started: CountDownLatch): Thread {
        return Thread(
            {
                started.countDown()
                try {
                    Thread.sleep(1_000L)
                } catch (_: InterruptedException) {
                    // expected during shutdown in some tests
                }
            },
            name,
        )
    }

    @Test
    fun `tryStart returns one worker when already idle`() {
        val lifecycle = VpnServiceWorkerLifecycle()
        val started = CountDownLatch(1)
        val firstWorker = lifecycle.tryStart { buildSleepingWorker("first-worker", started) }!!
        firstWorker.start()
        started.await(1, TimeUnit.SECONDS)

        assertEquals("first-worker", firstWorker.name)
        assertTrue(lifecycle.isActive())
    }

    @Test
    fun `second start is rejected while first worker is alive`() {
        val lifecycle = VpnServiceWorkerLifecycle()
        val started = CountDownLatch(1)
        val firstWorker = lifecycle.tryStart { buildSleepingWorker("first-worker", started) }!!
        firstWorker.start()
        started.await(1, TimeUnit.SECONDS)

        val secondWorker = lifecycle.tryStart { Thread("second-worker") }

        assertNull(secondWorker)
        assertEquals("first-worker", firstWorker.name)
        assertTrue(lifecycle.isActive())

        firstWorker.interrupt()
        firstWorker.join(1_000L)
    }

    @Test
    fun `stopActiveWorker clears lifecycle state`() {
        val lifecycle = VpnServiceWorkerLifecycle()
        val started = CountDownLatch(1)
        val firstWorker = lifecycle.tryStart { buildSleepingWorker("first-worker", started) }!!
        firstWorker.start()
        started.await(1, TimeUnit.SECONDS)

        val stoppedWorker = lifecycle.stopActiveWorker()

        assertEquals("first-worker", stoppedWorker?.name)
        assertFalse(lifecycle.isActive())

        val nextWorker = lifecycle.tryStart { Thread("next-worker") }!!
        assertEquals("next-worker", nextWorker.name)
        assertFalse(lifecycle.isActive())
        nextWorker.start()
        assertTrue(lifecycle.isActive())
        nextWorker.interrupt()
        nextWorker.join(1_000L)
    }
}
