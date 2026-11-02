package com.company.product

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors

class UiLoggerTest {
    @Before fun prepare() = UiLogger.clear()
    @After fun cleanup() = UiLogger.clear()
    @Test fun `journal is capped and oldest entries are dropped`() {
        repeat(150) { UiLogger.log("TEST", "event-$it") }
        val lines = UiLogger.logs.value.lines()
        assertEquals(100, lines.size)
        assertTrue(lines.first().endsWith("event-50"))
        assertTrue(lines.last().endsWith("event-149"))
    }
    @Test fun `tokens and multiline input are sanitized`() {
        val token = "123456789:" + "a".repeat(35)
        UiLogger.log("TEST", "error $token\nnew line")
        assertFalse(UiLogger.logs.value.contains(token))
        assertFalse(UiLogger.logs.value.contains('\n'))
        assertTrue(UiLogger.logs.value.contains("[jeton masqué]"))
    }
    @Test fun `parallel logging and clearing do not corrupt the buffer`() {
        val executor = Executors.newFixedThreadPool(6)
        try {
            val tasks = List(6) { worker -> executor.submit {
                repeat(100) { UiLogger.log("TEST", "$worker-$it"); if (it % 25 == 0) UiLogger.clear() }
            } }
            tasks.forEach { it.get() }
            assertTrue(UiLogger.logs.value.lines().size <= 100)
        } finally { executor.shutdownNow() }
    }
}
