package com.cyxwatch.app.domain

import com.cyxwatch.app.domain.model.PrivacyEvent

sealed interface UsageCollectionResult {
    data class Success(val events: List<PrivacyEvent>) : UsageCollectionResult

    data object PermissionMissing : UsageCollectionResult
}

