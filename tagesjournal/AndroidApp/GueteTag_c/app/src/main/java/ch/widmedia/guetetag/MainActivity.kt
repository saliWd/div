
package ch.widmedia.guetetag

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import ch.widmedia.guetetag.ui.MainScreen
import ch.widmedia.guetetag.ui.theme.GueteTagTheme
import ch.widmedia.guetetag.security.BiometricGate
import ch.widmedia.guetetag.db.DatabaseProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BiometricGate.authenticate(this) {
            val dao = DatabaseProvider.get(this).entryDao()
            setContent {
                val snackbarHostState = remember { SnackbarHostState() }
                GueteTagTheme {
                    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
                        MainScreen(dao, snackbarHostState)
                    }
                }
            }
        }
    }
}
