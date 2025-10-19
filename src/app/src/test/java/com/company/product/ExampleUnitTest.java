package com.company.product;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for AudioOverlay Mix
 */
public class AudioOverlayUnitTest {

    @Test
    public void presetCreation_isCorrect() {
        Preset preset = new Preset("Test", 0.8f, 0.5f, 0.7f, "com.test.app");
        assertEquals("Test", preset.getName());
        assertEquals(0.8f, preset.getVolMusic(), 0.01f);
        assertEquals(0.5f, preset.getVolChat(), 0.01f);
        assertEquals(0.7f, preset.getVolSystem(), 0.01f);
        assertEquals("com.test.app", preset.getAppTarget());
    }

    @Test
    public void logEntryCreation_isCorrect() {
        LogEntry log = new LogEntry(1, 123456789L, "volume_changed", "{\"stream\":\"music\",\"volume\":50}", "{\"volume\":40}");
        assertEquals(1, log.getId());
        assertEquals(123456789L, log.getTimestamp());
        assertEquals("volume_changed", log.getAction());
        assertEquals("{\"stream\":\"music\",\"volume\":50}", log.getValues());
        assertEquals("{\"volume\":40}", log.getPrevValues());
    }
}