package ch.widmedia.guetetag.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

/**
 * Thin wrapper around [BiometricPrompt] that works with a [CryptoObject]
 * to authenticate before decrypting/encrypting the database passphrase.
 */
object BiometricHelper {

    fun canAuthenticate(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the biometric prompt.
     * [cipher] must be initialised (ENCRYPT or DECRYPT mode).
     * [onSuccess] receives the authenticated cipher ready for use.
     * [onError]   receives a user-visible error message in German.
     */
    fun authenticate(
        activity: FragmentActivity,
        cipher: Cipher,
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val c = result.cryptoObject?.cipher
                if (c != null) onSuccess(c)
                else onError("Kein Cipher nach Authentifizierung verfügbar.")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }

            // onAuthenticationFailed = wrong finger, user can retry – do nothing
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(ch.widmedia.guetetag.R.string.biometric_title))
            .setSubtitle(activity.getString(ch.widmedia.guetetag.R.string.biometric_subtitle))
            .setNegativeButtonText(activity.getString(ch.widmedia.guetetag.R.string.biometric_negative))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        BiometricPrompt(activity, executor, callback)
            .authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }
}
