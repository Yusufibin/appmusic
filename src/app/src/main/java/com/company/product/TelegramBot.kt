package com.company.product

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Closeable
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class TelegramException(val code: Int, val retryAfterSeconds: Int? = null) : IOException() {
    val fatal: Boolean get() = code in setOf(400, 401, 403, 409)
    val userMessage: String get() = when (code) {
        400 -> "Configuration refusée par Telegram. Vérifiez le bot."
        401 -> "Jeton invalide ou révoqué. Modifiez la configuration."
        403 -> "Accès refusé. Ouvrez une conversation avec votre bot."
        409 -> "Ce bot est déjà utilisé par une autre session ou un webhook."
        429 -> "Limite Telegram atteinte. Nouvelle tentative automatique."
        else -> "Telegram est inaccessible. Vérifiez la connexion Internet."
    }
}

data class TelegramMessage(
    val chatId: Long, val senderId: Long, val chatType: String, val senderIsBot: Boolean,
    val dateSeconds: Long, val text: String?
)
data class TelegramUpdate(val id: Long, val message: TelegramMessage?)

object TelegramProtocol {
    fun updates(result: JSONArray): List<TelegramUpdate> = (0 until result.length()).mapNotNull { index ->
        val item = result.optJSONObject(index) ?: return@mapNotNull null
        if (!item.has("update_id")) return@mapNotNull null
        val message = item.optJSONObject("message")
        val chat = message?.optJSONObject("chat")
        val sender = message?.optJSONObject("from")
        TelegramUpdate(item.getLong("update_id"), if (message != null && chat != null && sender != null) {
            TelegramMessage(chat.optLong("id"), sender.optLong("id"), chat.optString("type"),
                sender.optBoolean("is_bot", true), message.optLong("date"),
                if (message.isNull("text")) null else message.optString("text"))
        } else null)
    }
}

/** One client per visible session; close() also interrupts the active long poll. */
class TelegramBot(
    private val token: String,
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) : Closeable {
    private val connections = Collections.newSetFromMap(ConcurrentHashMap<HttpURLConnection, Boolean>())
    @Volatile private var closed = false

    init { require(BotConfiguration.validToken(token)) }

    fun username(): String = decode {
        val bot = request("getMe", JSONObject()).getJSONObject("result")
        if (!bot.optBoolean("is_bot")) throw TelegramException(400)
        bot.getString("username")
    }

    fun getUpdates(offset: Long, timeout: Int = 25): List<TelegramUpdate> = decode { TelegramProtocol.updates(
        request("getUpdates", JSONObject().put("offset", offset).put("timeout", timeout)
            .put("limit", 100).put("allowed_updates", JSONArray().put("message"))).getJSONArray("result")
    ) }

    private inline fun <T> decode(block: () -> T): T = try { block() }
        catch (_: JSONException) { throw TelegramException(0) }

    fun sendMessage(chatId: Long, text: String) {
        request("sendMessage", JSONObject().put("chat_id", chatId).put("text", text.take(4_000)))
    }

    private fun request(method: String, body: JSONObject): JSONObject {
        if (closed) throw TelegramException(0)
        val connection = try {
            openConnection(URL("https://api.telegram.org/bot$token/$method"))
        } catch (_: Exception) { throw TelegramException(0) }
        connections.add(connection)
        try {
            if (closed) throw TelegramException(0)
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 35_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val buffer = CharArray(4096)
                val output = StringBuilder()
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    if (output.length + count > 1_048_576) throw TelegramException(0)
                    output.append(buffer, 0, count)
                }
                output.toString()
            } ?: throw TelegramException(status)
            val json = try { JSONObject(raw) } catch (_: Exception) { throw TelegramException(status) }
            if (status !in 200..299 || !json.optBoolean("ok")) {
                throw TelegramException(json.optInt("error_code", status),
                    json.optJSONObject("parameters")?.optInt("retry_after")?.coerceAtLeast(0))
            }
            return json
        } catch (error: TelegramException) {
            throw error
        } catch (_: Exception) {
            // Exceptions from networking can contain the URL, including the bot token.
            throw TelegramException(0)
        } finally {
            connections.remove(connection)
            connection.disconnect()
        }
    }

    override fun close() {
        closed = true
        connections.forEach { it.disconnect() }
        connections.clear()
    }
}
