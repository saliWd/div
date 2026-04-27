package ch.widmedia.guetetag.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtil {
    val ISO_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val DISPLAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun heute(): String = LocalDate.now().format(ISO_FORMAT)

    fun lokalDatum(isoDate: String): String {
        return try {
            LocalDate.parse(isoDate, ISO_FORMAT).format(DISPLAY_FORMAT)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun letzte14Tage(): List<LocalDate> {
        val today = LocalDate.now()
        return (13 downTo 0).map { today.minusDays(it.toLong()) }
    }

    fun kalenderWochen(): List<LocalDate> {
        val today = LocalDate.now()
        val diff = today.dayOfWeek.value - 1
        val currentMonday = today.minusDays(diff.toLong())
        val lastMonday = currentMonday.minusDays(7)
        return (0..13).map { lastMonday.plusDays(it.toLong()) }
    }

    fun toIso(date: LocalDate): String = date.format(ISO_FORMAT)

    fun wochentag(date: LocalDate): String {
        return when (date.dayOfWeek.value) {
            1 -> "Mo"
            2 -> "Di"
            3 -> "Mi"
            4 -> "Do"
            5 -> "Fr"
            6 -> "Sa"
            7 -> "So"
            else -> ""
        }
    }

    fun monatName(monat: Int): String {
        return when (monat) {
            1 -> "Januar"
            2 -> "Februar"
            3 -> "März"
            4 -> "April"
            5 -> "Mai"
            6 -> "Juni"
            7 -> "Juli"
            8 -> "August"
            9 -> "September"
            10 -> "Oktober"
            11 -> "November"
            12 -> "Dezember"
            else -> ""
        }
    }
}
