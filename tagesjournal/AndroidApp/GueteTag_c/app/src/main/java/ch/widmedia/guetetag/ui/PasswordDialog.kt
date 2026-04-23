
package ch.widmedia.guetetag.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun PasswordDialog(title: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var pwd by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = pwd, onValueChange = { pwd = it }, label = { Text("Passwort") }) },
        confirmButton = { Button(onClick = { onConfirm(pwd) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
