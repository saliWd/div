package ch.widmedia.tageswert.data.repository

import ch.widmedia.tageswert.data.db.TagEintragDao
import ch.widmedia.tageswert.data.model.TagEintrag
import kotlinx.coroutines.flow.Flow

class EintragRepository(private val dao: TagEintragDao) {

    fun alleEintraege(): Flow<List<TagEintrag>> = dao.alleEintraege()

    suspend fun eintraegFuerDatum(datum: String): TagEintrag? =
        dao.eintraegFuerDatum(datum)

    suspend fun datumMitEintrag(vonDatum: String, bisDatum: String): List<String> =
        dao.datumMitEintrag(vonDatum, bisDatum)

    suspend fun speichern(eintrag: TagEintrag): Long {
        return if (eintrag.id == 0L) {
            dao.einfuegen(eintrag)
        } else {
            dao.aktualisieren(eintrag.copy(geaendertAm = System.currentTimeMillis()))
            eintrag.id
        }
    }

    suspend fun loeschen(eintrag: TagEintrag) = dao.loeschen(eintrag)

    suspend fun alleLoeschen() = dao.alleLoeschen()

    suspend fun alleEintraegeListe(): List<TagEintrag> = dao.alleEintraegeListe()
}
