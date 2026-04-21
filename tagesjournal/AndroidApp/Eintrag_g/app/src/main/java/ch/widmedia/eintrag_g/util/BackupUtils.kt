package ch.widmedia.eintrag_g.util

import ch.widmedia.eintrag_g.data.entity.JournalEntry
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object BackupUtils {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, JournalEntry::class.java)
    private val adapter = moshi.adapter<List<JournalEntry>>(listType)

    fun encryptEntries(entries: List<JournalEntry>, password: CharArray): String {
        val json = adapter.toJson(entries)
        return EncryptionUtils.encrypt(json, password)
    }

    fun decryptEntries(encryptedData: String, password: CharArray): List<JournalEntry>? {
        return try {
            val json = EncryptionUtils.decrypt(encryptedData, password)
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
