package io.github.sebastiancastellanos123.llavero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.sebastiancastellanos123.llavero.R
import io.github.sebastiancastellanos123.llavero.backup.BackupCodec

/**
 * Pide la contraseña del respaldo. Con [confirm] pide escribirla dos veces
 * (al exportar, donde un error de tipeo dejaría el respaldo inservible).
 *
 * La contraseña sale como CharArray para que quien la use pueda borrarla.
 * El campo de texto de Compose guarda un String que no se puede borrar de memoria;
 * por eso se vacía apenas se confirma.
 */
@Composable
fun PasswordDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirm: Boolean,
    busy: Boolean,
    error: String?,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        localError = when {
            password.length < BackupCodec.MIN_PASSWORD_LENGTH ->
                "Usa al menos ${BackupCodec.MIN_PASSWORD_LENGTH} caracteres"
            confirm && password != repeat -> "Las contraseñas no coinciden"
            else -> null
        }
        if (localError == null) {
            val chars = password.toCharArray()
            password = ""
            repeat = ""
            onConfirm(chars)
        }
    }

    val transformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()
    val keyboard = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; localError = null },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = transformation,
                    keyboardOptions = keyboard,
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                                contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("password"),
                )
                if (confirm) {
                    OutlinedTextField(
                        value = repeat,
                        onValueChange = { repeat = it; localError = null },
                        label = { Text("Repite la contraseña") },
                        singleLine = true,
                        enabled = !busy,
                        visualTransformation = transformation,
                        keyboardOptions = keyboard,
                        modifier = Modifier.fillMaxWidth().testTag("repeat"),
                    )
                }
                (localError ?: error)?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Procesando…")
                }
            } else {
                TextButton(onClick = ::submit, modifier = Modifier.testTag("passwordConfirm")) { Text(confirmLabel) }
            }
        },
        dismissButton = {
            if (!busy) TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
