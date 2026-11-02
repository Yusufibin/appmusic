package com.company.product

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionPhase { STOPPED, CONNECTING, CONNECTED, RETRYING, ERROR }

data class ConnectionState(
    val phase: ConnectionPhase = ConnectionPhase.STOPPED,
    val botUsername: String? = null,
    val lastSyncMillis: Long? = null,
    val commandsHandled: Int = 0,
    val problem: String? = null
) {
    val running: Boolean get() = phase in setOf(ConnectionPhase.CONNECTING, ConnectionPhase.CONNECTED, ConnectionPhase.RETRYING)
}

object SessionState {
    internal val mutable = MutableStateFlow(ConnectionState())
    val state = mutable.asStateFlow()
}
