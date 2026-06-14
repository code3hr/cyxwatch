package com.cyxwatch.app.domain.model

data class NetworkUsageDailyTotals(
    val dayStartEpochMs: Long,
    val totalBytes: Long,
    val totalBytesByNetworkType: Map<String, Long>,
    val totalBytesByPackageName: Map<String, Long>,
    val persistedAtEpochMs: Long,
)

