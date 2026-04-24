package ch.widmedia.guetetag

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import ch.widmedia.guetetag.data.db.GueteTagDatabase
import ch.widmedia.guetetag.data.repository.EintragRepository
import ch.widmedia.guetetag.security.BiometricHelper
import ch.widmedia.guetetag.security.SecurityManager
import ch.widmedia.guetetag.ui.GueteTagNavigation
import ch.widmedia.guetetag.ui.MainViewModel
import ch.widmedia.guetetag.ui.screens.AuthStatus
import ch.widmedia.guetetag.ui.screens.SperrScreen
import ch.widmedia.guetetag.ui.theme.GueteTagTheme

class MainActivity : FragmentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize encrypted database with biometric-protected passphrase
        val passphrase = SecurityManager.getOrCreateDbPassphrase(this)
        val db = GueteTagDatabase.getInstance(this, passphrase)
        val repository = EintragRepository(db.tagEintragDao())

        viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(repository)
        )[MainViewModel::class.java]

        setContent {
            GueteTagTheme {
                AppContent()
            }
        }
    }

    @Composable
    private fun AppContent() {
        var entsperrt by remember { mutableStateOf(false) }
        var authStatus by remember { mutableStateOf(AuthStatus.WAITING) }
        var fehlermeldung by remember { mutableStateOf<String?>(null) }

        val triggerAuth: () -> Unit = {
            authStatus = AuthStatus.SCANNING
            BiometricHelper.showBiometricPrompt(
                activity = this,
                title = getString(R.string.auth_prompt),
                subtitle = getString(R.string.auth_subtitle),
                cancelText = getString(R.string.auth_cancel),
                onSuccess = {
                    authStatus = AuthStatus.SUCCESS
                },
                onError = { msg ->
                    fehlermeldung = msg
                    authStatus = AuthStatus.ERROR
                },
                onFailed = {
                    authStatus = AuthStatus.FAILED
                }
            )
        }

        // Auto-trigger on first launch if biometric is available
        val lifecycleOwner = LocalLifecycleOwner.current
        
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    entsperrt = false
                    authStatus = AuthStatus.WAITING
                    fehlermeldung = null
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (!entsperrt && authStatus == AuthStatus.WAITING) {
                    if (BiometricHelper.isBiometricAvailable(this@MainActivity)) {
                        triggerAuth()
                    } else {
                        // No biometrics: skip lock screen (or show error)
                        entsperrt = true
                    }
                }
            }
        }

        AnimatedContent(
            targetState = entsperrt,
            transitionSpec = {
                if (targetState) {
                    fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 } togetherWith
                    fadeOut(tween(300))
                } else {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                }
            },
            label = "authTransition",
            modifier = Modifier.fillMaxSize()
        ) { isUnlocked ->
            if (isUnlocked) {
                GueteTagNavigation(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                SperrScreen(
                    onAuthentifiziert = { entsperrt = true },
                    onTriggerAuth = triggerAuth,
                    authStatus = authStatus,
                    fehlermeldung = fehlermeldung,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
