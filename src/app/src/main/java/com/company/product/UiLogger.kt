package com.company.product

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** In-memory, bounded journal. Never store message contents or API URLs here. */
object UiLogger {
    private val mutable = MutableStateFlow("")
    val logs = mutable.asStateFlow()
    private val lines = ArrayDeque<String>()
    private val tokenPattern = Regex("[0-9]{6,16}:[A-Za-z0-9_-]{30,64}")

    @Synchronized
    fun log(tag: String, message: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(Date())
        val safe = tokenPattern.replace(message, "[jeton masqué]").replace('\n', ' ').take(200)
        if (lines.size == 100) lines.removeFirst()
        lines.addLast("$ts  ·  $tag  $safe")
        mutable.value = lines.joinToString("\n")
    }

    @Synchronized
    fun clear() { lines.clear(); mutable.value = "" }
}
