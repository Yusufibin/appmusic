package com.company.product

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Requires a device: exercises the real Android Keystore, not a JVM substitute. */
@RunWith(AndroidJUnit4::class)
class ConfigurationStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var store: ConfigurationStore
    private val config = BotConfiguration("123456789:" + "a".repeat(35), 42, "Test Android")
    @Before fun setup() { store = ConfigurationStore(context); store.clear() }
    @After fun cleanup() { store.clear() }

    @Test fun encryptedRoundTripAndDeletion() {
        store.save(config)
        assertEquals(config, store.load())
        val raw = context.getSharedPreferences("connection_v2", Context.MODE_PRIVATE).getString("configuration", "")!!
        assertFalse(raw.contains(config.token))
        store.clear()
        assertNull(store.load())
    }
    @Test fun credentialChangesResetThePollingCursor() {
        store.save(config)
        store.nextUpdateId = 123
        store.save(config.copy(deviceName = "Autre nom"))
        assertEquals(123L, store.nextUpdateId)
        store.save(config.copy(ownerId = 99))
        assertNull(store.nextUpdateId)
    }
    @Test fun corruptedDataIsNotSilentlyAccepted() {
        context.getSharedPreferences("connection_v2", Context.MODE_PRIVATE).edit().putString("configuration", "broken").commit()
        assertTrue(runCatching { store.load() }.isFailure)
        store.save(config)
        assertEquals(config, store.load())
    }
}
