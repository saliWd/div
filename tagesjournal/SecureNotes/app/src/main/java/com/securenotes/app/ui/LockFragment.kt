package com.securenotes.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.securenotes.app.R
import com.securenotes.app.databinding.FragmentLockBinding
import com.securenotes.app.viewmodel.LockState
import com.securenotes.app.viewmodel.LockViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.crypto.Cipher

@AndroidEntryPoint
class LockFragment : Fragment() {

    private var _binding: FragmentLockBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LockViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.lockState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LockState.NeedAuth -> showBiometricPrompt(state.cipher)
                is LockState.Unlocked -> navigateToNotes()
                is LockState.Error    -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    binding.btnUnlock.isEnabled = true
                }
                else -> Unit
            }
        }

        binding.btnUnlock.setOnClickListener {
            binding.btnUnlock.isEnabled = false
            viewModel.checkAndPrepareAuth()
        }

        // Check biometric availability
        checkBiometricSupport()
    }

    private fun checkBiometricSupport() {
        val bm = BiometricManager.from(requireContext())
        when (bm.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // ready – trigger automatically
                viewModel.checkAndPrepareAuth()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                showError(getString(R.string.error_no_fingerprint_enrolled))
            else ->
                showError(getString(R.string.error_biometric_unavailable))
        }
    }

    private fun showBiometricPrompt(cipher: Cipher) {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.cancel))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    result.cryptoObject?.cipher?.let { viewModel.onAuthSuccess(it) }
                        ?: viewModel.onAuthError("Cipher not available")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    viewModel.onAuthError(errString.toString())
                    binding.btnUnlock.isEnabled = true
                }

                override fun onAuthenticationFailed() {
                    // fingerprint not recognised – prompt stays visible, do nothing
                }
            })

        biometricPrompt.authenticate(
            promptInfo,
            BiometricPrompt.CryptoObject(cipher)
        )
    }

    private fun navigateToNotes() {
        findNavController().navigate(R.id.action_lockFragment_to_notesFragment)
    }

    private fun showError(message: String) {
        binding.tvStatus.text = message
        binding.btnUnlock.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
