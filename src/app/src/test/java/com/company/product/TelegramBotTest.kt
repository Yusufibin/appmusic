package com.company.product

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class TelegramBotTest {
    private val token = "123456789:" + "a".repeat(35)
    private class Connection(private val status: Int, private val body: String) : HttpURLConnection(URL("https://example.invalid")) {
        val written = ByteArrayOutputStream()
        var disconnected = false
        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getInputStream() = ByteArrayInputStream(body.toByteArray())
        override fun getErrorStream() = ByteArrayInputStream(body.toByteArray())
        override fun getOutputStream() = written
    }
    @Test fun `bot identity is checked and connection is always closed`() {
        val connection = Connection(200, """{"ok":true,"result":{"is_bot":true,"username":"my_bot"}}""")
        TelegramBot(token) { connection }.use { assertEquals("my_bot", it.username()) }
        assertTrue(connection.disconnected)
        assertFalse(connection.instanceFollowRedirects)
    }
    @Test fun `flood control preserves retry_after without exposing raw API text`() {
        val connection = Connection(429, """{"ok":false,"error_code":429,"description":"private text","parameters":{"retry_after":45}}""")
        try {
            TelegramBot(token) { connection }.getUpdates(0)
            fail("expected TelegramException")
        } catch (error: TelegramException) {
            assertEquals(45, error.retryAfterSeconds)
            assertFalse(error.fatal)
            assertFalse(error.userMessage.contains("private text"))
        }
        assertTrue(connection.disconnected)
    }
    @Test fun `revoked credentials and duplicate pollers are terminal`() {
        listOf(401, 403, 409).forEach { code ->
            val connection = Connection(code, """{"ok":false,"error_code":$code}""")
            try {
                TelegramBot(token) { connection }.username()
                fail("expected failure")
            } catch (error: TelegramException) { assertTrue(error.fatal) }
            assertTrue(connection.disconnected)
        }
    }
    @Test fun `transport errors do not leak the token through exception messages`() {
        val api = TelegramBot(token) { throw IOException("secret URL with $token") }
        // URL creation/open failures are also sanitized by the client.
        try { api.username(); fail("expected failure") }
        catch (error: IOException) { assertFalse(error.message.orEmpty().contains(token)) }
    }
    @Test fun `send message uses utf8 json without interpretive parse modes`() {
        val connection = Connection(200, """{"ok":true,"result":{}}""")
        TelegramBot(token) { connection }.sendMessage(42, "Téléphone <name>")
        val body = JSONObject(connection.written.toString("UTF-8"))
        assertEquals(42L, body.getLong("chat_id"))
        assertEquals("Téléphone <name>", body.getString("text"))
        assertFalse(body.has("parse_mode"))
    }
    @Test fun `long polling only subscribes to messages`() {
        val connection = Connection(200, """{"ok":true,"result":[]}""")
        TelegramBot(token) { connection }.getUpdates(123)
        val body = JSONObject(connection.written.toString("UTF-8"))
        assertEquals(123, body.getInt("offset"))
        assertEquals(25, body.getInt("timeout"))
        assertEquals("message", body.getJSONArray("allowed_updates").getString(0))
    }
    @Test fun `closed client cannot start another connection`() {
        var calls = 0
        val api = TelegramBot(token) { calls++; Connection(200, "{}") }
        api.close()
        try { api.username(); fail("expected failure") } catch (_: TelegramException) { }
        assertEquals(0, calls)
    }
    @Test fun `malformed API responses fail safely and disconnect`() {
        val connection = Connection(502, "<html>gateway failure</html>")
        try { TelegramBot(token) { connection }.username(); fail("expected failure") }
        catch (error: TelegramException) { assertEquals(502, error.code) }
        assertTrue(connection.disconnected)
    }
}
