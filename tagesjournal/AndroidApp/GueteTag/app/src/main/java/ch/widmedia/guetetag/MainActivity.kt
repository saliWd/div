package ch.widmedia.guetetag

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import ch.widmedia.guetetag.data.db.TagesWertDatabase
import ch.widmedia.guetetag.data.repository.EintragRepository
import ch.widmedia.guetetag.security.BiometricHelper
import ch.widmedia.guetetag.security.SecurityManager
import ch.widmedia.guetetag.ui.TagesWertNavigation
import ch.widmedia.guetetag.ui.MainViewModel
import ch.widmedia.guetetag.ui.screens.AuthStatus
import ch.widmedia.guetetag.ui.screens.SperrScreen
import ch.widmedia.guetetag.ui.theme.TagesWertTheme

class MainActivity : FragmentActivity() {

    private lateinit var viewModel: MainViewModel
    private var onPickerResult: ((Uri?) -> Unit)? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        onPickerResult?.invoke(uri)
        onPickerResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize encrypted database with biometric-protected passphrase
        val passphrase = SecurityManager.getOrCreateDbPassphrase(this)
        val db = TagesWertDatabase.getInstance(this, passphrase)
        val repository = EintragRepository(db.tagEintragDao())

        viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(repository)
        )[MainViewModel::class.java]

        setContent {
            TagesWertTheme {
                AppContent()
            }
        }
    }

    /**
     * Helper to launch file picker using Activity Result API
     */
    fun launchFilePicker(defaultFileName: String, callback: (Uri?) -> Unit) {
        onPickerResult = callback
        filePickerLauncher.launch(defaultFileName)
    }

    @Composable
    private fun AppContent() {
        var entsperrt by rememberSaveable { mutableStateOf(value = false) }
        var authStatus by rememberSaveable { mutableStateOf(value = AuthStatus.WAITING) }
        var fehlermeldung by rememberSaveable { mutableStateOf<String?>(value = null) }

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
                },
            )
        }

        // Auto-trigger on first launch if biometric is available
        val lifecycleOwner = LocalLifecycleOwner.current
        
        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (!entsperrt && (authStatus == AuthStatus.WAITING)) {
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
                TagesWertNavigation(
                    viewModel = viewModel,
                    onLock = {
                        TagesWertDatabase.destroyInstance()
                        // Restart activity to ensure all components are re-initialized with a fresh DB session
                        finish()
                        startActivity(intent)
                        @Suppress("DEPRECATION")
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    },
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
