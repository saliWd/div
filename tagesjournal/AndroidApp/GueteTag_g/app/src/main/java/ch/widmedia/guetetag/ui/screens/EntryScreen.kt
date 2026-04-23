package ch.widmedia.guetetag.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ch.widmedia.guetetag.data.AppDatabase
import ch.widmedia.guetetag.data.Entry
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(dateMillis: Long, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val entryDao = database.entryDao()

    var rating by remember { mutableFloatStateOf(5f) }
    var text by remember { mutableStateOf("") }
    var existingEntry by remember { mutableStateOf<Entry?>(null) }

    val formattedDate = remember(dateMillis) {
        val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        date.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN))
    }

    LaunchedEffect(dateMillis) {
        val startOfDay = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
        
        // Simpler check for today's entry since our DAO handles range or we can just fetch all and filter
        // For simplicity, let's just get all entries and find one that matches the date
        entryDao.getAllEntries().collect { entries ->
            val entry = entries.find { 
                Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == 
                Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            }
            if (entry != null) {
                existingEntry = entry
                rating = entry.rating.toFloat()
                text = entry.text
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eintrag bearbeiten", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (existingEntry != null) {
                        IconButton(onClick = {
                            scope.launch {
                                entryDao.deleteEntry(existingEntry!!)
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Löschen")
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val newEntry = Entry(
                                id = existingEntry?.id ?: 0,
                                date = dateMillis,
                                rating = rating.toInt(),
                                text = text
                            )
                            if (existingEntry == null) {
                                entryDao.insertEntry(newEntry)
                            } else {
                                entryDao.updateEntry(newEntry)
                            }
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Rounded.Save, contentDescription = "Speichern")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(bottom = 80.dp) // 10% bottom padding
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Text(
                text = "Wie war dein Tag? (${rating.toInt()}/10)",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = rating,
                onValueChange = { rating = it },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Deine Gedanken...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
