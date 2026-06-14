package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.AppInventoryChange
import com.cyxwatch.app.domain.model.AppProfile
import org.junit.Assert.assertEquals
import org.junit.Test

private fun fakeProfile(
    packageName: String,
    permissions: List<String> = emptyList(),
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

class DetectAppInventoryChangesUseCaseTest {
    @Test
    fun `detects new installed app`() {
        val current = listOf(fakeProfile("com.example.new", permissions = listOf("android.permission.INTERNET")))
        val previous = emptyList<AppProfile>()
        val useCase = DetectAppInventoryChangesUseCase()

        val result = useCase.detectChanges(current, previous)

        assertEquals(1, result.size)
        assertEquals("com.example.new", (result[0] as AppInventoryChange.NewInstall).packageName)
    }

    @Test
    fun `detects permission deltas`() {
        val current = listOf(
            fakeProfile("com.example.mail", permissions = listOf("android.permission.INTERNET", "android.permission.CAMERA")),
            fakeProfile("com.example.news", permissions = listOf("android.permission.ACCESS_FINE_LOCATION")),
        )
        val previous = listOf(
            fakeProfile("com.example.mail", permissions = listOf("android.permission.INTERNET")),
            fakeProfile("com.example.news", permissions = listOf("android.permission.ACCESS_FINE_LOCATION")),
        )
        val useCase = DetectAppInventoryChangesUseCase()

        val result = useCase.detectChanges(current, previous)

        assertEquals(1, result.size)
        val change = result[0] as AppInventoryChange.PermissionDelta
        assertEquals("com.example.mail", change.packageName)
        assertEquals(listOf("android.permission.CAMERA"), change.addedPermissions)
        assertEquals(emptyList<String>(), change.removedPermissions)
    }
}

