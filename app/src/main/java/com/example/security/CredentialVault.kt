package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-local encrypted credential vault. Raw secrets never leave this class. */
class CredentialVault(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun listCredentials(): List<CredentialMeta> = readStore().map { it.meta() }

    fun putCredential(
        id: String,
        provider: String,
        kind: CredentialKind,
        label: String,
        secret: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        require(secret.isNotBlank()) { "Credential kosong." }
        val store = readStore().associateBy { it.id }.toMutableMap()
        store[id] = VaultEntry(
            id = id,
            provider = provider,
            kind = kind,
            label = label.ifBlank { id },
            encryptedSecret = encrypt(secret),
            metadata = metadata
        )
        writeStore(store.values.toList())
    }

    fun getSecret(id: String): String? = readStore().firstOrNull { it.id == id }?.let { decrypt(it.encryptedSecret) }

    fun removeCredential(id: String) {
        writeStore(readStore().filterNot { it.id == id })
    }

    fun hasCredential(id: String): Boolean = readStore().any { it.id == id }

    private fun readStore(): List<VaultEntry> {
        val raw = prefs.getString(STORE_KEY, "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val metadata = mutableMapOf<String, String>()
                val m = o.optJSONObject("metadata") ?: JSONObject()
                m.keys().forEach { key -> metadata[key] = m.optString(key) }
                add(
                    VaultEntry(
                        id = o.getString("id"),
                        provider = o.getString("provider"),
                        kind = CredentialKind.valueOf(o.getString("kind")),
                        label = o.getString("label"),
                        encryptedSecret = o.getString("secret"),
                        metadata = metadata
                    )
                )
            }
        }
    }

    private fun writeStore(entries: List<VaultEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val o = JSONObject()
                .put("id", entry.id)
                .put("provider", entry.provider)
                .put("kind", entry.kind.name)
                .put("label", entry.label)
                .put("secret", entry.encryptedSecret)
            val metadata = JSONObject()
            entry.metadata.forEach { (k, v) -> metadata.put(k, v) }
            o.put("metadata", metadata)
            array.put(o)
        }
        prefs.edit().putString(STORE_KEY, array.toString()).apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val ciphertext = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val parts = value.split(':', limit = 2)
        require(parts.size == 2) { "Credential vault entry rusak." }
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }

    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private data class VaultEntry(
        val id: String,
        val provider: String,
        val kind: CredentialKind,
        val label: String,
        val encryptedSecret: String,
        val metadata: Map<String, String>
    ) {
        fun meta() = CredentialMeta(id, provider, kind, label, metadata)
    }

    companion object {
        private const val PREFS = "ai_workstation_secure_vault"
        private const val STORE_KEY = "entries"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "AI_WORKSTATION_CREDENTIAL_V1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

enum class CredentialKind { AI_API_KEY, ROUTER_API_KEY, GITHUB, CLOUD, CLI }

data class CredentialMeta(
    val id: String,
    val provider: String,
    val kind: CredentialKind,
    val label: String,
    val metadata: Map<String, String>
)
