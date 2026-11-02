package com.company.product

import org.junit.Assert.*
import org.junit.Test

class CommandPolicyTest {
    private val now = 1_700_000_000L
    private val message = TelegramMessage(42, 42, "private", false, now, "/ping")

    @Test fun `explicit owner can issue a fresh private command`() {
        assertTrue(CommandPolicy.authorized(message, 42, now))
    }
    @Test fun `first stranger cannot claim the device`() {
        assertFalse(CommandPolicy.authorized(message, 0, now))
        assertFalse(CommandPolicy.authorized(message.copy(chatId = 99, senderId = 99), 42, now))
    }
    @Test fun `sender must match even when chat matches`() {
        assertFalse(CommandPolicy.authorized(message.copy(senderId = 99), 42, now))
    }
    @Test fun `groups and bots are rejected`() {
        assertFalse(CommandPolicy.authorized(message.copy(chatType = "supergroup"), 42, now))
        assertFalse(CommandPolicy.authorized(message.copy(senderIsBot = true), 42, now))
    }
    @Test fun `stale and future messages are rejected`() {
        assertFalse(CommandPolicy.authorized(message.copy(dateSeconds = now - 121), 42, now))
        assertFalse(CommandPolicy.authorized(message.copy(dateSeconds = now + 31), 42, now))
        assertTrue(CommandPolicy.authorized(message.copy(dateSeconds = now - 120), 42, now))
    }
    @Test fun `telegram command addressing is respected`() {
        assertEquals("/ping", CommandPolicy.command("  /ping@MY_BOT  ", "my_bot"))
        assertNull(CommandPolicy.command("/ping@other_bot", "my_bot"))
        assertNull(CommandPolicy.command("/ping@", "my_bot"))
    }
    @Test fun `unsupported and collection commands cannot be routed`() {
        listOf("/screen", "/record 10", "/sms", "/contacts", "/file /sdcard/file", "/ls", "/pingevil", "hello", "")
            .forEach { assertNull(it, CommandPolicy.command(it, "my_bot")) }
        assertNull(CommandPolicy.command(null, "my_bot"))
    }
    @Test fun `diagnostic commands produce bounded useful replies`() {
        assertEquals("Pong · Pixel", CommandPolicy.reply("/ping", "Pixel", "diagnostic"))
        assertEquals("diagnostic", CommandPolicy.reply("/info", "Pixel", "diagnostic"))
        assertTrue(CommandPolicy.reply("/help", "Pixel", "diagnostic").contains("/info"))
    }
}
