package com.cyxwatch.app.domain

import com.cyxwatch.app.data.inventory.AppInventorySnapshotRepository
import com.cyxwatch.app.domain.model.AppProfile
import com.cyxwatch.app.domain.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Test

private fun freshProfile(
    packageName: String,
    permissions: List<String>,
): AppProfile {
    return AppProfile(
        packageName = packageName,
        label = packageName,
        versionName = "1.0.0",
        versionCode = 1L,
        firstInstallTimeEpochMs = 0L,
        lastUpdateTimeEpochMs = 0L,
        isLaunchable = true,
        permissions = permissions,
    )
}

private class FakeAppInventorySnapshotRepository(
    var storedProfiles: List<AppProfile> = emptyList(),
) : AppInventorySnapshotRepository {
    var wasRead = false
    var wasWritten = false
    var lastWrittenProfiles: List<AppProfile> = emptyList()

    override fun readSnapshot(): List<AppProfile> {
        wasRead = true
        return storedProfiles
    }

    override fun writeSnapshot(profiles: List<AppProfile>) {
        wasWritten = true
        lastWrittenProfiles = profiles
        storedProfiles = profiles
    }
}

class RefreshInstalledAppInventoryUseCaseTest {
    @Test
    fun `refreshes inventory snapshot and emits change events from deltas`() {
        val currentProfiles = listOf(
            freshProfile("com.example.mail", permissions = listOf("android.permission.INTERNET", "android.permission.CAMERA")),
        )
        val previousProfiles = listOf(
            freshProfile("com.example.mail", permissions = listOf("android.permission.INTERNET")),
        )
        val readUseCase = ReadInstalledAppInventoryUseCase(
            appInventoryRepository = object : com.cyxwatch.app.data.inventory.AppInventoryRepository {
                override fun fetchInstalledAppProfiles(): List<AppProfile> = currentProfiles
            },
        )
        val snapshotRepository = FakeAppInventorySnapshotRepository(storedProfiles = previousProfiles)
        val useCase = RefreshInstalledAppInventoryUseCase(
            readInstalledAppInventoryUseCase = readUseCase,
            snapshotRepository = snapshotRepository,
            detectAppInventoryChangesUseCase = DetectAppInventoryChangesUseCase(),
            buildInventoryChangeEventsUseCase = BuildInventoryChangeEventsUseCase(),
        )

        val result = useCase.refresh()

        assertEquals(true, snapshotRepository.wasRead)
        assertEquals(true, snapshotRepository.wasWritten)
        assertEquals(1, result.currentProfiles.size)
        assertEquals(1, result.inventoryChanges.size)
        assertEquals(1, result.inventoryEvents.size)
        assertEquals(EventType.PERMISSION_CHANGED, result.inventoryEvents[0].eventType)
        assertEquals(currentProfiles[0].packageName, result.inventoryEvents[0].packageName)
        assertEquals(currentProfiles, snapshotRepository.lastWrittenProfiles)
    }

    @Test
    fun `persists all current profiles even when no inventory changes detected`() {
        val currentProfiles = listOf(
            freshProfile("com.example.news", permissions = listOf("android.permission.INTERNET")),
        )
        val snapshotRepository = FakeAppInventorySnapshotRepository(storedProfiles = currentProfiles)
        val readUseCase = ReadInstalledAppInventoryUseCase(
            appInventoryRepository = object : com.cyxwatch.app.data.inventory.AppInventoryRepository {
                override fun fetchInstalledAppProfiles(): List<AppProfile> = currentProfiles
            },
        )
        val useCase = RefreshInstalledAppInventoryUseCase(
            readInstalledAppInventoryUseCase = readUseCase,
            snapshotRepository = snapshotRepository,
            detectAppInventoryChangesUseCase = DetectAppInventoryChangesUseCase(),
            buildInventoryChangeEventsUseCase = BuildInventoryChangeEventsUseCase(),
        )

        val result = useCase.refresh()

        assertEquals(0, result.inventoryChanges.size)
        assertEquals(0, result.inventoryEvents.size)
        assertEquals(true, snapshotRepository.wasWritten)
        assertEquals(currentProfiles, snapshotRepository.lastWrittenProfiles)
    }
}
