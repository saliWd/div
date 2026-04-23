package ch.widmedia.guetetag

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.entryProvider
import ch.widmedia.guetetag.security.BiometricAuthenticator
import ch.widmedia.guetetag.ui.navigation.Destination
import ch.widmedia.guetetag.ui.screens.CalendarScreen
import ch.widmedia.guetetag.ui.screens.EntryScreen
import ch.widmedia.guetetag.ui.theme.GueteTagTheme

class MainActivity : FragmentActivity() {
    private lateinit var biometricAuthenticator: BiometricAuthenticator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricAuthenticator = BiometricAuthenticator(this)
        enableEdgeToEdge()
        setContent {
            GueteTagTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember { mutableStateOf(false) }
                    var authError by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        authenticateUser(
                            onSuccess = { isAuthenticated = true },
                            onError = { error -> authError = error }
                        )
                    }

                    if (isAuthenticated) {
                        GueteTagApp()
                    } else {
                        LockScreen(
                            error = authError,
                            onRetry = {
                                authError = null
                                authenticateUser(
                                    onSuccess = { isAuthenticated = true },
                                    onError = { error -> authError = error }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun authenticateUser(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (biometricAuthenticator.isBiometricAvailable()) {
            biometricAuthenticator.authenticate(
                activity = this,
                title = "Journal entsperren",
                subtitle = "Bitte authentifizieren Sie sich, um auf Ihr Tagebuch zuzugreifen.",
                onSuccess = { onSuccess() },
                onError = { _, errString -> onError(errString.toString()) },
                onFailed = { onError("Authentifizierung fehlgeschlagen") }
            )
        } else {
            // Wenn keine Biometrie verfügbar ist (z.B. Emulator ohne Setup), 
            // erlauben wir den Zugriff oder zeigen eine Meldung.
            // In einer echten Sicherheits-App würde man hier ein Backup-Passwort verlangen.
            onSuccess()
        }
    }
}

@Composable
fun LockScreen(error: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 80.dp), // 10% Bottom Padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "GueteTag ist gesperrt",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Authentifizieren Sie sich, um Ihre privaten Einträge zu sehen.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (error != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Erneut versuchen")
        }
    }
}

@Composable
fun GueteTagApp() {
    val backStack = rememberNavBackStack(Destination.Calendar)

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        onBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.size - 1)
            }
        },
        entryProvider = entryProvider {
            entry<Destination.Calendar> {
                CalendarScreen(
                    onNavigateToEntry = { dateMillis ->
                        backStack.add(Destination.Entry(dateMillis))
                    }
                )
            }
            entry<Destination.Entry> { key ->
                EntryScreen(
                    dateMillis = key.dateMillis,
                    onNavigateBack = {
                        backStack.removeAt(backStack.size - 1)
                    }
                )
            }
        }
    )
}
