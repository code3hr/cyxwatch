package com.cyxwatch.app.domain

fun interface UsagePermissionStateProvider {
    fun hasUsageAccess(): Boolean
}

