package com.company.product

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.ProgressBar
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)

        updateStatus("Vérification des permissions...")
        checkPermissions()
    }

    private fun updateStatus(message: String) {
        statusText.text = message
    }

    private fun checkPermissions() {
        // Check audio recording permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            updateStatus("Permission d'enregistrement audio requise")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Check notification permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                updateStatus("Permission de notification requise")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
                return
            }
        }

        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            updateStatus("Permission d'affichage par-dessus requise")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            return
        }

        // Check usage stats permission
        if (!hasUsageStatsPermission()) {
            updateStatus("Permission de statistiques d'utilisation requise")
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivityForResult(intent, USAGE_STATS_PERMISSION_REQUEST_CODE)
            return
        }

        // All permissions granted, start overlay service
        updateStatus("Toutes les permissions accordées ! Démarrage du service...")
        progressBar.visibility = View.VISIBLE
        startOverlayService()
        // Attendre un peu avant de fermer pour s'assurer que le service démarre
        handler.postDelayed({
            finish() // Close main activity
        }, 1000)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun startOverlayService() {
        try {
            val intent = Intent(this, OverlayService::class.java)
            startService(intent)
            updateStatus("Service démarré avec succès !")
            Toast.makeText(this, "AudioOverlay Mix est maintenant actif", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            updateStatus("Erreur : ${e.message}")
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Erreur au démarrage du service: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            // Ne pas fermer l'activité en cas d'erreur pour permettre le débogage
            return
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            AUDIO_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateStatus("Permission audio accordée ✓")
                    handler.postDelayed({ checkPermissions() }, 300) // Continue checking other permissions
                } else {
                    updateStatus("Permission audio refusée ✗")
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "La permission d'enregistrement audio est requise", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateStatus("Permission notification accordée ✓")
                    handler.postDelayed({ checkPermissions() }, 300) // Continue checking other permissions
                } else {
                    updateStatus("Permission notification refusée (optionnelle)")
                    Toast.makeText(this, "La permission de notification est recommandée", Toast.LENGTH_SHORT).show()
                    handler.postDelayed({ checkPermissions() }, 300) // Continue anyway for notifications
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE || requestCode == USAGE_STATS_PERMISSION_REQUEST_CODE) {
            updateStatus("Vérification des permissions...")
            handler.postDelayed({ checkPermissions() }, 300) // Re-check after permission grant
        }
    }

    companion object {
        const val AUDIO_PERMISSION_REQUEST_CODE = 1000
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1003
        const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
        const val USAGE_STATS_PERMISSION_REQUEST_CODE = 1002
    }
}
