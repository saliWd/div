package ch.widmedia.eintrag_g

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ch.widmedia.eintrag_g.security.BiometricPromptWrapper
import ch.widmedia.eintrag_g.ui.screen.EntryEditorScreen
import ch.widmedia.eintrag_g.ui.screen.EntryListScreen
import ch.widmedia.eintrag_g.ui.theme.Eintrag_gTheme
import ch.widmedia.eintrag_g.ui.viewmodel.JournalViewModel
import java.io.OutputStreamWriter

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Eintrag_gTheme {
                var isAuthenticated by remember { mutableStateOf(false) }
                val biometricWrapper = remember {
                    BiometricPromptWrapper(
                        activity = this,
                        onAuthSuccess = { isAuthenticated = true },
                        onAuthError = { _, errString ->
                            Toast.makeText(this, "Fehler: $errString", Toast.LENGTH_SHORT).show()
                        },
                        onAuthFailed = {
                            Toast.makeText(this, "Authentifizierung fehlgeschlagen", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                LaunchedEffect(Unit) {
                    if (biometricWrapper.canAuthenticate()) {
                        biometricWrapper.authenticate()
                    } else {
                        // Fallback if no biometrics set up, for now just allow access
                        isAuthenticated = true
                    }
                }

                if (isAuthenticated) {
                    val viewModel: JournalViewModel = viewModel()
                    MainNavigation(viewModel)
                } else {
                    LockScreen()
                }
            }
        }
    }
}

@Composable
fun MainNavigation(viewModel: JournalViewModel) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            viewModel.exportEntries(
                onSuccess = { data ->
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        OutputStreamWriter(stream).use { writer ->
                            writer.write(data)
                        }
                    }
                    Toast.makeText(context, "Export erfolgreich", Toast.LENGTH_SHORT).show()
                },
                onError = { e ->
                    Toast.makeText(context, "Export Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                reader.readText()
            }
            if (content != null) {
                viewModel.importEntries(
                    encryptedData = content,
                    onSuccess = {
                        Toast.makeText(context, "Import erfolgreich", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            EntryListScreen(
                viewModel = viewModel,
                onEntryClick = { id -> navController.navigate("editor/$id") },
                onAddClick = { navController.navigate("editor") },
                onExportClick = { 
                    createDocumentLauncher.launch("eintrag_backup.ejrn")
                },
                onImportClick = {
                    openDocumentLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                }
            )
        }
        // ... rest of the composables
        composable(
            route = "editor?id={id}",
            arguments = listOf(navArgument("id") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null 
            })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            EntryEditorScreen(
                viewModel = viewModel,
                entryId = id,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "editor/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id")
            EntryEditorScreen(
                viewModel = viewModel,
                entryId = id,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun LockScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Bitte authentifizieren Sie sich...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    Eintrag_gTheme {
        LockScreen()
    }
}
