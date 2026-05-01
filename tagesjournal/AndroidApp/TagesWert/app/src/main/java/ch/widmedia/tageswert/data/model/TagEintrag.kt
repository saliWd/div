package ch.widmedia.tageswert.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eintraege")
data class TagEintrag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val datum: String,          // ISO date string: "YYYY-MM-DD"
    val bewertung: Int,         // 1-10
    val notizen: String = "",
    val erstelltAm: Long = System.currentTimeMillis(),
    val geaendertAm: Long = System.currentTimeMillis(),
)
