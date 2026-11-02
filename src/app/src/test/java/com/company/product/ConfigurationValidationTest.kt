package com.company.product

import org.junit.Assert.*
import org.junit.Test

class ConfigurationValidationTest {
    private val token = "123456789:" + "a".repeat(35)
    @Test fun `valid locally supplied settings are accepted`() {
        assertTrue(BotConfiguration(token, 42, "Mon téléphone").isValid())
    }
    @Test fun `token cannot inject a URL path or query`() {
        listOf("", "invalid", "$token/other", "$token?foo=bar", "https://host/$token", "$token\n")
            .forEach { assertFalse(BotConfiguration.validToken(it)) }
    }
    @Test fun `owner must be a positive personal id`() {
        listOf("0", "-10042", "user", "9223372036854775808").forEach { assertFalse(BotConfiguration.validOwner(it)) }
        assertTrue(BotConfiguration.validOwner("1234567890"))
    }
    @Test fun `names cannot be empty oversized or multiline`() {
        listOf("", " ", "a".repeat(49), "name\nline", "name\tline").forEach {
            assertFalse(BotConfiguration(token, 42, it).isValid())
        }
    }
    @Test fun `backoff grows and obeys telegram flood control`() {
        assertEquals(1_000, RetryPolicy.delayMillis(1))
        assertEquals(2_000, RetryPolicy.delayMillis(2))
        assertEquals(60_000, RetryPolicy.delayMillis(1, 60))
        assertEquals(64_000, RetryPolicy.delayMillis(100))
    }
    @Test fun `backoff cannot overflow or sleep unboundedly`() {
        assertEquals(300_000, RetryPolicy.delayMillis(10, Int.MAX_VALUE))
        assertEquals(1_000, RetryPolicy.delayMillis(0, -1))
    }
}
