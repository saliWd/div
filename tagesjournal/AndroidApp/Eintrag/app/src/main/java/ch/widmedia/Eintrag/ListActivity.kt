package ch.widmedia.Eintrag

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ch.widmedia.Eintrag.databinding.ActivityListBinding
import ch.widmedia.Eintrag.databinding.DialogExportPasswordBinding
import ch.widmedia.Eintrag.databinding.DialogImportPasswordBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListBinding
    private lateinit var adapter: EntryAdapter
    private val db get() = DatabaseHelper.getInstance(this, AppState.dbPassword!!)

    companion object {
        private const val REQ_EXPORT = 1001
        private const val REQ_IMPORT = 1002
        const val EXTRA_ENTRY_ID = "entry_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = EntryAdapter { entry ->
            startActivity(
                Intent(this, DetailActivity::class.java)
                    .putExtra(EXTRA_ENTRY_ID, entry.id)
            )
        }

        binding.recyclerEntries.layoutManager = LinearLayoutManager(this)
        binding.recyclerEntries.adapter = adapter
        binding.fabAdd.setOnClickListener { startActivity(Intent(this, AddEntryActivity::class.java)) }
        binding.btnMenu.setOnClickListener { showOptionsMenu(it) }
    }

    override fun onResume() {
        super.onResume()
        refreshEntries()
    }

    private fun refreshEntries() {
        val entries = db.getAllEntries()
        adapter.submitList(entries)
        val count = entries.size
        binding.tvEntryCount.text = if (count == 0) "" else
            if (count == 1) getString(R.string.entries_count_one, count)
            else getString(R.string.entries_count_other, count)
        binding.recyclerEntries.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility    = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }

    // ── Options menu ───────────────────────────────────────────────────────────

    private fun showOptionsMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, getString(R.string.export_database))
            menu.add(0, 2, 1, getString(R.string.import_database))
            menu.add(0, 3, 2, getString(R.string.change_export_password))
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> initiateExport()
                    2 -> initiateImport()
                    3 -> changeExportPassword()
                }
                true
            }
            show()
        }
    }

    // ── Export ─────────────────────────────────────────────────────────────────

    private fun initiateExport() {
        if (!CryptoManager.hasExportPassword(this)) {
            changeExportPassword(onDone = { startExportPicker() })
            return
        }
        startExportPicker()
    }

    private fun startExportPicker() {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "eintrag_$ts.enc")
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQ_EXPORT)
    }

    private fun performExport(uri: Uri) {
        val (iv, encPass) = CryptoManager.loadEncryptedExportPassword(this) ?: return
        val cipher = CryptoManager.decryptDbPasswordCipher(iv)
        CryptoManager.showBiometricPrompt(
            activity = this,
            title = getString(R.string.biometric_export),
            subtitle = getString(R.string.biometric_subtitle),
            negativeText = getString(R.string.biometric_cancel),
            cipher = cipher,
            onSuccess = { authCipher ->
                val passBytes = authCipher.doFinal(encPass)
                val exportPw = passBytes.map { it.toInt().toChar() }.toCharArray()
                passBytes.fill(0)
                try {
                    DatabaseHelper.clearInstance()
                    val dbFile = getDatabasePath(DatabaseHelper.DB_NAME)
                    val tmp = File(cacheDir, "exp_tmp.enc")
                    CryptoManager.encryptFile(dbFile, tmp, exportPw)
                    exportPw.fill('0')
                    DatabaseHelper.getInstance(this, AppState.dbPassword!!)
                    contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(tmp).use { it.copyTo(out) }
                    }
                    tmp.delete()
                    toast(getString(R.string.export_success))
                } catch (e: Exception) {
                    DatabaseHelper.getInstance(this, AppState.dbPassword!!)
                    toast(getString(R.string.export_failed, e.message))
                }
            },
            onError = { msg -> toast(getString(R.string.auth_failed, msg)) }
        )
    }

    // ── Import ─────────────────────────────────────────────────────────────────

    private fun initiateImport() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.import_confirm_title))
            .setMessage(getString(R.string.import_confirm_msg))
            .setPositiveButton(getString(R.string.import_label)) { _, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/octet-stream"
                }
                @Suppress("DEPRECATION")
                startActivityForResult(intent, REQ_IMPORT)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun performImport(uri: Uri) {
        if (CryptoManager.hasExportPassword(this)) {
            val (iv, encPass) = CryptoManager.loadEncryptedExportPassword(this)!!
            val cipher = CryptoManager.decryptDbPasswordCipher(iv)
            CryptoManager.showBiometricPrompt(
                activity = this,
                title = getString(R.string.biometric_import),
                subtitle = getString(R.string.biometric_subtitle),
                negativeText = getString(R.string.biometric_cancel),
                cipher = cipher,
                onSuccess = { authCipher ->
                    val passBytes = authCipher.doFinal(encPass)
                    val exportPw = passBytes.map { it.toInt().toChar() }.toCharArray()
                    passBytes.fill(0)
                    doImport(uri, exportPw)
                },
                onError = { promptManualImport(uri) }
            )
        } else {
            promptManualImport(uri)
        }
    }

    private fun promptManualImport(uri: Uri) {
        val dialogBinding = DialogImportPasswordBinding.inflate(LayoutInflater.from(this))
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.enter_export_password))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.import_label)) { _, _ ->
                doImport(uri, dialogBinding.etImportPassword.text.toString().toCharArray())
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun doImport(uri: Uri, password: CharArray) {
        try {
            val tmpEnc = File(cacheDir, "imp_src.enc")
            val tmpDec = File(cacheDir, "imp_dec.db")
            contentResolver.openInputStream(uri)?.use { FileOutputStream(tmpEnc).use { o -> it.copyTo(o) } }
            CryptoManager.decryptFile(tmpEnc, tmpDec, password)
            password.fill('0')
            tmpEnc.delete()
            DatabaseHelper.clearInstance()
            val dest = getDatabasePath(DatabaseHelper.DB_NAME)
            dest.parentFile?.mkdirs()
            tmpDec.copyTo(dest, overwrite = true)
            tmpDec.delete()
            DatabaseHelper.getInstance(this, AppState.dbPassword!!)
            refreshEntries()
            toast(getString(R.string.import_success))
        } catch (e: Exception) {
            toast(getString(R.string.import_failed))
        }
    }

    // ── Export password ────────────────────────────────────────────────────────

    private fun changeExportPassword(onDone: (() -> Unit)? = null) {
        val dialogBinding = DialogExportPasswordBinding.inflate(LayoutInflater.from(this))
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.export_password_title))
            .setMessage(getString(R.string.export_password_msg))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val p1 = dialogBinding.etPassword.text.toString()
                val p2 = dialogBinding.etConfirm.text.toString()
                when {
                    p1.length < 8 -> toast(getString(R.string.error_password_too_short))
                    p1 != p2      -> toast(getString(R.string.error_passwords_no_match))
                    else -> {
                        val cipher = CryptoManager.encryptDbPasswordCipher()
                        CryptoManager.showBiometricPrompt(
                            activity = this,
                            title = getString(R.string.biometric_save_password),
                            subtitle = getString(R.string.biometric_subtitle),
                            negativeText = getString(R.string.biometric_cancel),
                            cipher = cipher,
                            onSuccess = { authCipher ->
                                CryptoManager.saveExportPassword(this, authCipher, p1.toByteArray())
                                toast(getString(R.string.password_saved))
                                onDone?.invoke()
                            },
                            onError = { msg -> toast(getString(R.string.save_failed, msg)) }
                        )
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ── Activity result ────────────────────────────────────────────────────────

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQ_EXPORT -> performExport(uri)
            REQ_IMPORT -> performImport(uri)
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
