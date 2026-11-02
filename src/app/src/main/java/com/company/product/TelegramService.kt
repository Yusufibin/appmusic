package com.company.product

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class TelegramService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null
    @Volatile private var client: TelegramBot? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || intent?.action != ACTION_START) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (pollingJob?.isActive == true) return START_NOT_STICKY
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), FLAGS)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, TelegramService::class.java).setAction(ACTION_STOP), FLAGS)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_connection)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_body))
            .setContentIntent(open)
            .addAction(R.drawable.ic_stop, getString(R.string.stop), stop)
            .setOngoing(true).setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        SessionState.mutable.value = ConnectionState(ConnectionPhase.CONNECTING)
        pollingJob = scope.launch { poll() }
        return START_NOT_STICKY
    }

    private suspend fun poll() {
        val store = ConfigurationStore(this)
        try {
            val config = store.load() ?: error("No configuration")
            val api = TelegramBot(config.token).also { client = it }
            var username: String? = null
            var offset = store.nextUpdateId
            var failures = 0
            UiLogger.log("SESSION", "Connexion demandée depuis cet appareil.")
            while (scope.isActive) {
                try {
                    if (username == null) {
                        username = api.username()
                        SessionState.mutable.update { it.copy(botUsername = username) }
                    }
                    // On a new association, discard any old bot backlog before accepting commands.
                    if (offset == null) {
                        offset = (api.getUpdates(-1, 0).maxOfOrNull { it.id } ?: -1L) + 1
                        store.nextUpdateId = offset
                    }
                    if (SessionState.state.value.phase == ConnectionPhase.CONNECTING) {
                        SessionState.mutable.update {
                            it.copy(phase = ConnectionPhase.CONNECTED, lastSyncMillis = System.currentTimeMillis())
                        }
                        UiLogger.log("SESSION", "Bot vérifié, session connectée.")
                    }
                    val updates = api.getUpdates(offset)
                    scope.ensureActive()
                    SessionState.mutable.update {
                        it.copy(phase = ConnectionPhase.CONNECTED, problem = null, lastSyncMillis = System.currentTimeMillis())
                    }
                    if (failures > 0) UiLogger.log("RÉSEAU", "Connexion rétablie.")
                    failures = 0
                    for (update in updates) {
                        scope.ensureActive()
                        val message = update.message
                        if (message != null && CommandPolicy.authorized(message, config.ownerId, System.currentTimeMillis() / 1_000)) {
                            val command = CommandPolicy.command(message.text, username)
                            if (command != null) {
                                val reply = CommandPolicy.reply(command, config.deviceName, DeviceManager(this).summary(config.deviceName))
                                api.sendMessage(config.ownerId, reply)
                                scope.ensureActive()
                                SessionState.mutable.update { it.copy(commandsHandled = it.commandsHandled + 1) }
                                UiLogger.log("COMMANDE", "$command traitée.")
                            }
                        }
                        // Advance only after handling: a failed response remains retryable.
                        offset = update.id + 1
                        store.nextUpdateId = offset
                    }
                    // Even an unexpectedly empty immediate response cannot create a busy loop.
                    delay(250)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: TelegramException) {
                    scope.ensureActive()
                    if (error.fatal) {
                        fail(error.userMessage)
                        return
                    }
                    failures = (failures + 1).coerceAtMost(10)
                    SessionState.mutable.update { it.copy(phase = ConnectionPhase.RETRYING, problem = error.userMessage) }
                    if (failures == 1) UiLogger.log("RÉSEAU", error.userMessage)
                    delay(RetryPolicy.delayMillis(failures, error.retryAfterSeconds) + Random.nextLong(250))
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            if (scope.isActive) fail(getString(R.string.configuration_unreadable))
        } finally {
            client?.close()
            stopSelf()
        }
    }

    private fun fail(message: String) {
        SessionState.mutable.update { it.copy(phase = ConnectionPhase.ERROR, problem = message) }
        UiLogger.log("ERREUR", message)
    }

    override fun onDestroy() {
        scope.cancel()
        client?.close()
        pollingJob = null
        SessionState.mutable.update {
            if (it.phase == ConnectionPhase.ERROR) it else it.copy(phase = ConnectionPhase.STOPPED, problem = null)
        }
        UiLogger.log("SESSION", "Session terminée.")
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.company.product.START_SESSION"
        const val ACTION_STOP = "com.company.product.STOP_SESSION"
        private const val CHANNEL = "appmusic_session"
        private const val NOTIFICATION_ID = 420
        private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    }
}
