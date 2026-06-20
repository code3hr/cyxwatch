package com.cyxwatch.app

import com.cyxwatch.app.domain.ScoringRule

data class LaunchAction(
    val targetPackageName: String,
    val targetRule: ScoringRule? = null,
)
