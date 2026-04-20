package com.securenotes.app.di

import android.content.Context
import com.securenotes.app.data.NoteDao
import com.securenotes.app.data.NotesDatabase
import com.securenotes.app.security.CryptoManager
import com.securenotes.app.security.PassphraseManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * The database is provided lazily via [DatabaseHolder] because it cannot
     * be opened until biometric auth succeeds and the passphrase is available.
     */
    @Provides
    @Singleton
    fun provideDatabaseHolder(
        @ApplicationContext context: Context,
        cryptoManager: CryptoManager,
        passphraseManager: PassphraseManager
    ): DatabaseHolder = DatabaseHolder(context, cryptoManager, passphraseManager)

    @Provides
    fun provideNoteDao(holder: DatabaseHolder): NoteDao = holder.database.noteDao()
}
