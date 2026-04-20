package com.securenotes.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a hardware-backed AES-256 key in the Android Keystore.
 *
 * The key requires biometric (fingerprint) or device credential authentication
 * every time it is used.  We use it to encrypt/decrypt a random 32-byte
 * database passphrase that is stored in EncryptedSharedPreferences.
 */
@Singleton
class CryptoManager @Inject constructor() {

    companion object {
        private const val KEY_ALIAS        = "secure_notes_db_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION    = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH    = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }

    // ── Key lifecycle ────────────────────────────────────────────────────────

    fun getOrCreateKey(): SecretKey {
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }
        return generateKey()
    }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // Requires strong biometric (Class 3 = fingerprint, face, iris)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                0,   // 0 = require auth for every use
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun keyExists(): Boolean = keyStore.containsAlias(KEY_ALIAS)

    fun deleteKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    // ── Cipher helpers ───────────────────────────────────────────────────────

    /** Returns an ENCRYPT Cipher ready for biometric prompt auth. */
    fun getEncryptCipher(key: SecretKey): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }

    /** Returns a DECRYPT Cipher initialised with the stored IV. */
    fun getDecryptCipher(key: SecretKey, iv: ByteArray): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        }

    fun encrypt(cipher: Cipher, data: ByteArray): Pair<ByteArray, ByteArray> {
        val encrypted = cipher.doFinal(data)
        return Pair(encrypted, cipher.iv)
    }

    fun decrypt(cipher: Cipher, encryptedData: ByteArray): ByteArray =
        cipher.doFinal(encryptedData)
}
