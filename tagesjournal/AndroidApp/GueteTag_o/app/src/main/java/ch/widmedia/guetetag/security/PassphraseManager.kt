package ch.widmedia.guetetag.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores and retrieves the database passphrase in SharedPreferences.
 * The passphrase itself is encrypted with the biometric-protected KeyStore key
 * managed by [KeystoreHelper].
 */
object PassphraseManager {

    private const val PREFS_NAME       = "guetetag_auth_prefs"
    private const val KEY_ENC_PASS     = "encrypted_passphrase"
    private const val KEY_IV           = "passphrase_iv"
    private const val PASSPHRASE_BYTES = 32

    fun isFirstLaunch(context: Context): Boolean =
        !prefs(context).contains(KEY_ENC_PASS)

    /**
     * Generates a random passphrase, encrypts it with [cipher], and stores
     * the result. Returns the plaintext passphrase (caller must zero it after use).
     */
    fun generateAndStorePassphrase(context: Context, cipher: Cipher): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val encrypted  = cipher.doFinal(passphrase)
        val iv         = cipher.parameters.getParameterSpec(GCMParameterSpec::class.java).iv

        prefs(context).edit()
            .putString(KEY_ENC_PASS, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV,       Base64.encodeToString(iv,        Base64.NO_WRAP))
            .apply()

        return passphrase
    }

    /**
     * Decrypts and returns the passphrase using [cipher] (which must be
     * initialised with the stored IV via [getStoredIv]).
     */
    fun retrievePassphrase(context: Context, cipher: Cipher): ByteArray {
        val enc = Base64.decode(
            prefs(context).getString(KEY_ENC_PASS, null)
                ?: error("No passphrase stored"),
            Base64.NO_WRAP
        )
        return cipher.doFinal(enc)
    }

    /** Returns the IV that was stored alongside the encrypted passphrase. */
    fun getStoredIv(context: Context): ByteArray {
        val ivBase64 = prefs(context).getString(KEY_IV, null)
            ?: error("No IV stored")
        return Base64.decode(ivBase64, Base64.NO_WRAP)
    }

    /** Removes stored auth data – call when resetting the app. */
    fun clearAll(context: Context) =
        prefs(context).edit().clear().apply()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
