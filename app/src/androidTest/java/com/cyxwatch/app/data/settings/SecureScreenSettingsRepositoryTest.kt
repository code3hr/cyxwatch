package com.cyxwatch.app.data.settings

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureScreenSettingsRepositoryTest {
    private val preferenceName = "cyxwatch_secure_screen_settings"

    @Test
    fun `readState returns defaults for empty state`() {
        val context = context()
        context.deleteSharedPreferences(preferenceName)

        val repository = SecureScreenSettingsRepository(context)
        val state = repository.readState()

        assertFalse(state.isEnabled)
    }

    @Test
    fun `setEnabled persists secure screen mode state`() {
        val context = context()
        context.deleteSharedPreferences(preferenceName)

        val repository = SecureScreenSettingsRepository(context)
        repository.setEnabled(true)

        val state = repository.readState()

        assertTrue(state.isEnabled)
    }

    private fun context(): Context {
        return InstrumentationRegistry.getInstrumentation().targetContext
    }
}
