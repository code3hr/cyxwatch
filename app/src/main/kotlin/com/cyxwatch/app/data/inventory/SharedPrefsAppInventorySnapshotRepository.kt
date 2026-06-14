package com.cyxwatch.app.data.inventory

import android.content.Context
import com.cyxwatch.app.domain.model.AppProfile
import java.nio.charset.StandardCharsets
import java.util.Base64

private const val SNAPSHOT_PREFERENCES_FILE = "cyxwatch_app_inventory"
private const val SNAPSHOT_KEY = "installed_app_profiles"
private const val FIELD_SEPARATOR = "\u001F"
private const val PERMISSION_SEPARATOR = "\u001E"

class SharedPrefsAppInventorySnapshotRepository(
    context: Context,
) : AppInventorySnapshotRepository {
    private val sharedPreferences = context.getSharedPreferences(
        SNAPSHOT_PREFERENCES_FILE,
        Context.MODE_PRIVATE,
    )

    override fun readSnapshot(): List<AppProfile> {
        val serializedProfiles = sharedPreferences.getString(SNAPSHOT_KEY, "") ?: return emptyList()
        if (serializedProfiles.isBlank()) return emptyList()

        return serializedProfiles
            .split('\n')
            .asSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { parseProfileLine(it) }
            .toList()
    }

    override fun writeSnapshot(profiles: List<AppProfile>) {
        val payload = profiles
            .sortedBy { it.packageName }
            .joinToString(separator = "\n") { it.toStorageLine() }
        sharedPreferences.edit()
            .putString(SNAPSHOT_KEY, payload)
            .apply()
    }

    private fun AppProfile.toStorageLine(): String {
        return buildString {
            append(encodeField(packageName))
            append(FIELD_SEPARATOR)
            append(encodeField(label))
            append(FIELD_SEPARATOR)
            append(encodeField(versionName ?: ""))
            append(FIELD_SEPARATOR)
            append(versionCode)
            append(FIELD_SEPARATOR)
            append(firstInstallTimeEpochMs)
            append(FIELD_SEPARATOR)
            append(lastUpdateTimeEpochMs)
            append(FIELD_SEPARATOR)
            append(isLaunchable)
            append(FIELD_SEPARATOR)
            append(
                permissions
                    .sorted()
                    .joinToString(separator = PERMISSION_SEPARATOR) { permission ->
                        encodeField(permission)
                    },
            )
        }
    }

    private fun parseProfileLine(line: String): AppProfile? {
        val parts = line.split(FIELD_SEPARATOR)
        if (parts.size != 8) return null
        val isLaunchable = parts[6].lowercase() == "true"

        return AppProfile(
            packageName = decodeField(parts[0]),
            label = decodeField(parts[1]),
            versionName = decodeField(parts[2]).ifEmpty { null },
            versionCode = parts[3].toLongOrNull() ?: return null,
            firstInstallTimeEpochMs = parts[4].toLongOrNull() ?: return null,
            lastUpdateTimeEpochMs = parts[5].toLongOrNull() ?: return null,
            isLaunchable = isLaunchable,
            permissions = if (parts[7].isNotEmpty()) {
                parts[7]
                    .split(PERMISSION_SEPARATOR)
                    .map { decodeField(it) }
                    .sorted()
            } else {
                emptyList()
            },
        )
    }

    private fun encodeField(value: String): String {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeField(value: String): String {
        return String(
            Base64.getUrlDecoder().decode(value),
            StandardCharsets.UTF_8,
        )
    }
}
