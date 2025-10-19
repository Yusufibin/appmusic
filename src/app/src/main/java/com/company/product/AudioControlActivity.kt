package com.company.product

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

class AudioControlActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager

    // UI Components
    private lateinit var statusIndicator: View
    private lateinit var statusText: TextView
    private lateinit var musicSlider: Slider
    private lateinit var chatSlider: Slider
    private lateinit var systemSlider: Slider
    private lateinit var musicVolumeText: TextView
    private lateinit var chatVolumeText: TextView
    private lateinit var systemVolumeText: TextView
    private lateinit var presetSoloButton: MaterialButton
    private lateinit var presetMultiButton: MaterialButton
    private lateinit var presetMusicButton: MaterialButton
    private lateinit var undoButton: MaterialButton
    private lateinit var autoDuckSwitch: SwitchMaterial
    private lateinit var fab: ExtendedFloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_control)

        // Initialize AudioManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize UI components
        initializeViews()

        // Setup listeners
        setupSliders()
        setupPresets()
        setupActions()

        // Load current volumes
        loadCurrentVolumes()

        // Check service status
        updateServiceStatus()
    }

    private fun initializeViews() {
        statusIndicator = findViewById(R.id.statusIndicator)
        statusText = findViewById(R.id.statusText)

        musicSlider = findViewById(R.id.musicSlider)
        chatSlider = findViewById(R.id.chatSlider)
        systemSlider = findViewById(R.id.systemSlider)

        musicVolumeText = findViewById(R.id.musicVolumeText)
        chatVolumeText = findViewById(R.id.chatVolumeText)
        systemVolumeText = findViewById(R.id.systemVolumeText)

        presetSoloButton = findViewById(R.id.presetSoloButton)
        presetMultiButton = findViewById(R.id.presetMultiButton)
        presetMusicButton = findViewById(R.id.presetMusicButton)

        undoButton = findViewById(R.id.undoButton)
        autoDuckSwitch = findViewById(R.id.autoDuckSwitch)
        fab = findViewById(R.id.fab)
    }

    private fun setupSliders() {
        // Music Slider
        musicSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                val volume = (value * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                musicVolumeText.text = "${value.toInt()}%"

                // Broadcast to service for logging
                broadcastVolumeChange("music", value.toInt())
            }
        }

        musicSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                Toast.makeText(this@AudioControlActivity, "Musique: ${slider.value.toInt()}%", Toast.LENGTH_SHORT).show()
            }
        })

        // Chat Slider
        chatSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                val volume = (value * audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) / 100).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volume, 0)
                chatVolumeText.text = "${value.toInt()}%"

                broadcastVolumeChange("chat", value.toInt())
            }
        }

        chatSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                Toast.makeText(this@AudioControlActivity, "Chat: ${slider.value.toInt()}%", Toast.LENGTH_SHORT).show()
            }
        })

        // System Slider
        systemSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                val volume = (value * audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM) / 100).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, volume, 0)
                systemVolumeText.text = "${value.toInt()}%"

                broadcastVolumeChange("system", value.toInt())
            }
        }

        systemSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                Toast.makeText(this@AudioControlActivity, "Système: ${slider.value.toInt()}%", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupPresets() {
        presetSoloButton.setOnClickListener {
            applyPreset(80, 20, 50, "Jeu Solo")
        }

        presetMultiButton.setOnClickListener {
            applyPreset(40, 100, 50, "Multi avec Chat")
        }

        presetMusicButton.setOnClickListener {
            applyPreset(100, 50, 50, "Musique Priorité")
        }
    }

    private fun setupActions() {
        undoButton.setOnClickListener {
            // Send broadcast to service to undo
            val intent = Intent("com.company.product.ACTION_UNDO")
            sendBroadcast(intent)
            Toast.makeText(this, "Annulation de la dernière action...", Toast.LENGTH_SHORT).show()
        }

        autoDuckSwitch.setOnCheckedChangeListener { _, isChecked ->
            val intent = Intent("com.company.product.ACTION_TOGGLE_AUTO_DUCK")
            intent.putExtra("enabled", isChecked)
            sendBroadcast(intent)

            val status = if (isChecked) "activé" else "désactivé"
            Toast.makeText(this, "Auto-Duck $status", Toast.LENGTH_SHORT).show()
        }

        fab.setOnClickListener {
            // Minimize to background
            moveTaskToBack(true)
            Toast.makeText(this, "App minimisée. Contrôles disponibles dans la notification", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyPreset(musicPercent: Int, chatPercent: Int, systemPercent: Int, presetName: String) {
        // Update sliders
        musicSlider.value = musicPercent.toFloat()
        chatSlider.value = chatPercent.toFloat()
        systemSlider.value = systemPercent.toFloat()

        // Update volumes
        val musicVolume = (musicPercent * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100)
        val chatVolume = (chatPercent * audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) / 100)
        val systemVolume = (systemPercent * audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM) / 100)

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, chatVolume, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, systemVolume, 0)

        // Update text
        musicVolumeText.text = "$musicPercent%"
        chatVolumeText.text = "$chatPercent%"
        systemVolumeText.text = "$systemPercent%"

        // Broadcast to service
        val intent = Intent("com.company.product.ACTION_PRESET_APPLIED")
        intent.putExtra("music", musicPercent)
        intent.putExtra("chat", chatPercent)
        intent.putExtra("system", systemPercent)
        intent.putExtra("name", presetName)
        sendBroadcast(intent)

        Toast.makeText(this, "✓ Preset \"$presetName\" appliqué", Toast.LENGTH_SHORT).show()
    }

    private fun loadCurrentVolumes() {
        // Get current volumes from AudioManager
        val musicMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val chatMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
        val systemMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)

        val musicCurrent = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val chatCurrent = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        val systemCurrent = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)

        val musicPercent = if (musicMax > 0) (musicCurrent * 100 / musicMax) else 50
        val chatPercent = if (chatMax > 0) (chatCurrent * 100 / chatMax) else 50
        val systemPercent = if (systemMax > 0) (systemCurrent * 100 / systemMax) else 50

        // Update sliders
        musicSlider.value = musicPercent.toFloat()
        chatSlider.value = chatPercent.toFloat()
        systemSlider.value = systemPercent.toFloat()

        // Update text
        musicVolumeText.text = "$musicPercent%"
        chatVolumeText.text = "$chatPercent%"
        systemVolumeText.text = "$systemPercent%"
    }

    private fun updateServiceStatus() {
        // Check if service is running
        val isRunning = isServiceRunning(OverlayService::class.java)

        if (isRunning) {
            statusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt()))
            statusText.text = "Service actif"
            statusText.setTextColor(0xFF4CAF50.toInt())
        } else {
            statusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF44336.toInt()))
            statusText.text = "Service inactif"
            statusText.setTextColor(0xFFF44336.toInt())

            // Offer to start service
            Toast.makeText(this, "Service inactif. Relancez l'application depuis le launcher.", Toast.LENGTH_LONG).show()
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun broadcastVolumeChange(stream: String, value: Int) {
        val intent = Intent("com.company.product.ACTION_VOLUME_CHANGED")
        intent.putExtra("stream", stream)
        intent.putExtra("value", value)
        sendBroadcast(intent)
    }

    override fun onResume() {
        super.onResume()
        loadCurrentVolumes()
        updateServiceStatus()
    }
}
