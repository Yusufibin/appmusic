package com.company.product

import org.junit.Assert.assertEquals
import org.junit.Test

class PresetTest {

    @Test
    fun testPresetCreation() {
        val preset = Preset(
            name = "Test Preset",
            volMusic = 0.8f,
            volChat = 0.5f,
            volSystem = 0.7f,
            appTarget = "com.test.app"
        )

        assertEquals("Test Preset", preset.name)
        assertEquals(0.8f, preset.volMusic)
        assertEquals(0.5f, preset.volChat)
        assertEquals(0.7f, preset.volSystem)
        assertEquals("com.test.app", preset.appTarget)
    }

    @Test
    fun testPresetDefaultValues() {
        val preset = Preset(
            name = "Default Preset",
            volMusic = 1.0f,
            volChat = 1.0f,
            volSystem = 1.0f
        )

        assertEquals(null, preset.appTarget)
        assertEquals(0L, preset.id)
    }
}