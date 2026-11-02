package com.company.product

/** Deny by default, including groups, callbacks, other senders and old queued messages. */
object CommandPolicy {
    private val supported = setOf("/start", "/devices", "/ping", "/info", "/help")

    fun authorized(message: TelegramMessage, ownerId: Long, nowSeconds: Long): Boolean =
        ownerId > 0 && message.chatType == "private" && message.chatId == ownerId &&
            message.senderId == ownerId && !message.senderIsBot &&
            message.dateSeconds in (nowSeconds - 120)..(nowSeconds + 30)

    fun command(text: String?, username: String): String? {
        val word = text?.trim()?.split(Regex("\\s+"), limit = 2)?.firstOrNull() ?: return null
        val parts = word.split('@', limit = 2)
        if (parts.size == 2 && !parts[1].equals(username, ignoreCase = true)) return null
        return parts[0].takeIf { it in supported }
    }

    fun reply(command: String, name: String, info: String): String = when (command) {
        "/ping" -> "Pong · $name"
        "/info" -> info
        "/devices" -> "Appareil associé : $name\nUn bot dédié par appareil."
        else -> "$name est connecté.\n\n/ping — Tester la connexion\n/info — Modèle, Android et batterie\n/devices — Appareil associé\n/help — Afficher cette aide"
    }
}

/** Bounded retries avoid battery-draining tight loops on network/API failures. */
object RetryPolicy {
    fun delayMillis(failures: Int, retryAfterSeconds: Int? = null): Long {
        val exponential = 1_000L shl (failures - 1).coerceIn(0, 6)
        return maxOf(exponential, (retryAfterSeconds ?: 0).toLong() * 1_000).coerceIn(1_000, 300_000)
    }
}
