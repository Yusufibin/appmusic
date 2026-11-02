package com.company.product

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.company.product.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var store: ConfigurationStore
    private var busy = true
    private var checking = false
    @Volatile private var checkingClient: TelegramBot? = null

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startSession() else {
            busy = false
            render(SessionState.state.value)
            message(getString(R.string.notifications_required))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.navigationBarColor = getColor(R.color.canvas)
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = ConfigurationStore(this)
        binding.etDeviceName.setText(Build.MODEL.take(48))
        binding.etToken.doAfterTextChanged { binding.tokenLayout.error = null }
        binding.etOwnerId.doAfterTextChanged { binding.ownerLayout.error = null }
        binding.etDeviceName.doAfterTextChanged { binding.nameLayout.error = null }
        binding.btnGuide.setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle(R.string.setup_title).setMessage(R.string.setup_body)
                .setPositiveButton(R.string.understood, null).show()
        }
        binding.btnClear.setOnClickListener { UiLogger.clear() }
        binding.btnSave.setOnClickListener { save(start = false) }
        binding.btnTest.setOnClickListener { verifyBot() }
        binding.btnSession.setOnClickListener {
            if (SessionState.state.value.running) stopService(Intent(this, TelegramService::class.java))
            else save(start = true)
        }
        binding.btnForget.setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle(R.string.forget_title).setMessage(R.string.forget_message)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.forget) { _, _ -> forget() }.show()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { SessionState.state.collect { render(it) } }
                launch { UiLogger.logs.collect { logs ->
                    val atBottom = !binding.scrollLog.canScrollVertically(1)
                    binding.tvLog.text = logs.ifEmpty { getString(R.string.empty_journal) }
                    if (atBottom) binding.scrollLog.post { binding.scrollLog.fullScroll(android.view.View.FOCUS_DOWN) }
                } }
            }
        }
        lifecycleScope.launch {
            try {
                val config = withContext(Dispatchers.IO) { store.load() }
                if (config != null) {
                    binding.etToken.setText(config.token)
                    if (savedInstanceState == null) {
                        binding.etOwnerId.setText(config.ownerId.toString())
                        binding.etDeviceName.setText(config.deviceName)
                    }
                }
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) { message(getString(R.string.configuration_unreadable))
            } finally { busy = false; render(SessionState.state.value) }
        }
        render(SessionState.state.value)
    }

    private fun configuration(): BotConfiguration? {
        val token = binding.etToken.text.toString().trim()
        val owner = binding.etOwnerId.text.toString().trim()
        val name = binding.etDeviceName.text.toString().trim()
        binding.tokenLayout.error = if (BotConfiguration.validToken(token)) null else getString(R.string.invalid_token)
        binding.ownerLayout.error = if (BotConfiguration.validOwner(owner)) null else getString(R.string.invalid_owner)
        binding.nameLayout.error = if (name.isNotEmpty() && name.length <= 48 && name.none { it.isISOControl() }) null else getString(R.string.invalid_name)
        val invalid = when {
            binding.nameLayout.error != null -> binding.etDeviceName
            binding.tokenLayout.error != null -> binding.etToken
            binding.ownerLayout.error != null -> binding.etOwnerId
            else -> null
        }
        if (invalid != null) { invalid.requestFocus(); return null }
        return BotConfiguration(token, owner.toLong(), name)
    }

    private fun save(start: Boolean) {
        if (busy || SessionState.state.value.running) return
        val config = configuration() ?: return
        busy = true
        render(SessionState.state.value)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { store.save(config) }
                if (start) {
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else startSession()
                } else message(getString(R.string.saved))
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) { message(getString(R.string.save_error))
            } finally { busy = false; render(SessionState.state.value) }
        }
    }

    private fun startSession() {
        try {
            ContextCompat.startForegroundService(this,
                Intent(this, TelegramService::class.java).setAction(TelegramService.ACTION_START))
        } catch (_: Exception) { message(getString(R.string.start_failed)) }
        busy = false
        render(SessionState.state.value)
    }

    private fun verifyBot() {
        if (busy || SessionState.state.value.running) return
        val config = configuration() ?: return
        busy = true
        checking = true
        render(SessionState.state.value)
        lifecycleScope.launch {
            try {
                val username = withContext(Dispatchers.IO) {
                    TelegramBot(config.token).also { checkingClient = it }.use { it.username() }
                }
                message(getString(R.string.verified, username))
            } catch (error: CancellationException) { throw error
            } catch (error: TelegramException) { message(error.userMessage)
            } catch (_: Exception) { message(TelegramException(0).userMessage)
            } finally {
                checkingClient = null
                checking = false
                busy = false
                render(SessionState.state.value)
            }
        }
    }

    private fun forget() {
        if (busy || SessionState.state.value.running) return
        busy = true
        render(SessionState.state.value)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { store.clear() }
                binding.etToken.text?.clear()
                binding.etOwnerId.text?.clear()
                binding.etDeviceName.setText(Build.MODEL.take(48))
                SessionState.mutable.value = ConnectionState()
                UiLogger.clear()
                message(getString(R.string.forgotten))
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) { message(getString(R.string.save_error))
            } finally { busy = false; render(SessionState.state.value) }
        }
    }

    private fun render(state: ConnectionState) = with(binding) {
        tvStatus.setText(when (state.phase) {
            ConnectionPhase.STOPPED -> R.string.status_stopped
            ConnectionPhase.CONNECTING -> R.string.status_connecting
            ConnectionPhase.CONNECTED -> R.string.status_connected
            ConnectionPhase.RETRYING -> R.string.status_retrying
            ConnectionPhase.ERROR -> R.string.status_error
        })
        tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity,
            if (state.phase == ConnectionPhase.ERROR) R.color.error else R.color.primary))
        connectionProgress.isVisible = state.phase in setOf(ConnectionPhase.CONNECTING, ConnectionPhase.RETRYING)
        tvSessionDetail.text = state.problem ?: when (state.phase) {
            ConnectionPhase.CONNECTING -> getString(R.string.session_connecting)
            ConnectionPhase.CONNECTED -> "@${state.botUsername} · ${getString(R.string.session_connected)}"
            else -> getString(R.string.session_idle)
        }
        tvLastSync.text = state.lastSyncMillis?.let { DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(it)) }
            ?: getString(R.string.never)
        tvCommandCount.text = state.commandsHandled.toString()
        btnSession.setText(if (state.running) R.string.stop else R.string.start)
        btnSession.isEnabled = !busy || state.running
        val editable = !state.running && !busy
        nameLayout.isEnabled = editable
        tokenLayout.isEnabled = editable
        ownerLayout.isEnabled = editable
        btnSave.isEnabled = editable
        btnTest.isEnabled = editable
        btnForget.isEnabled = editable
        btnTest.setText(if (checking) R.string.testing else R.string.test_bot)
        tvConfigurationHint.setText(if (state.running) R.string.configuration_locked else R.string.configuration_description)
    }

    private fun message(text: String) { Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG).setTextMaxLines(5).show() }

    override fun onDestroy() {
        checkingClient?.close()
        super.onDestroy()
    }
}
