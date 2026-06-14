package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.PrivacyEvent
import com.cyxwatch.app.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

private class FakePermissionStateProvider(
    private var hasUsageAccess: Boolean,
) : UsagePermissionStateProvider {
    var wasChecked = false

    override fun hasUsageAccess(): Boolean {
        wasChecked = true
        return hasUsageAccess
    }
}

private class TrackingUsageEventCollector(
    private val events: List<PrivacyEvent>,
) : UsageEventCollector {
    var wasCalled = false

    override fun collectUsageEvents(): List<PrivacyEvent> {
        wasCalled = true
        return events
    }
}

private fun buildPrivacyEvent(id: String): PrivacyEvent {
    return PrivacyEvent(
        eventId = id,
        timestamp = Instant.parse("2026-06-13T10:00:00Z"),
        packageName = "com.example.app",
        eventType = EventType.APP_FOREGROUND,
        severity = Severity.LOW,
        source = "Usage",
        title = "sample event",
        explanation = "Sample",
        evidenceJson = "{}",
    )
}

class CollectUsageEventsUseCaseTest {
    @Test
    fun `returns permission missing when usage access denied`() {
        val permissionProvider = FakePermissionStateProvider(hasUsageAccess = false)
        val collector = TrackingUsageEventCollector(listOf(buildPrivacyEvent("1")))
        val useCase = CollectUsageEventsUseCase(
            permissionStateProvider = permissionProvider,
            usageEventCollector = collector,
        )

        val result = useCase.collectLast24hUsageEvents()

        assertEquals(true, permissionProvider.wasChecked)
        assertEquals(false, collector.wasCalled)
        assertEquals(UsageCollectionResult.PermissionMissing, result)
    }

    @Test
    fun `collects events when usage access granted`() {
        val permissionProvider = FakePermissionStateProvider(hasUsageAccess = true)
        val expectedEvents = listOf(buildPrivacyEvent("1"), buildPrivacyEvent("2"))
        val collector = TrackingUsageEventCollector(expectedEvents)
        val useCase = CollectUsageEventsUseCase(
            permissionStateProvider = permissionProvider,
            usageEventCollector = collector,
        )

        val result = useCase.collectLast24hUsageEvents()

        assertEquals(true, permissionProvider.wasChecked)
        assertEquals(true, collector.wasCalled)
        assertEquals(UsageCollectionResult.Success(expectedEvents), result)
    }
}

