package com.company.product

import org.junit.Assert.assertEquals
import org.junit.Test

class LogEntryTest {

    @Test
    fun testLogEntryCreation() {
        val timestamp = System.currentTimeMillis()
        val logEntry = LogEntry(
            timestamp = timestamp,
            action = "volume_changed",
            values = "{\"stream\":\"music\",\"volume\":80}",
            prevValues = "{\"volume\":70}"
        )

        assertEquals(timestamp, logEntry.timestamp)
        assertEquals("volume_changed", logEntry.action)
        assertEquals("{\"stream\":\"music\",\"volume\":80}", logEntry.values)
        assertEquals("{\"volume\":70}", logEntry.prevValues)
    }

    @Test
    fun testLogEntryWithoutPrevValues() {
        val logEntry = LogEntry(
            timestamp = 123456789L,
            action = "preset_applied",
            values = "{\"music\":80,\"chat\":100,\"system\":50}"
        )

        assertEquals(null, logEntry.prevValues)
    }
}