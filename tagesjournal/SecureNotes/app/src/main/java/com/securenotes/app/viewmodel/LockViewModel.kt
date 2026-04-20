package com.securenotes.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.securenotes.app.di.DatabaseHolder
import com.securenotes.app.security.CryptoManager
import com.securenotes.app.security.PassphraseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.crypto.Cipher
import javax.inject.Inject

sealed class LockState {
    object Idle          : LockState()
    object FirstRun      : LockState()   // no passphrase yet → generate + encrypt
    data class NeedAuth(val cipher: Cipher) : LockState()  // show biometric prompt
    object Unlocked      : LockState()
    data class Error(val message: String) : LockState()
}

@HiltViewModel
class LockViewModel @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val passphraseManager: PassphraseManager,
    private val databaseHolder: DatabaseHolder
) : ViewModel() {

    private val _lockState = MutableLiveData<LockState>(LockState.Idle)
    val lockState: LiveData<LockState> = _lockState

    fun checkAndPrepareAuth() {
        if (databaseHolder.isInitialised) {
            _lockState.value = LockState.Unlocked
            return
        }

        runCatching {
            val key = cryptoManager.getOrCreateKey()

            if (!passphraseManager.hasPassphrase()) {
                // First run: encrypt a fresh passphrase
                val cipher = cryptoManager.getEncryptCipher(key)
                _lockState.value = LockState.FirstRun.also {
                    _lockState.value = LockState.NeedAuth(cipher)
                }
            } else {
                // Returning user: need to decrypt existing passphrase
                val iv = passphraseManager.getPassphraseIv()
                    ?: error("IV not found")
                val cipher = cryptoManager.getDecryptCipher(key, iv)
                _lockState.value = LockState.NeedAuth(cipher)
            }
        }.onFailure {
            _lockState.value = LockState.Error("Key error: ${it.message}")
        }
    }

    /**
     * Called after the BiometricPrompt succeeds with the authenticated [cipher].
     */
    fun onAuthSuccess(cipher: Cipher) {
        runCatching {
            if (!passphraseManager.hasPassphrase()) {
                // First run – encrypt and store a new passphrase
                val rawPassphrase = passphraseManager.generatePassphrase()
                val (encrypted, iv) = cryptoManager.encrypt(cipher, rawPassphrase)
                passphraseManager.storeEncryptedPassphrase(encrypted, iv)
                databaseHolder.initDatabase(rawPassphrase)
            } else {
                // Decrypt existing passphrase
                val encryptedPassphrase = passphraseManager.getEncryptedPassphrase()
                    ?: error("Passphrase not found")
                val rawPassphrase = cryptoManager.decrypt(cipher, encryptedPassphrase)
                databaseHolder.initDatabase(rawPassphrase)
            }
            _lockState.value = LockState.Unlocked
        }.onFailure {
            _lockState.value = LockState.Error("Decryption failed: ${it.message}")
        }
    }

    fun onAuthError(errString: String) {
        _lockState.value = LockState.Error(errString)
    }
}
