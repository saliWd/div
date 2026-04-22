package ch.widmedia.Eintrag

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ch.widmedia.Eintrag.databinding.ActivityUnlockBinding
import java.security.SecureRandom

class UnlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUnlockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (CryptoManager.hasDbPassword(this)) {
            binding.tvUnlockTitle.text = getString(R.string.unlock_title)
            binding.tvUnlockSubtitle.text = getString(R.string.unlock_subtitle)
            binding.btnUnlock.text = getString(R.string.authenticate)
            binding.btnUnlock.setOnClickListener { doUnlock() }
            doUnlock()
        } else {
            binding.tvUnlockTitle.text = getString(R.string.setup_title)
            binding.tvUnlockSubtitle.text = getString(R.string.setup_subtitle)
            binding.btnUnlock.text = getString(R.string.setup_button)
            binding.btnUnlock.setOnClickListener { doSetup() }
        }
    }

    private fun doSetup() {
        val newPass = generateDbPassword()
        val cipher = CryptoManager.encryptDbPasswordCipher()
        CryptoManager.showBiometricPrompt(
            activity = this,
            title = getString(R.string.biometric_register),
            subtitle = getString(R.string.biometric_subtitle),
            negativeText = getString(R.string.biometric_cancel),
            cipher = cipher,
            onSuccess = { authenticatedCipher ->
                CryptoManager.saveEncryptedDbPassword(
                    this, authenticatedCipher,
                    newPass.map { it.code.toByte() }.toByteArray()
                )
                AppState.dbPassword = newPass
                DatabaseHelper.getInstance(this, newPass)
                openList()
            },
            onError = { msg -> showError(getString(R.string.setup_failed, msg)) }
        )
    }

    private fun doUnlock() {
        hideError()
        val (iv, _) = CryptoManager.loadEncryptedDbPassword(this) ?: run {
            showError(getString(R.string.error_credentials_not_found)); return
        }
        val cipher = CryptoManager.decryptDbPasswordCipher(iv)
        CryptoManager.showBiometricPrompt(
            activity = this,
            title = getString(R.string.biometric_unlock),
            subtitle = getString(R.string.biometric_subtitle),
            negativeText = getString(R.string.biometric_cancel),
            cipher = cipher,
            onSuccess = { authenticatedCipher ->
                val (_, encPass) = CryptoManager.loadEncryptedDbPassword(this)!!
                val passBytes = authenticatedCipher.doFinal(encPass)
                val password = passBytes.map { it.toInt().toChar() }.toCharArray()
                passBytes.fill(0)
                AppState.dbPassword = password
                DatabaseHelper.getInstance(this, password)
                openList()
            },
            onError = { msg ->
                showError(msg)
                binding.btnUnlock.setOnClickListener { doUnlock() }
            }
        )
    }

    private fun openList() {
        startActivity(Intent(this, ListActivity::class.java))
        finish()
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }

    private fun generateDbPassword(): CharArray {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()"
        val rng = SecureRandom()
        return CharArray(64) { chars[rng.nextInt(chars.length)] }
    }
}
