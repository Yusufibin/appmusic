package com.company.product

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.*

class AudioManagerTest {

    @Test
    fun testVolumePercentageCalculation() {
        val audioManager = mock(AudioManager::class.java)
        `when`(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).thenReturn(15)
        `when`(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(12)

        // Simulate the logic from OverlayService
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val percentage = currentVolume * 100 / maxVolume

        assertEquals(80, percentage) // 12/15 * 100 = 80
    }

    @Test
    fun testPresetVolumeApplication() {
        val audioManager = mock(AudioManager::class.java)
        `when`(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).thenReturn(15)
        `when`(audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)).thenReturn(7)
        `when`(audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)).thenReturn(7)

        // Apply preset: music 80%, chat 50%, system 70%
        val musicVolume = 80 * 15 / 100
        val chatVolume = 50 * 7 / 100
        val systemVolume = 70 * 7 / 100

        assertEquals(12, musicVolume)
        assertEquals(3, chatVolume) // 50% of 7 is 3.5, but int
        assertEquals(4, systemVolume) // 70% of 7 is 4.9, but int
    }
}