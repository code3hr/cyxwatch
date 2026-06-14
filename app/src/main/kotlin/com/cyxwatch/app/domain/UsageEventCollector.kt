package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.PrivacyEvent

fun interface UsageEventCollector {
    fun collectUsageEvents(): List<PrivacyEvent>
}

