package ch.widmedia.guetetag.utils

import android.content.Context
import android.net.Uri
import ch.widmedia.guetetag.data.model.TagEintrag
import ch.widmedia.guetetag.security.SecurityManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ExportImportUtil {

    private val gson = Gson()
    private const val EXPORT_MAGIC = "TAGESWERT_EXPORT_V1"

    data class ExportData(
        val magic: String = EXPORT_MAGIC,
        val version: Int = 1,
        val exportedAt: Long = System.currentTimeMillis(),
        val eintraege: List<TagEintrag>
    )

    fun getEncryptedExportData(
        context: Context,
        eintraege: List<TagEintrag>,
        password: String,
    ): ByteArray {
        val exportData = ExportData(eintraege = eintraege)
        val json = gson.toJson(exportData)
        val jsonBytes = json.toByteArray(Charsets.UTF_8)

        // Save password to secure storage
        SecurityManager.saveExportPassword(context, password)

        // Encrypt with KeyStore key (password is used as IV seed)
        return SecurityManager.encryptExportData(jsonBytes, password)
    }

    fun importieren(
        context: Context,
        uri: Uri,
        password: String
    ): List<TagEintrag> {
        val encryptedData = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: throw IllegalArgumentException("Datei konnte nicht gelesen werden")

        val decrypted = SecurityManager.decryptExportData(encryptedData, password)
        val json = String(decrypted, Charsets.UTF_8)

        val type = object : TypeToken<ExportData>() {}.type
        val exportData: ExportData = gson.fromJson(json, type)

        if (exportData.magic != EXPORT_MAGIC) {
            throw IllegalArgumentException("Ungültiges Dateiformat")
        }

        return exportData.eintraege
    }
}
