package ch.widmedia.Eintrag

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private var dbHelper: DatabaseHelper? = null
    private var dbPassword: CharArray? = null

    companion object {
        private const val REQ_EXPORT = 1001
        private const val REQ_IMPORT = 1002
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (CryptoManager.hasDbPassword(this)) {
            showBiometricUnlock()
        } else {
            showFirstTimeSetup()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbPassword?.fill('0')
        DatabaseHelper.clearInstance()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQ_EXPORT -> performExport(uri)
            REQ_IMPORT -> performImport(uri)
        }
    }

    // ── Setup / Unlock ─────────────────────────────────────────────────────────

    private fun showFirstTimeSetup() {
        val layout = buildRootLayout()
        layout.setPadding(48, 96, 48, 0)
        val tv = TextView(this).apply {
            text = "Welcome to Eintrag\n\nRegister your fingerprint to create your encrypted database."
            textSize = 17f
            setPadding(0, 0, 0, 32)
        }
        val btn = Button(this).apply {
            text = "Setup Fingerprint"
            setOnClickListener { registerBiometric() }
        }
        layout.addView(tv)
        layout.addView(btn)
        layout.addView(spacer())
        setContentView(layout)
    }

    private fun registerBiometric() {
        val newPass = generateDbPassword()
        val cipher = CryptoManager.encryptDbPasswordCipher()
        CryptoManager.showBiometricPrompt(
            activity = this,
            title = "Register Fingerprint",
            cipher = cipher,
            onSuccess = { authenticatedCipher ->
                CryptoManager.saveEncryptedDbPassword(
                    this, authenticatedCipher,
                    newPass.map { it.code.toByte() }.toByteArray()
                )
                dbPassword = newPass
                openDatabase()
                showEntryList()
            },
            onError = { msg -> toast("Setup failed: $msg") }
        )
    }

    private fun showBiometricUnlock() {
        val (iv, _) = CryptoManager.loadEncryptedDbPassword(this) ?: run {
            showFirstTimeSetup(); return
        }
        val cipher = CryptoManager.decryptDbPasswordCipher(iv)
        CryptoManager.showBiometricPrompt(
            activity = this,
            title = "Unlock Eintrag",
            cipher = cipher,
            onSuccess = { authenticatedCipher ->
                val (_, encPass) = CryptoManager.loadEncryptedDbPassword(this)!!
                val passBytes = authenticatedCipher.doFinal(encPass)
                dbPassword = passBytes.map { it.toInt().toChar() }.toCharArray()
                passBytes.fill(0)
                openDatabase()
                showEntryList()
            },
            onError = { msg ->
                val layout = buildRootLayout()
                layout.setPadding(48, 96, 48, 0)
                val tv = TextView(this).apply { text = "Authentication failed: $msg"; textSize = 16f }
                val btn = Button(this).apply { text = "Retry"; setOnClickListener { showBiometricUnlock() } }
                layout.addView(tv)
                layout.addView(btn)
                layout.addView(spacer())
                setContentView(layout)
            }
        )
    }

    private fun openDatabase() {
        dbHelper = DatabaseHelper.getInstance(this, dbPassword!!)
    }

    // ── Entry List View ────────────────────────────────────────────────────────

    private fun showEntryList() {
        val layout = buildRootLayout()

        // Header bar
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 48, 16, 8)
        }
        val tvTitle = TextView(this).apply {
            text = "Eintrag"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val menuBtn = Button(this).apply {
            text = "⋮"
            textSize = 20f
            background = null
            setOnClickListener { showOptionsMenu(it) }
        }
        header.addView(tvTitle)
        header.addView(menuBtn)

        val addBtn = Button(this).apply {
            text = "+ New Entry"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(24, 0, 24, 12) }
            setOnClickListener { showAddEntry() }
        }

        val listView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            dividerHeight = 1
        }

        layout.addView(header)
        layout.addView(addBtn)
        layout.addView(listView)
        layout.addView(spacer())
        setContentView(layout)

        refreshList(listView)
    }

    private fun refreshList(listView: ListView) {
        val entries = dbHelper!!.getAllEntries()
        if (entries.isEmpty()) {
            listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
                listOf("No entries yet. Tap '+ New Entry' to begin."))
            return
        }
        val adapter = object : ArrayAdapter<Entry>(this, android.R.layout.simple_list_item_2, android.R.id.text1, entries) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val e = entries[position]
                view.findViewById<TextView>(android.R.id.text1).apply {
                    text = e.title
                    textSize = 16f
                }
                view.findViewById<TextView>(android.R.id.text2).apply {
                    text = "${e.date}  •  ${e.text.take(80).replace('\n', ' ')}"
                }
                return view
            }
        }
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, pos, _ ->
            showEntryDetail(entries[pos])
        }
        listView.setOnItemLongClickListener { _, _, pos, _ ->
            confirmDelete(entries[pos]) { refreshList(listView) }
            true
        }
    }

    private fun showOptionsMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Export Database")
        popup.menu.add(0, 2, 1, "Import Database")
        popup.menu.add(0, 3, 2, "Change Export Password")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> initiateExport()
                2 -> initiateImport()
                3 -> changeExportPassword()
            }
            true
        }
        popup.show()
    }

    // ── Add Entry View ─────────────────────────────────────────────────────────

    private fun showAddEntry() {
        val layout = buildRootLayout()
        layout.setPadding(24, 48, 24, 0)

        val tvHeader = TextView(this).apply {
            text = "New Entry"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        val etDate = EditText(this).apply {
            hint = "Date (YYYY-MM-DD)"
            setText(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
        }
        val etTitle = EditText(this).apply {
            hint = "Title"
            setPadding(paddingLeft, 16, paddingRight, 16)
        }
        val etText = EditText(this).apply {
            hint = "Text"
            minLines = 6
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }
        val btnCancel = Button(this).apply {
            text = "Cancel"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showEntryList() }
        }
        val btnSave = Button(this).apply {
            text = "Save"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val date = etDate.text.toString().trim()
                val title = etTitle.text.toString().trim()
                val text = etText.text.toString().trim()
                if (date.isBlank() || title.isBlank()) {
                    toast("Date and title are required")
                    return@setOnClickListener
                }
                dbHelper!!.addEntry(Entry(date = date, title = title, text = text))
                showEntryList()
            }
        }
        btnRow.addView(btnCancel)
        btnRow.addView(btnSave)

        layout.addView(tvHeader)
        layout.addView(etDate)
        layout.addView(etTitle)
        layout.addView(etText)
        layout.addView(btnRow)
        layout.addView(spacer())
        setContentView(layout)
    }

    // ── Entry Detail View ──────────────────────────────────────────────────────

    private fun showEntryDetail(entry: Entry) {
        val layout = buildRootLayout()
        layout.setPadding(24, 48, 24, 0)

        val tvDate = TextView(this).apply {
            text = entry.date; textSize = 13f; alpha = 0.55f
        }
        val tvTitle = TextView(this).apply {
            text = entry.title; textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 8, 0, 20)
        }
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        val tvText = TextView(this).apply {
            text = entry.text; textSize = 16f; setLineSpacing(0f, 1.4f)
        }
        scrollView.addView(tvText)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }
        val btnBack = Button(this).apply {
            text = "← Back"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showEntryList() }
        }
        val btnDelete = Button(this).apply {
            text = "Delete"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { confirmDelete(entry) { showEntryList() } }
        }
        btnRow.addView(btnBack)
        btnRow.addView(btnDelete)

        layout.addView(tvDate)
        layout.addView(tvTitle)
        layout.addView(scrollView)
        layout.addView(btnRow)
        layout.addView(spacer())
        setContentView(layout)
    }

    private fun confirmDelete(entry: Entry, onDeleted: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Delete \"${entry.title}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper!!.deleteEntry(entry.id)
                onDeleted()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Export ─────────────────────────────────────────────────────────────────

    private fun initiateExport() {
        if (!CryptoManager.hasExportPassword(this)) {
            changeExportPassword { startExportFilePicker() }
            return
        }
        startExportFilePicker()
    }

    private fun startExportFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "eintrag_backup_${
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            }.enc")
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQ_EXPORT)
    }

    private fun performExport(uri: Uri) {
        val (iv, encPass) = CryptoManager.loadEncryptedExportPassword(this) ?: run {
            toast("No export password set"); return
        }
        val cipher = CryptoManager.decryptDbPasswordCipher(iv)
        CryptoManager.showBiometricPrompt(
            activity = this,
            title = "Authenticate to Export",
            cipher = cipher,
            onSuccess = { authenticatedCipher ->
                val passBytes = authenticatedCipher.doFinal(encPass)
                val exportPassword = passBytes.map { it.toInt().toChar() }.toCharArray()
                passBytes.fill(0)
                try {
                    // Flush DB to disk, then encrypt
                    DatabaseHelper.clearInstance()
                    val dbFile = this.getDatabasePath(DatabaseHelper.DB_NAME)
                    val tmpFile = File(cacheDir, "export_tmp.enc")
                    CryptoManager.encryptFile(dbFile, tmpFile, exportPassword)
                    exportPassword.fill('0')
                    // Re-open DB
                    openDatabase()
                    contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(tmpFile).use { it.copyTo(out) }
                    }
                    tmpFile.delete()
                    toast("Export successful")
                } catch (e: Exception) {
                    toast("Export failed: ${e.message}")
                    openDatabase()
                }
            },
            onError = { msg -> toast("Auth failed: $msg") }
        )
    }

    // ── Import ─────────────────────────────────────────────────────────────────

    private fun initiateImport() {
        AlertDialog.Builder(this)
            .setTitle("Import Database")
            .setMessage("Importing will replace ALL current data. Continue?")
            .setPositiveButton("Import") { _, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/octet-stream"
                }
                @Suppress("DEPRECATION")
                startActivityForResult(intent, REQ_IMPORT)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performImport(uri: Uri) {
        if (CryptoManager.hasExportPassword(this)) {
            val (iv, encPass) = CryptoManager.loadEncryptedExportPassword(this)!!
            val cipher = CryptoManager.decryptDbPasswordCipher(iv)
            CryptoManager.showBiometricPrompt(
                activity = this,
                title = "Authenticate to Import",
                cipher = cipher,
                onSuccess = { authenticatedCipher ->
                    val passBytes = authenticatedCipher.doFinal(encPass)
                    val exportPassword = passBytes.map { it.toInt().toChar() }.toCharArray()
                    passBytes.fill(0)
                    doImport(uri, exportPassword)
                },
                onError = { promptManualImportPassword(uri) }
            )
        } else {
            promptManualImportPassword(uri)
        }
    }

    private fun promptManualImportPassword(uri: Uri) {
        val et = EditText(this).apply {
            hint = "Export password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(48, 16, 48, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("Enter Export Password")
            .setView(et)
            .setPositiveButton("Import") { _, _ ->
                doImport(uri, et.text.toString().toCharArray())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun doImport(uri: Uri, password: CharArray) {
        try {
            val tmpEnc = File(cacheDir, "import_src.enc")
            val tmpDec = File(cacheDir, "import_dec.db")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmpEnc).use { input.copyTo(it) }
            }
            CryptoManager.decryptFile(tmpEnc, tmpDec, password)
            password.fill('0')
            tmpEnc.delete()

            DatabaseHelper.clearInstance()
            val dbDest = this.getDatabasePath(DatabaseHelper.DB_NAME)
            dbDest.parentFile?.mkdirs()
            tmpDec.copyTo(dbDest, overwrite = true)
            tmpDec.delete()

            openDatabase()
            showEntryList()
            toast("Import successful")
        } catch (e: Exception) {
            toast("Import failed — wrong password or corrupted file")
        }
    }

    // ── Export Password Management ─────────────────────────────────────────────

    private fun changeExportPassword(onDone: (() -> Unit)? = null) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 24, 64, 8)
        }
        val etPass = EditText(this).apply {
            hint = "New export password (min 8 chars)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etConfirm = EditText(this).apply {
            hint = "Confirm password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(etPass)
        container.addView(etConfirm)

        AlertDialog.Builder(this)
            .setTitle("Set Export Password")
            .setMessage("This password protects exported backup files and is stored securely on this device.")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val p1 = etPass.text.toString()
                val p2 = etConfirm.text.toString()
                if (p1.length < 8) { toast("Password must be at least 8 characters"); return@setPositiveButton }
                if (p1 != p2) { toast("Passwords do not match"); return@setPositiveButton }
                val cipher = CryptoManager.encryptDbPasswordCipher()
                CryptoManager.showBiometricPrompt(
                    activity = this,
                    title = "Authenticate to Save Password",
                    cipher = cipher,
                    onSuccess = { authenticatedCipher ->
                        CryptoManager.saveExportPassword(this, authenticatedCipher, p1.toByteArray())
                        toast("Export password saved securely")
                        onDone?.invoke()
                    },
                    onError = { msg -> toast("Failed to save: $msg") }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── UI Helpers ─────────────────────────────────────────────────────────────

    private fun buildRootLayout() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    /** 100px bottom spacer required on all views */
    private fun spacer() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun generateDbPassword(): CharArray {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()"
        val rng = SecureRandom()
        return CharArray(64) { chars[rng.nextInt(chars.length)] }
    }
}
