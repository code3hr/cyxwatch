package com.cyxwatch.app

import com.cyxwatch.app.domain.ScoringRule
import com.cyxwatch.app.platform.notifications.EXTRA_ALERT_PACKAGE
import com.cyxwatch.app.platform.notifications.EXTRA_ALERT_RULE
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class LaunchActionParserTest {
    @Test
    fun `parses package and optional rule when valid`() {
        val action = LaunchActionParser.parse(
            mapOf(
                EXTRA_ALERT_PACKAGE to "com.example.news",
                EXTRA_ALERT_RULE to ScoringRule.ScreenOffAppActivity.name,
            ),
        )

        assertEquals(
            LaunchAction(
                targetPackageName = "com.example.news",
                targetRule = ScoringRule.ScreenOffAppActivity,
            ),
            action,
        )
    }

    @Test
    fun `parses package without rule when rule is absent`() {
        val action = LaunchActionParser.parse(
            mapOf(
                EXTRA_ALERT_PACKAGE to "com.example.news",
            ),
        )

        assertEquals(
            LaunchAction(
                targetPackageName = "com.example.news",
                targetRule = null,
            ),
            action,
        )
    }

    @Test
    fun `parses package when package value contains whitespace`() {
        val action = LaunchActionParser.parse(
            mapOf(
                EXTRA_ALERT_PACKAGE to "   com.example.news   ",
            ),
        )

        assertEquals("com.example.news", action?.targetPackageName)
        assertEquals(null, action?.targetRule)
    }

    @Test
    fun `rejects invalid package values`() {
        assertNull(
            LaunchActionParser.parse(
                mapOf(
                    EXTRA_ALERT_PACKAGE to ".com.example",
                ),
            ),
        )
    }

    @Test
    fun `rejects non-string package values`() {
        assertNull(
            LaunchActionParser.parse(
                mapOf(
                    EXTRA_ALERT_PACKAGE to 123,
                ),
            ),
        )
    }

    @Test
    fun `rejects unexpected extras`() {
        assertNull(
            LaunchActionParser.parse(
                mapOf(
                    EXTRA_ALERT_PACKAGE to "com.example.news",
                    "unexpected" to "nope",
                ),
            ),
        )
    }

    @Test
    fun `rejects invalid rule values`() {
        assertNull(
            LaunchActionParser.parse(
                mapOf(
                    EXTRA_ALERT_PACKAGE to "com.example.news",
                    EXTRA_ALERT_RULE to "NotARule",
                ),
            ),
        )
    }

    @Test
    fun `rejects blank rule values`() {
        assertNull(
            LaunchActionParser.parse(
                mapOf(
                    EXTRA_ALERT_PACKAGE to "com.example.news",
                    EXTRA_ALERT_RULE to "   ",
                ),
            ),
        )
    }

    @Test
    fun `rejects non-string rule values`() {
        assertNull(
            LaunchActionParser.parse(
                mapOf(
                    EXTRA_ALERT_PACKAGE to "com.example.news",
                    EXTRA_ALERT_RULE to 42,
                ),
            ),
        )
    }

    @Test
    fun `rejects null rule values when rule key is present`() {
        assertNull(
            LaunchActionParser.parse(
                mapOf(
                    EXTRA_ALERT_PACKAGE to "com.example.news",
                    EXTRA_ALERT_RULE to null,
                ),
            ),
        )
    }

    @Test
    fun `rejects empty extras`() {
        assertNull(LaunchActionParser.parse(emptyMap()))
    }
}
