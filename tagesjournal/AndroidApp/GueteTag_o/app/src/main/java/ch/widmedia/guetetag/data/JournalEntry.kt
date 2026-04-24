package ch.widmedia.guetetag.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val date: String,   // ISO format: yyyy-MM-dd
    val rating: Int,                // 1..10
    val text: String
)
