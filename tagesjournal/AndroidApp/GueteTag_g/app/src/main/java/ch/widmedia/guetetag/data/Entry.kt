package ch.widmedia.guetetag.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Repräsentiert einen Tagebucheintrag.
 *
 * @property id Eindeutige ID des Eintrags.
 * @property date Zeitstempel des Eintrags in Millisekunden.
 * @property rating Bewertung des Tages (1-10).
 * @property text Inhalt des Tagebucheintrags.
 */
@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val rating: Int,
    val text: String
)
