package com.cyxwatch.app.domain

import com.cyxwatch.app.data.inventory.AppInventoryRepository
import com.cyxwatch.app.domain.model.AppProfile
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeAppInventoryRepository(
    private val appProfiles: List<AppProfile>,
) : AppInventoryRepository {
    var wasRead = false

    override fun fetchInstalledAppProfiles(): List<AppProfile> {
        wasRead = true
        return appProfiles
    }
}

private fun fakeProfile(
    packageName: String,
    isLaunchable: Boolean = true,
): AppProfile {
    return AppProfile(
        packageName = packageName,
        label = packageName,
        versionName = "1.0.0",
        versionCode = 1L,
        firstInstallTimeEpochMs = 0L,
        lastUpdateTimeEpochMs = 0L,
        isLaunchable = isLaunchable,
        permissions = listOf("android.permission.INTERNET"),
    )
}

class ReadInstalledAppInventoryUseCaseTest {
    @Test
    fun `loads app profiles from repository`() {
        val repository = FakeAppInventoryRepository(
            listOf(
                fakeProfile("com.example.alpha"),
                fakeProfile("com.example.beta", isLaunchable = false),
            ),
        )
        val useCase = ReadInstalledAppInventoryUseCase(repository)

        val result = useCase.readInstalledAppProfiles()

        assertEquals(true, repository.wasRead)
        assertEquals(2, result.size)
        assertEquals("com.example.alpha", result[0].packageName)
    }
}

