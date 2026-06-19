package com.cyxwatch.app.domain

enum class ScoringRule(val delta: Int, val description: String) {
    SensitivePermissionAdded(15, "Sensitive permission added"),
    NewAppWithSensitivePermissions(10, "New app has sensitive permissions"),
    LowBackgroundNetwork(2, "Low background network observed"),
    HighBackgroundNetwork(12, "High background network observed"),
    MediumBackgroundNetwork(6, "Background network observed"),
    ScreenOffAppActivity(8, "App activity while screen was off"),
    KeyguardAppActivity(6, "App activity while device was locked"),
    ;

    fun isSensitivePermissionWarning(): Boolean {
        return this == SensitivePermissionAdded || this == NewAppWithSensitivePermissions
    }

    fun isCriticalWarning(): Boolean {
        return this == SensitivePermissionAdded ||
            this == NewAppWithSensitivePermissions ||
            this == HighBackgroundNetwork
    }

    override fun toString(): String = description
}
