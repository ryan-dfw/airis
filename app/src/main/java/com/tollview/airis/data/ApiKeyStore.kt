package com.tollview.airis.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Persists the OpenAI API key on-device.
 *
 * The key is encrypted with an AES-GCM key that lives in the Android Keystore,
 * so the value in SharedPreferences is useless off this phone. Backups of
 * shared prefs are also excluded in the manifest's backup rules.
 */
class ApiKeyStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): String? {
        val stored = prefs.getString(KEY_BLOB, null) ?: return null
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, IV_LENGTH)
            val cipherText = bytes.copyOfRange(IV_LENGTH, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            // Keystore was reset (e.g. lock-screen change on some devices). Ask again.
            prefs.edit().remove(KEY_BLOB).apply()
            null
        }
    }

    fun save(apiKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        val blob = Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
        prefs.edit().putString(KEY_BLOB, blob).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_BLOB).apply()
    }

    /** Last camera picture URL we developed, so a stale getEvent result is never re-developed. */
    var lastProcessedUrl: String?
        get() = prefs.getString(KEY_LAST_URL, null)
        set(value) {
            prefs.edit().putString(KEY_LAST_URL, value).apply()
        }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS = "airis_secure"
        const val KEY_BLOB = "openai_key"
        const val KEY_LAST_URL = "last_camera_url"
        const val ALIAS = "airis_openai_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
    }
}
