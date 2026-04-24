package ch.widmedia.guetetag.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.widmedia.guetetag.ui.theme.Gold
import ch.widmedia.guetetag.ui.theme.GoldLight
import ch.widmedia.guetetag.ui.theme.Plum
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val MONTH_FORMATTER      = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)
private val SHORT_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM",      Locale.GERMAN)

private val DAY_LABELS = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

/**
 * Calendar displaying the last 14 days starting from the Monday on or before
 * today-13. Days with existing entries are highlighted in gold, days in the
 * 14-day range without entries are shown in a muted plum tint. Days outside
 * the range are greyed out and non-clickable.
 *
 * The composable fills all vertical space given to it by its parent (use a
 * weight modifier from the parent to achieve the 40 % height constraint).
 */
@Composable
fun DiaryCalendar(
    entryDates: Set<String>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val rangeStart = remember(today) { today.minusDays(13) }
    val gridStart  = remember(rangeStart) {
        rangeStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val gridEnd = remember(today) {
        today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    }

    val weeks: List<List<LocalDate>> = remember(gridStart, gridEnd) {
        buildList {
            var current = gridStart
            while (!current.isAfter(gridEnd)) {
                add((0 until 7).map { current.plusDays(it.toLong()) })
                current = current.plusDays(7)
            }
        }
    }

    val headerText = remember(rangeStart, today) {
        val s = SHORT_MONTH_FORMATTER.format(rangeStart).replaceFirstChar { it.uppercase() }
        val e = SHORT_MONTH_FORMATTER.format(today).replaceFirstChar { it.uppercase() }
        val year = today.year
        if (s == e) "$s $year" else "$s / $e $year"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Month / Year header
        Text(
            text  = headerText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Day-name headers
        Row(modifier = Modifier.fillMaxWidth()) {
            DAY_LABELS.forEach { label ->
                Text(
                    text      = label,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(
            modifier  = Modifier.padding(vertical = 6.dp),
            color     = MaterialTheme.colorScheme.outlineVariant
        )

        // Calendar grid – distributes remaining height equally among rows
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            weeks.forEach { week ->
                Row(
                    modifier             = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    week.forEach { date ->
                        val inRange  = !date.isBefore(rangeStart) && !date.isAfter(today)
                        val hasEntry = entryDates.contains(date.toString())
                        CalendarDayCell(
                            day      = date.dayOfMonth,
                            inRange  = inRange,
                            hasEntry = hasEntry,
                            onClick  = { if (inRange) onDayClick(date) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    inRange: Boolean,
    hasEntry: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor: Color = when {
        !inRange && !hasEntry -> Color.Transparent
        hasEntry              -> Gold
        else                  -> Plum.copy(alpha = 0.12f)
    }
    val textColor: Color = when {
        !inRange  -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
        hasEntry  -> Color.White
        else      -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    }
    val weight = if (hasEntry) FontWeight.Bold else FontWeight.Normal

    Box(
        modifier = modifier
            .padding(3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(if (inRange) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = day.toString(),
            style      = MaterialTheme.typography.bodyMedium.copy(fontWeight = weight),
            color      = textColor,
            textAlign  = TextAlign.Center
        )
    }
}
