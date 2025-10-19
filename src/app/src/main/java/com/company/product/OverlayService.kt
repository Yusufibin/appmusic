package com.company.product

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStats
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.content.pm.ServiceInfo
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.room.Room
import androidx.appcompat.widget.SwitchCompat
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var audioManager: AudioManager
    private lateinit var database: AudioDatabase
    private val gson = Gson()
    private lateinit var floatingButton: View
    private lateinit var overlayView: View
    private var isOverlayVisible = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private val handler = Handler(Looper.getMainLooper())
    private var lastForegroundApp: String? = null
    private var audioRecord: AudioRecord? = null
    private var isDucked = false
    private var originalMusicVolume = 0
    private var isDebugMode = false

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "audio_overlay_channel",
                "Audio Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AudioOverlay Mix est actif"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // Intent to open AudioControlActivity
        val openIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            Intent(this, AudioControlActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for Solo preset
        val soloIntent = android.app.PendingIntent.getBroadcast(
            this,
            1,
            Intent("com.company.product.ACTION_PRESET_SOLO"),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for Multi preset
        val multiIntent = android.app.PendingIntent.getBroadcast(
            this,
            2,
            Intent("com.company.product.ACTION_PRESET_MULTI"),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for Music preset
        val musicIntent = android.app.PendingIntent.getBroadcast(
            this,
            3,
            Intent("com.company.product.ACTION_PRESET_MUSIC"),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "audio_overlay_channel")
                .setContentTitle("🎵 AudioOverlay Mix")
                .setContentText("Tapez pour ouvrir les contrôles")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(openIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_play, "Solo", soloIntent)
                .addAction(android.R.drawable.ic_menu_call, "Multi", multiIntent)
                .addAction(android.R.drawable.ic_media_ff, "Music", musicIntent)
                .setStyle(Notification.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2))
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("🎵 AudioOverlay Mix")
                .setContentText("Service actif")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(openIntent)
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("AudioOverlay", "=== Service onCreate() START ===")
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            android.util.Log.d("AudioOverlay", "WindowManager initialized")

            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            android.util.Log.d("AudioOverlay", "AudioManager initialized")

            database = Room.databaseBuilder(
                applicationContext,
                AudioDatabase::class.java, "audio_database"
            ).build()
            android.util.Log.d("AudioOverlay", "Database initialized")

            createNotificationChannel()
            android.util.Log.d("AudioOverlay", "Notification channel created")

            // Start foreground with proper type for Android 14+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(1, createNotification())
            }
            android.util.Log.d("AudioOverlay", "Service started in foreground")

            requestAudioFocus()
            android.util.Log.d("AudioOverlay", "Audio focus requested")

            // Register broadcast receiver for notification actions
            registerNotificationReceiver()
            android.util.Log.d("AudioOverlay", "Notification receiver registered")

            createFloatingButton()
            android.util.Log.d("AudioOverlay", "Floating button creation attempted")

            initializeDefaultPresets()
            android.util.Log.d("AudioOverlay", "Default presets initialized")

            startAppDetection()
            android.util.Log.d("AudioOverlay", "App detection started")

            startAutoDuck()
            android.util.Log.d("AudioOverlay", "Auto-duck started")

            android.util.Log.d("AudioOverlay", "=== Service onCreate() SUCCESS ===")
        } catch (e: Exception) {
            android.util.Log.e("AudioOverlay", "=== Service onCreate() FAILED ===", e)
            Toast.makeText(this, "Erreur service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private val notificationReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                "com.company.product.ACTION_PRESET_SOLO" -> {
                    applyPreset(80, 20, 50)
                    Toast.makeText(this@OverlayService, "✓ Preset Jeu Solo", Toast.LENGTH_SHORT).show()
                }
                "com.company.product.ACTION_PRESET_MULTI" -> {
                    applyPreset(40, 100, 50)
                    Toast.makeText(this@OverlayService, "✓ Preset Multi Chat", Toast.LENGTH_SHORT).show()
                }
                "com.company.product.ACTION_PRESET_MUSIC" -> {
                    applyPreset(100, 50, 50)
                    Toast.makeText(this@OverlayService, "✓ Preset Musique", Toast.LENGTH_SHORT).show()
                }
                "com.company.product.ACTION_UNDO" -> {
                    undoLastAction()
                }
                "com.company.product.ACTION_TOGGLE_AUTO_DUCK" -> {
                    val enabled = intent.getBooleanExtra("enabled", true)
                    // Toggle auto-duck (implementation depends on your needs)
                }
            }
        }
    }

    private fun registerNotificationReceiver() {
        val filter = android.content.IntentFilter().apply {
            addAction("com.company.product.ACTION_PRESET_SOLO")
            addAction("com.company.product.ACTION_PRESET_MULTI")
            addAction("com.company.product.ACTION_PRESET_MUSIC")
            addAction("com.company.product.ACTION_UNDO")
            addAction("com.company.product.ACTION_TOGGLE_AUTO_DUCK")
            addAction("com.company.product.ACTION_VOLUME_CHANGED")
            addAction("com.company.product.ACTION_PRESET_APPLIED")
        }
        registerReceiver(notificationReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { }
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun initializeDefaultPresets() {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = database.audioDao()
            val existingPresets = dao.getAllPresets()
            if (existingPresets.isEmpty()) {
                dao.insertPreset(Preset(name = "Jeu Solo", volMusic = 0.8f, volChat = 0.2f, volSystem = 0.5f))
                dao.insertPreset(Preset(name = "Multi avec Chat", volMusic = 0.4f, volChat = 1.0f, volSystem = 0.5f))
                dao.insertPreset(Preset(name = "Musique Priorité", volMusic = 1.0f, volChat = 0.5f, volSystem = 0.5f))
            }
        }
    }

    private fun startAppDetection() {
        val appDetectionRunnable = object : Runnable {
            override fun run() {
                val currentApp = getForegroundApp()
                if (currentApp != lastForegroundApp) {
                    lastForegroundApp = currentApp
                    applyPresetForApp(currentApp)
                }
                handler.postDelayed(this, 2000) // Check every 2 seconds
            }
        }
        handler.post(appDetectionRunnable)
    }

    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)
        if (stats != null) {
            var recentStats: UsageStats? = null
            for (usageStats in stats) {
                if (recentStats == null || usageStats.lastTimeUsed > recentStats.lastTimeUsed) {
                    recentStats = usageStats
                }
            }
            return recentStats?.packageName
        }
        return null
    }

    private fun applyPresetForApp(packageName: String?) {
        if (packageName == null) return
        CoroutineScope(Dispatchers.IO).launch {
            val dao = database.audioDao()
            val presets = dao.getAllPresets()
            val matchingPreset = presets.find { it.appTarget == packageName }
            matchingPreset?.let {
                val musicPercent = (it.volMusic * 100).toInt()
                val chatPercent = (it.volChat * 100).toInt()
                val systemPercent = (it.volSystem * 100).toInt()
                applyPreset(musicPercent, chatPercent, systemPercent)
            }
        }
    }

    private fun startAutoDuck() {
        val bufferSize = AudioRecord.getMinBufferSize(8000, android.media.AudioFormat.CHANNEL_IN_MONO, android.media.AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 8000, android.media.AudioFormat.CHANNEL_IN_MONO, android.media.AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        audioRecord?.startRecording()

        val audioBuffer = ShortArray(bufferSize)
        val autoDuckRunnable = object : Runnable {
            override fun run() {
                audioRecord?.let { record ->
                    val readSize = record.read(audioBuffer, 0, bufferSize)
                    if (readSize > 0) {
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            sum += abs(audioBuffer[i].toDouble())
                        }
                        val amplitude = sum / readSize
                        if (amplitude > 500) { // Threshold for voice detection
                            if (!isDucked) {
                                duckMusic()
                            }
                        } else {
                            if (isDucked) {
                                restoreMusic()
                            }
                        }
                    }
                }
                handler.postDelayed(this, 500) // Check every 500ms
            }
        }
        handler.post(autoDuckRunnable)
    }

    private fun duckMusic() {
        originalMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val duckedVolume = originalMusicVolume / 2
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, duckedVolume, 0)
        isDucked = true
    }

    private fun restoreMusic() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
        isDucked = false
    }

    private fun createFloatingButton() {
        android.util.Log.d("AudioOverlay", "createFloatingButton() START")
        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 100
            }
            android.util.Log.d("AudioOverlay", "WindowManager params created")

            floatingButton = LayoutInflater.from(this).inflate(R.layout.floating_button, null)
            android.util.Log.d("AudioOverlay", "Floating button layout inflated")

            val button = floatingButton.findViewById<Button>(R.id.floating_button)
            android.util.Log.d("AudioOverlay", "Button view found")

            button.setOnClickListener {
                android.util.Log.d("AudioOverlay", "Floating button clicked!")
                toggleOverlay()
            }

        button.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingButton, params)
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    // Snap to edges
                    val screenWidth = resources.displayMetrics.widthPixels
                    params.x = if (params.x < screenWidth / 2) 0 else screenWidth - floatingButton.width
                    windowManager.updateViewLayout(floatingButton, params)
                    true
                }
                else -> false
            }
        }

            windowManager.addView(floatingButton, params)
            android.util.Log.d("AudioOverlay", "✓ Floating button ADDED TO WINDOW MANAGER ✓")
            Toast.makeText(this, "Bouton flottant ajouté !", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("AudioOverlay", "✗ FAILED to create floating button ✗", e)
            Toast.makeText(this, "ERREUR bouton: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleOverlay() {
        if (isOverlayVisible) {
            hideOverlay()
        } else {
            showOverlay()
        }
    }

    private fun showOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        // Initialize seekbars
        val musicSeekBar = overlayView.findViewById<SeekBar>(R.id.music_seekbar)
        val chatSeekBar = overlayView.findViewById<SeekBar>(R.id.chat_seekbar)
        val systemSeekBar = overlayView.findViewById<SeekBar>(R.id.system_seekbar)

        // Set initial values
        musicSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        chatSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) * 100 / audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
        systemSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM) * 100 / audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)

        // Set listeners
        musicSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            private var prevProgress = musicSeekBar.progress
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val volume = progress * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                prevProgress = musicSeekBar.progress
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Toast.makeText(this@OverlayService, "Music volume: ${musicSeekBar.progress}%", Toast.LENGTH_SHORT).show()
                logAction("volume_changed", mapOf("volume" to musicSeekBar.progress), mapOf("volume" to prevProgress))
            }
        })

        chatSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            private var prevProgress = chatSeekBar.progress
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val volume = progress * audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) / 100
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volume, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                prevProgress = chatSeekBar.progress
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Toast.makeText(this@OverlayService, "Chat volume: ${chatSeekBar.progress}%", Toast.LENGTH_SHORT).show()
                logAction("volume_changed", mapOf("volume" to chatSeekBar.progress), mapOf("volume" to prevProgress))
            }
        })

        systemSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            private var prevProgress = systemSeekBar.progress
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val volume = progress * audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM) / 100
                    audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, volume, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                prevProgress = systemSeekBar.progress
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Toast.makeText(this@OverlayService, "System volume: ${systemSeekBar.progress}%", Toast.LENGTH_SHORT).show()
                logAction("volume_changed", mapOf("volume" to systemSeekBar.progress), mapOf("volume" to prevProgress))
            }
        })

        // Preset buttons
        overlayView.findViewById<Button>(R.id.preset_solo).setOnClickListener { applyPreset(80, 20, 50) }
        overlayView.findViewById<Button>(R.id.preset_multi).setOnClickListener { applyPreset(40, 100, 50) }
        overlayView.findViewById<Button>(R.id.preset_music).setOnClickListener { applyPreset(100, 50, 50) }

        // Undo button
        overlayView.findViewById<Button>(R.id.undo_button).setOnClickListener { undoLastAction() }

        // Debug switch
        val debugSwitch = overlayView.findViewById<SwitchCompat>(R.id.debug_switch)
        debugSwitch.isChecked = isDebugMode
        debugSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            isDebugMode = isChecked
            android.util.Log.d("AudioOverlay", "Debug mode: $isDebugMode")
        }

        windowManager.addView(overlayView, params)
        isOverlayVisible = true
    }

    private fun applyPreset(musicPercent: Int, chatPercent: Int, systemPercent: Int) {
        // Capture previous volumes
        val prevMusic = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val prevChat = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) * 100 / audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
        val prevSystem = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM) * 100 / audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)

        val musicVolume = musicPercent * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100
        val chatVolume = chatPercent * audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) / 100
        val systemVolume = systemPercent * audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM) / 100

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, chatVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, systemVolume, 0)

        // Update seekbars if overlay is visible
        if (isOverlayVisible) {
            overlayView.findViewById<SeekBar>(R.id.music_seekbar).progress = musicPercent
            overlayView.findViewById<SeekBar>(R.id.chat_seekbar).progress = chatPercent
            overlayView.findViewById<SeekBar>(R.id.system_seekbar).progress = systemPercent
        }

        // Show toast
        Toast.makeText(this, "Preset applied", Toast.LENGTH_SHORT).show()

        // Log the action
        logAction("preset_applied", mapOf("music" to musicPercent, "chat" to chatPercent, "system" to systemPercent), mapOf("music" to prevMusic, "chat" to prevChat, "system" to prevSystem))
    }

    private fun logAction(action: String, values: Map<String, Int>, prevValues: Map<String, Int>? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val logEntry = LogEntry(
                timestamp = System.currentTimeMillis(),
                action = action,
                values = gson.toJson(values),
                prevValues = prevValues?.let { gson.toJson(it) }
            )
            database.audioDao().insertLog(logEntry)
        }
    }

    private fun undoLastAction() {
        CoroutineScope(Dispatchers.IO).launch {
            val recentLogs = database.audioDao().getRecentLogs()
            if (recentLogs.isNotEmpty()) {
                val lastLog = recentLogs.first()
                lastLog.prevValues?.let { prevJson ->
                    val prevValues = gson.fromJson(prevJson, Map::class.java) as Map<String, Int>
                    when (lastLog.action) {
                        "volume_changed" -> {
                            val stream = when (gson.fromJson(lastLog.values, Map::class.java)["stream"]) {
                                "music" -> AudioManager.STREAM_MUSIC
                                "chat" -> AudioManager.STREAM_VOICE_CALL
                                "system" -> AudioManager.STREAM_SYSTEM
                                else -> AudioManager.STREAM_MUSIC
                            }
                            val volume = prevValues["volume"]!! * audioManager.getStreamMaxVolume(stream) / 100
                            audioManager.setStreamVolume(stream, volume, 0)
                            // Update seekbar if visible
                            if (isOverlayVisible) {
                                val seekBarId = when (stream) {
                                    AudioManager.STREAM_MUSIC -> R.id.music_seekbar
                                    AudioManager.STREAM_VOICE_CALL -> R.id.chat_seekbar
                                    AudioManager.STREAM_SYSTEM -> R.id.system_seekbar
                                    else -> R.id.music_seekbar
                                }
                                overlayView.findViewById<SeekBar>(seekBarId).progress = prevValues["volume"]!!
                            }
                        }
                        "preset_applied" -> {
                            val musicVolume = prevValues["music"]!! * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100
                            val chatVolume = prevValues["chat"]!! * audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) / 100
                            val systemVolume = prevValues["system"]!! * audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM) / 100
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicVolume, 0)
                            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, chatVolume, 0)
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, systemVolume, 0)
                            // Update seekbars
                            if (isOverlayVisible) {
                                overlayView.findViewById<SeekBar>(R.id.music_seekbar).progress = prevValues["music"]!!
                                overlayView.findViewById<SeekBar>(R.id.chat_seekbar).progress = prevValues["chat"]!!
                                overlayView.findViewById<SeekBar>(R.id.system_seekbar).progress = prevValues["system"]!!
                            }
                        }
                    }
                    database.audioDao().deleteLog(lastLog.id)
                    Toast.makeText(this@OverlayService, "Action undone", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(this@OverlayService, "Cannot undo this action", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@OverlayService, "No actions to undo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideOverlay() {
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
            isOverlayVisible = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecord?.stop()
        audioRecord?.release()
        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            android.util.Log.e("AudioOverlay", "Error unregistering receiver", e)
        }
        if (::floatingButton.isInitialized) {
            windowManager.removeView(floatingButton)
        }
        if (::overlayView.isInitialized && isOverlayVisible) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
