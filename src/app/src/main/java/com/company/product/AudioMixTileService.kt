package com.company.product

import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AudioMixTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // Open AudioControlActivity when tile is clicked
        val intent = Intent(this, AudioControlActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
    }

    private fun updateTileState() {
        val tile = qsTile ?: return

        // Check if service is running
        val isServiceRunning = isServiceRunning(OverlayService::class.java)

        if (isServiceRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "AudioMix"
            tile.subtitle = "Actif"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "AudioMix"
            tile.subtitle = "Inactif"
        }

        tile.updateTile()
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

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }
}
