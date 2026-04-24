package ch.widmedia.guetetag.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the AES-256-GCM key stored in the Android KeyStore.
 * The key is protected by strong biometric authentication and is
 * invalidated if biometric enrollments change.
 */
object KeystoreHelper {

    private const val KEY_ALIAS   = "guetetag_db_key"
    private const val KEYSTORE    = "AndroidKeyStore"
    private const val CIPHER_ALGO = "AES/GCM/NoPadding"
    private const val GCM_TAG_LEN = 128

    /** Creates the key if it does not yet exist. Safe to call on every launch. */
    fun generateOrGetKey() {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) return

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
            .setInvalidatedByBiometricEnrollment(true)
            .build()

        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    /** Returns an ENCRYPT_MODE cipher (use for first launch / passphrase creation). */
    fun getEncryptCipher(): Cipher {
        val key = loadKey()
        return Cipher.getInstance(CIPHER_ALGO).also { it.init(Cipher.ENCRYPT_MODE, key) }
    }

    /** Returns a DECRYPT_MODE cipher initialised with the stored IV. */
    fun getDecryptCipher(iv: ByteArray): Cipher {
        val key = loadKey()
        return Cipher.getInstance(CIPHER_ALGO).also {
            it.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LEN, iv))
        }
    }

    /** Deletes the key – call when resetting the app after key invalidation. */
    fun deleteKey() {
        runCatching {
            KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
        }
    }

    private fun loadKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        return ks.getKey(KEY_ALIAS, null) as SecretKey
    }
}
