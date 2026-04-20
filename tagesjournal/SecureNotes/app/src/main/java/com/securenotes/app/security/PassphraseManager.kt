package com.securenotes.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the AES-encrypted database passphrase (and its IV) in
 * EncryptedSharedPreferences so the raw bytes never hit disk unprotected.
 */
@Singleton
class PassphraseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME       = "secure_notes_prefs"
        private const val KEY_ENC_PASS     = "enc_passphrase"
        private const val KEY_PASS_IV      = "passphrase_iv"
        private const val PASSPHRASE_BYTES = 32
    }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasPassphrase(): Boolean = prefs.contains(KEY_ENC_PASS)

    /** Generate a fresh random passphrase (first-run only). */
    fun generatePassphrase(): ByteArray {
        val raw = ByteArray(PASSPHRASE_BYTES)
        SecureRandom().nextBytes(raw)
        return raw
    }

    /** Store the AES-encrypted passphrase + IV. */
    fun storeEncryptedPassphrase(encryptedPassphrase: ByteArray, iv: ByteArray) {
        prefs.edit()
            .putString(KEY_ENC_PASS, encryptedPassphrase.toHex())
            .putString(KEY_PASS_IV, iv.toHex())
            .apply()
    }

    fun getEncryptedPassphrase(): ByteArray? =
        prefs.getString(KEY_ENC_PASS, null)?.fromHex()

    fun getPassphraseIv(): ByteArray? =
        prefs.getString(KEY_PASS_IV, null)?.fromHex()

    // ── Hex helpers ──────────────────────────────────────────────────────────

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray {
        check(length % 2 == 0)
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
