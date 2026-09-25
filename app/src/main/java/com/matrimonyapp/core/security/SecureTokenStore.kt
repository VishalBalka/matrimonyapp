package com.matrimonyapp.core.security

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class SecureTokenStore(context: Context) : TokenStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun save(token: String) {
        require(token.isNotBlank())
        val cipher = newCipher(Cipher.ENCRYPT_MODE)
        val ciphertext = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()
    }

    override fun get(): String? {
        val ivEncoded = preferences.getString(KEY_IV, null) ?: return null
        val ciphertextEncoded = preferences.getString(KEY_CIPHERTEXT, null) ?: return null

        return try {
            val iv = Base64.decode(ivEncoded, Base64.NO_WRAP)
            val ciphertext = Base64.decode(ciphertextEncoded, Base64.NO_WRAP)
            val cipher = newCipher(Cipher.DECRYPT_MODE, iv)
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        } catch (_: Exception) {
            clear()
            null
        }
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private fun newCipher(mode: Int, iv: ByteArray? = null): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = getOrCreateKey()
        if (mode == Cipher.ENCRYPT_MODE) {
            cipher.init(mode, key)
        } else {
            requireNotNull(iv)
            cipher.init(mode, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        }
        return cipher
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "matrimony_auth_token"
        const val PREFERENCES_NAME = "secure_auth_state"
        const val KEY_IV = "iv"
        const val KEY_CIPHERTEXT = "ciphertext"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
