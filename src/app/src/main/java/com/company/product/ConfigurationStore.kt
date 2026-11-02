package com.company.product

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The token never enters source code, logs, backups, or plaintext preferences. */
class ConfigurationStore(context: Context) {
    private val prefs = context.getSharedPreferences("connection_v2", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }

    fun load(): BotConfiguration? {
        val encoded = prefs.getString("configuration", null) ?: return null
        val parts = encoded.split(":", limit = 2)
        require(parts.size == 2)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        val json = JSONObject(String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8))
        return BotConfiguration(json.getString("token"), json.getLong("owner"), json.getString("name"))
            .also { require(it.isValid()) }
    }

    fun save(configuration: BotConfiguration) {
        require(configuration.isValid())
        val old = runCatching { load() }.getOrNull()
        val json = JSONObject().put("token", configuration.token).put("owner", configuration.ownerId)
            .put("name", configuration.deviceName).toString()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encoded = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(cipher.doFinal(json.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        val editor = prefs.edit().putString("configuration", encoded)
        if (old == null || old.token != configuration.token || old.ownerId != configuration.ownerId) editor.remove("offset")
        check(editor.commit()) { "Unable to save configuration" }
    }

    var nextUpdateId: Long?
        get() = if (prefs.contains("offset")) prefs.getLong("offset", 0) else null
        set(value) { check(prefs.edit().apply {
            if (value == null) remove("offset") else putLong("offset", value)
        }.commit()) }

    fun clear() {
        check(prefs.edit().clear().commit())
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(KEY_ALIAS) }
    }

    private companion object { const val KEY_ALIAS = "appmusic.connection.v2" }
}
