package com.cyxwatch.app.domain.model

data class AppProfile(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val firstInstallTimeEpochMs: Long,
    val lastUpdateTimeEpochMs: Long,
    val isLaunchable: Boolean,
    val permissions: List<String>,
)

