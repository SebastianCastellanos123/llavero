package io.github.sebastiancastellanos123.llavero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.sebastiancastellanos123.llavero.R

/**
 * Pantalla que reemplaza a toda la app mientras está bloqueada. La lista de cuentas
 * ni siquiera se compone detrás: así tampoco queda expuesta a servicios de accesibilidad.
 */
@Composable
fun LockScreen(onUnlockRequest: () -> Unit, errorMessage: String? = null) {
    // Pide la huella apenas aparece; si el usuario cancela, queda el botón.
    LaunchedEffect(Unit) { onUnlockRequest() }

    Surface(Modifier.fillMaxSize().testTag("lock")) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Text("Llavero está bloqueado", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Usa tu huella, tu rostro o el bloqueo de pantalla del teléfono para ver tus códigos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Button(onClick = onUnlockRequest, modifier = Modifier.fillMaxWidth().testTag("unlock")) {
                Text("Desbloquear")
            }
        }
    }
}
