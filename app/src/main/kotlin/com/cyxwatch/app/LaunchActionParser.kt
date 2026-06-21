package com.cyxwatch.app

import android.os.Bundle
import com.cyxwatch.app.domain.ScoringRule
import com.cyxwatch.app.platform.notifications.EXTRA_ALERT_PACKAGE
import com.cyxwatch.app.platform.notifications.EXTRA_ALERT_RULE

object LaunchActionParser {
    private val packageNamePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    private val supportedExtraKeys = setOf(
        EXTRA_ALERT_PACKAGE,
        EXTRA_ALERT_RULE,
    )

    fun parse(intentExtras: Bundle?): LaunchAction? {
        val rawExtras = intentExtras ?: return null
        if (rawExtras.keySet().isEmpty()) {
            return null
        }
        if ((rawExtras.keySet() - supportedExtraKeys).isNotEmpty()) {
            return null
        }

        val extras = buildMap {
            put(EXTRA_ALERT_PACKAGE, rawExtras.getString(EXTRA_ALERT_PACKAGE))
            if (rawExtras.containsKey(EXTRA_ALERT_RULE)) {
                put(EXTRA_ALERT_RULE, rawExtras.getString(EXTRA_ALERT_RULE))
            }
        }
        return parse(extras)
    }

    fun parse(extras: Map<String, Any?>): LaunchAction? {
        if (extras.isEmpty()) {
            return null
        }

        if ((extras.keys - supportedExtraKeys).isNotEmpty()) {
            return null
        }

        val packageName = extras[EXTRA_ALERT_PACKAGE]
            ?.let { it as? String }
            ?.trim()
            ?: return null

        if (!packageNamePattern.matches(packageName)) {
            return null
        }

        val hasTargetRule = extras.containsKey(EXTRA_ALERT_RULE)
        val targetRuleValue = if (hasTargetRule) extras[EXTRA_ALERT_RULE] else null
        val targetRule = when {
            !hasTargetRule -> null
            targetRuleValue !is String -> return null
            targetRuleValue.isBlank() -> return null
            else -> runCatching { ScoringRule.valueOf(targetRuleValue.trim()) }
                .getOrNull()
                ?: return null
        }

        return LaunchAction(targetPackageName = packageName, targetRule = targetRule)
    }
}
