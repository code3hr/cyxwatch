package com.cyxwatch.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class EncryptedPreferencesStore(
    context: Context,
    preferenceFileName: String,
) {
    private val encryptedPreferences: SharedPreferences
    private val legacyPreferences: SharedPreferences = context.getSharedPreferences(
        preferenceFileName,
        Context.MODE_PRIVATE,
    )
    private val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    init {
        encryptedPreferences = EncryptedSharedPreferences.create(
            preferenceFileName,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun readBoolean(
        key: String,
        defaultValue: Boolean,
    ): Boolean {
        if (encryptedPreferences.contains(key)) {
            return encryptedPreferences.getBoolean(key, defaultValue)
        }

        val legacyValue = if (legacyPreferences.contains(key)) {
            legacyPreferences.getBoolean(key, defaultValue)
        } else {
            defaultValue
        }

        if (legacyPreferences.contains(key)) {
            migrateLegacyBoolean(key, legacyValue)
        }

        return legacyValue
    }

    fun readInt(key: String, defaultValue: Int): Int {
        if (encryptedPreferences.contains(key)) {
            return encryptedPreferences.getInt(key, defaultValue)
        }

        val legacyValue = if (legacyPreferences.contains(key)) {
            legacyPreferences.getInt(key, defaultValue)
        } else {
            defaultValue
        }

        if (legacyPreferences.contains(key)) {
            migrateLegacyInt(key, legacyValue)
        }

        return legacyValue
    }

    fun readLong(key: String, defaultValue: Long): Long {
        if (encryptedPreferences.contains(key)) {
            return encryptedPreferences.getLong(key, defaultValue)
        }

        val legacyValue = if (legacyPreferences.contains(key)) {
            legacyPreferences.getLong(key, defaultValue)
        } else {
            defaultValue
        }

        if (legacyPreferences.contains(key)) {
            migrateLegacyLong(key, legacyValue)
        }

        return legacyValue
    }

    fun readString(key: String, defaultValue: String): String {
        if (encryptedPreferences.contains(key)) {
            return encryptedPreferences.getString(key, defaultValue) ?: defaultValue
        }

        val legacyValue = if (legacyPreferences.contains(key)) {
            legacyPreferences.getString(key, defaultValue) ?: defaultValue
        } else {
            defaultValue
        }

        if (legacyPreferences.contains(key)) {
            migrateLegacyString(key, legacyValue)
        }

        return legacyValue
    }

    fun writeBoolean(key: String, value: Boolean) {
        encryptedPreferences.edit()
            .putBoolean(key, value)
            .apply()
        clearLegacyValue(key)
    }

    fun writeInt(key: String, value: Int) {
        encryptedPreferences.edit()
            .putInt(key, value)
            .apply()
        clearLegacyValue(key)
    }

    fun writeLong(key: String, value: Long) {
        encryptedPreferences.edit()
            .putLong(key, value)
            .apply()
        clearLegacyValue(key)
    }

    fun writeString(key: String, value: String) {
        encryptedPreferences.edit()
            .putString(key, value)
            .apply()
        clearLegacyValue(key)
    }

    private fun migrateLegacyBoolean(key: String, value: Boolean) {
        encryptedPreferences.edit()
            .putBoolean(key, value)
            .apply()
        clearLegacyValue(key)
    }

    private fun migrateLegacyInt(key: String, value: Int) {
        encryptedPreferences.edit()
            .putInt(key, value)
            .apply()
        clearLegacyValue(key)
    }

    private fun migrateLegacyLong(key: String, value: Long) {
        encryptedPreferences.edit()
            .putLong(key, value)
            .apply()
        clearLegacyValue(key)
    }

    private fun migrateLegacyString(key: String, value: String) {
        encryptedPreferences.edit()
            .putString(key, value)
            .apply()
        clearLegacyValue(key)
    }

    private fun clearLegacyValue(key: String) {
        legacyPreferences.edit()
            .remove(key)
            .apply()
    }
}
