package ch.widmedia.guetetag.data.db

import androidx.room.*
import ch.widmedia.guetetag.data.model.TagEintrag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagEintragDao {

    @Query("SELECT * FROM eintraege ORDER BY datum DESC")
    fun alleEintraege(): Flow<List<TagEintrag>>

    @Query("SELECT * FROM eintraege WHERE datum = :datum LIMIT 1")
    suspend fun eintraegFuerDatum(datum: String): TagEintrag?

    @Query("SELECT datum FROM eintraege WHERE datum >= :vonDatum AND datum <= :bisDatum")
    suspend fun datumMitEintrag(vonDatum: String, bisDatum: String): List<String>

    @Query("SELECT * FROM eintraege WHERE id = :id LIMIT 1")
    suspend fun eintragNachId(id: Long): TagEintrag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegen(eintrag: TagEintrag): Long

    @Update
    suspend fun aktualisieren(eintrag: TagEintrag)

    @Delete
    suspend fun loeschen(eintrag: TagEintrag)

    @Query("DELETE FROM eintraege")
    suspend fun alleLoeschen()

    @Query("SELECT * FROM eintraege ORDER BY datum ASC")
    suspend fun alleEintraegeListe(): List<TagEintrag>
}
