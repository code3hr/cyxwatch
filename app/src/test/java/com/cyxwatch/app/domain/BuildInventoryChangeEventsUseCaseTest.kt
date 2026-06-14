package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.AppInventoryChange
import com.cyxwatch.app.domain.model.AppProfile
import com.cyxwatch.app.domain.model.EventType
import com.cyxwatch.app.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

private fun appProfile(
    packageName: String,
    label: String = packageName,
    permissions: List<String> = emptyList(),
    versionName: String? = "1.0.0",
): AppProfile {
    return AppProfile(
        packageName = packageName,
        label = label,
        versionName = versionName,
        versionCode = 1L,
        firstInstallTimeEpochMs = 0L,
        lastUpdateTimeEpochMs = 0L,
        isLaunchable = true,
        permissions = permissions,
    )
}

class BuildInventoryChangeEventsUseCaseTest {
    @Test
    fun `maps new install into permission-changed evidence event`() {
        val changes = listOf(
            AppInventoryChange.NewInstall(
                packageName = "com.example.new",
                installedProfile = appProfile("com.example.new", permissions = listOf("android.permission.INTERNET")),
            ),
        )
        val fixedTime = Instant.parse("2026-06-13T12:00:00Z")
        val useCase = BuildInventoryChangeEventsUseCase()

        val events = useCase.buildEvents(changes, fixedTime)

        assertEquals(1, events.size)
        assertEquals(EventType.PERMISSION_CHANGED, events[0].eventType)
        assertEquals("com.example.new", events[0].packageName)
        assertEquals(Severity.LOW, events[0].severity)
        assertEquals("New app installed", events[0].title)
        assertEquals(fixedTime, events[0].timestamp)
        assertEquals(
            true,
            events[0].evidenceJson.contains("\"kind\":\"new_install\""),
        )
    }

    @Test
    fun `escapes json strings in new install evidence`() {
        val changes = listOf(
            AppInventoryChange.NewInstall(
                packageName = "com.example.new",
                installedProfile = appProfile(
                    packageName = "com.example.new",
                    label = "Photo \"Camera\" App",
                    permissions = listOf("android.permission.INTERNET"),
                    versionName = null,
                ),
            ),
        )
        val fixedTime = Instant.parse("2026-06-13T12:00:00Z")
        val useCase = BuildInventoryChangeEventsUseCase()

        val events = useCase.buildEvents(changes, fixedTime)

        assertEquals(1, events.size)
        val evidence = events[0].evidenceJson
        assertEquals(true, evidence.startsWith("{\"kind\":\"new_install\","))
        assertEquals(true, evidence.contains("\"label\":\"Photo \\\"Camera\\\" App\""))
        assertEquals(true, events[0].evidenceJson.contains("\"versionName\":null"))
        assertEquals(true, events[0].evidenceJson.contains("\"permissions\":[\"android.permission.INTERNET\"]"))
    }

    @Test
    fun `maps permission deltas into evidence event`() {
        val changes = listOf(
            AppInventoryChange.PermissionDelta(
                packageName = "com.example.mail",
                addedPermissions = listOf("android.permission.CAMERA"),
                removedPermissions = listOf("android.permission.READ_CONTACTS"),
            ),
        )
        val fixedTime = Instant.parse("2026-06-13T12:00:00Z")
        val useCase = BuildInventoryChangeEventsUseCase()

        val events = useCase.buildEvents(changes, fixedTime)

        assertEquals(1, events.size)
        assertEquals("AppInventory", events[0].source)
        assertEquals(EventType.PERMISSION_CHANGED, events[0].eventType)
        assertEquals(Severity.MEDIUM, events[0].severity)
        assertEquals(true, events[0].evidenceJson.contains("\"addedPermissions\":[\"android.permission.CAMERA\"]"))
        assertEquals(true, events[0].evidenceJson.contains("\"removedPermissions\":[\"android.permission.READ_CONTACTS\"]"))
    }
}
