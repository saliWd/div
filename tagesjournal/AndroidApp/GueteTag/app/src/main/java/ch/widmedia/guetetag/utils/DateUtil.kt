package ch.widmedia.guetetag.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtil {
    val ISO_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val DISPLAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun lokalDatum(isoDate: String): String {
        return try {
            LocalDate.parse(isoDate, ISO_FORMAT).format(DISPLAY_FORMAT)
        } catch (_: Exception) {
            isoDate
        }
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
}
