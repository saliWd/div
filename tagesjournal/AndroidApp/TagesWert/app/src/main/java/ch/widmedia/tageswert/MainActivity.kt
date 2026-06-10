package ch.widmedia.tageswert

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Rational
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
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
import ch.widmedia.tageswert.data.db.TagesWertDatabase
import ch.widmedia.tageswert.data.repository.EintragRepository
import ch.widmedia.tageswert.security.BiometricHelper
import ch.widmedia.tageswert.security.SecurityManager
import ch.widmedia.tageswert.ui.TagesWertNavigation
import ch.widmedia.tageswert.ui.MainViewModel
import ch.widmedia.tageswert.ui.screens.AuthStatus
import ch.widmedia.tageswert.ui.screens.SperrScreen
import ch.widmedia.tageswert.ui.theme.AppBackground
import ch.widmedia.tageswert.ui.theme.TagesWertTheme

class MainActivity : FragmentActivity() {

    private lateinit var viewModel: MainViewModel
    private var onPickerResult: ((Uri?) -> Unit)? = null
    private var isInPipMode by mutableStateOf(false)

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        onPickerResult?.invoke(uri)
        onPickerResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
                AppBackground {
                    if (isInPipMode) {
                        PipContent()
                    } else {
                        AppContent()
                    }
                }
            }
        }
    }

    private fun updatePipParams(enabled: Boolean) {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setAutoEnterEnabled(enabled)
            .build()
        setPictureInPictureParams(params)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    @Composable
    private fun PipContent() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = getString(R.string.app_name))
        }
    }

    /**
     * Helper to launch file picker using Activity Result API
     */
    fun launchFilePicker(defaultFileName: String, callback: (Uri?) -> Unit) {
        onPickerResult = callback
        filePickerLauncher.launch(defaultFileName)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (::viewModel.isInitialized) {
            viewModel.resetInactivityTimer()
        }
    }

    @Composable
    private fun AppContent() {
        var entsperrt by rememberSaveable { mutableStateOf(value = false) }
        var authStatus by rememberSaveable { mutableStateOf(value = AuthStatus.WAITING) }
        var fehlermeldung by rememberSaveable { mutableStateOf<String?>(value = null) }

        // Observe lock state from ViewModel
        val shouldLock by viewModel.shouldLock.collectAsState()
        
        LaunchedEffect(entsperrt) {
            updatePipParams(entsperrt)
        }

        LaunchedEffect(shouldLock) {
            if (shouldLock && entsperrt) {
                entsperrt = false
                authStatus = AuthStatus.WAITING
            }
        }

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
                        // No biometrics: stay on lock screen and show error
                        fehlermeldung = getString(R.string.auth_no_biometric)
                        authStatus = AuthStatus.ERROR
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
                        overrideActivityTransition(
                            OVERRIDE_TRANSITION_OPEN,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                SperrScreen(
                    onAuthentifiziert = { 
                        entsperrt = true
                        viewModel.resetInactivityTimer()
                    },
                    onTriggerAuth = triggerAuth,
                    authStatus = authStatus,
                    fehlermeldung = fehlermeldung,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
