package ch.widmedia.guetetag

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import android.security.keystore.KeyPermanentlyInvalidatedException
import ch.widmedia.guetetag.data.AppDatabase
import ch.widmedia.guetetag.security.BiometricHelper
import ch.widmedia.guetetag.security.KeystoreHelper
import ch.widmedia.guetetag.security.PassphraseManager
import ch.widmedia.guetetag.ui.GueteTagApp
import ch.widmedia.guetetag.ui.theme.Gold
import ch.widmedia.guetetag.ui.theme.GueteTagTheme
import ch.widmedia.guetetag.ui.theme.Plum

// ── UI state for the auth flow ────────────────────────────────────────────────

private sealed interface AuthUiState {
    object Loading : AuthUiState
    data class Error(val message: String, val canReset: Boolean = false) : AuthUiState
    data class Ready(val db: AppDatabase) : AuthUiState
}

class MainActivity : FragmentActivity() {

    private var uiState by mutableStateOf<AuthUiState>(AuthUiState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GueteTagTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    when (val s = uiState) {
                        is AuthUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text  = "GueteTag",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Plum
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    CircularProgressIndicator(color = Gold)
                                }
                            }
                        }

                        is AuthUiState.Error -> {
                            Box(
                                modifier          = Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment  = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text      = s.message,
                                        style     = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        color     = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick  = { startAuthFlow() },
                                        colors   = ButtonDefaults.buttonColors(containerColor = Plum)
                                    ) { Text("Erneut versuchen") }

                                    if (s.canReset) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick  = { resetApp() },
                                            colors   = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) { Text("App zurücksetzen") }
                                    }
                                }
                            }
                        }

                        is AuthUiState.Ready -> GueteTagApp(db = s.db)
                    }
                }
            }
        }

        startAuthFlow()

        // Re-authenticate when app returns to foreground
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // When app goes to background, lock it
                if (uiState is AuthUiState.Ready) {
                    AppDatabase.closeAndReset()
                    uiState = AuthUiState.Loading
                }
            } else if (event == Lifecycle.Event.ON_START) {
                // When app comes to foreground, trigger auth if not already ready
                if (uiState !is AuthUiState.Ready) {
                    startAuthFlow()
                }
            }
        })
    }

    // ── Auth flow ─────────────────────────────────────────────────────────────

    private fun startAuthFlow() {
        uiState = AuthUiState.Loading

        // 1. Check biometric availability
        if (!BiometricHelper.canAuthenticate(this)) {
            uiState = AuthUiState.Error(
                "Keine Biometrie verfügbar. Bitte richten Sie einen Fingerabdruck ein."
            )
            return
        }

        // 2. Generate KeyStore key (idempotent)
        try {
            KeystoreHelper.generateOrGetKey()
        } catch (e: Exception) {
            uiState = AuthUiState.Error("Fehler beim Initialisieren der Sicherheitskomponenten.")
            return
        }

        // 3. Prepare cipher for encrypt (first launch) or decrypt (subsequent)
        val cipher = try {
            if (PassphraseManager.isFirstLaunch(this)) {
                KeystoreHelper.getEncryptCipher()
            } else {
                val iv = PassphraseManager.getStoredIv(this)
                KeystoreHelper.getDecryptCipher(iv)
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            uiState = AuthUiState.Error(
                message  = "Der Sicherheitsschlüssel wurde durch Änderungen der Biometrie ungültig. Bitte App zurücksetzen.",
                canReset = true
            )
            return
        } catch (e: Exception) {
            uiState = AuthUiState.Error("Fehler beim Laden des Schlüssels: ${e.localizedMessage}")
            return
        }

        // 4. Show biometric prompt
        BiometricHelper.authenticate(
            activity  = this,
            cipher    = cipher,
            onSuccess = { authenticatedCipher ->
                try {
                    val passphrase = if (PassphraseManager.isFirstLaunch(this)) {
                        PassphraseManager.generateAndStorePassphrase(this, authenticatedCipher)
                    } else {
                        PassphraseManager.retrievePassphrase(this, authenticatedCipher)
                    }
                    val db = AppDatabase.getInstance(this, passphrase)
                    uiState = AuthUiState.Ready(db)
                } catch (e: Exception) {
                    uiState = AuthUiState.Error(
                        "Fehler beim Öffnen der Datenbank: ${e.localizedMessage}"
                    )
                }
            },
            onError   = { msg ->
                uiState = AuthUiState.Error("Authentifizierung fehlgeschlagen: $msg")
            }
        )
    }

    // ── Hard reset (key invalidated) ──────────────────────────────────────────

    private fun resetApp() {
        AppDatabase.closeAndReset()
        PassphraseManager.clearAll(this)
        KeystoreHelper.deleteKey()
        deleteDatabase("guetetag.db")
        startAuthFlow()

        // Re-authenticate when app returns to foreground
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // When app goes to background, lock it
                if (uiState is AuthUiState.Ready) {
                    AppDatabase.closeAndReset()
                    uiState = AuthUiState.Loading
                }
            } else if (event == Lifecycle.Event.ON_START) {
                // When app comes to foreground, trigger auth if not already ready
                if (uiState !is AuthUiState.Ready) {
                    startAuthFlow()
                }
            }
        })
    }
}
