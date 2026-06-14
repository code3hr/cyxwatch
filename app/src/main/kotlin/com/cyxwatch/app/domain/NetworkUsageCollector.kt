package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.NetworkUsageSummary

interface NetworkUsageCollector {
    fun collectNetworkUsageSummary(): NetworkUsageSummary
}

