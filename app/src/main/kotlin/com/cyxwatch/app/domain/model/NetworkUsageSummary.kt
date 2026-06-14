package com.cyxwatch.app.domain.model

data class NetworkUsageSummary(
    val windowStartEpochMs: Long,
    val windowEndEpochMs: Long,
    val totalBytes: Long,
    val totalBytesByNetworkType: Map<String, Long>,
    val totalBytesByPackageName: Map<String, Long>,
    val events: List<PrivacyEvent>,
)

