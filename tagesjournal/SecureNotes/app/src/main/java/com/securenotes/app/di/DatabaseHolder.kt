package com.securenotes.app.di

import android.content.Context
import com.securenotes.app.data.NotesDatabase
import com.securenotes.app.security.CryptoManager
import com.securenotes.app.security.PassphraseManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the [NotesDatabase] instance.  The DB cannot be initialised until
 * biometric authentication has decrypted the passphrase, so [initDatabase]
 * must be called from [com.securenotes.app.ui.LockFragment] after successful auth.
 */
@Singleton
class DatabaseHolder @Inject constructor(
    private val context: Context,
    private val cryptoManager: CryptoManager,
    private val passphraseManager: PassphraseManager
) {
    private var _database: NotesDatabase? = null

    val database: NotesDatabase
        get() = _database ?: error("Database not initialised – unlock first")

    val isInitialised: Boolean get() = _database != null

    /** Call after biometric cipher has decrypted the passphrase. */
    fun initDatabase(passphrase: ByteArray) {
        if (_database == null) {
            _database = NotesDatabase.build(context, passphrase)
        }
        passphrase.fill(0) // zero passphrase bytes in memory immediately
    }
}
