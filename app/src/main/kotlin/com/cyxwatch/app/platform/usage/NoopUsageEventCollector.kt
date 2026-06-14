package com.cyxwatch.app.platform.usage

import com.cyxwatch.app.domain.UsageEventCollector
import com.cyxwatch.app.domain.model.PrivacyEvent

class NoopUsageEventCollector : UsageEventCollector {
    override fun collectUsageEvents(): List<PrivacyEvent> {
        return emptyList()
    }
}

