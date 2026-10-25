package io.github.sebastiancastellanos123.llavero.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.sebastiancastellanos123.llavero.R

/**
 * Los cambios que bajan la protección (quitar el bloqueo, permitir capturas) los
 * confirma [LlaveroApp] con huella o PIN antes de aplicarlos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    lockAvailable: Boolean,
    lockEnabled: Boolean,
    blockScreenshots: Boolean,
    onLockChange: (Boolean) -> Unit,
    onBlockScreenshotsChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Text(
                "Seguridad",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )
            ListItem(
                headlineContent = { Text("Bloquear con huella o PIN") },
                supportingContent = {
                    Text(
                        if (lockAvailable) {
                            "Pide verificación al abrir la app y después de 1 minuto en segundo plano."
                        } else {
                            "Configura un bloqueo de pantalla en el teléfono para usar esta opción."
                        }
                    )
                },
                trailingContent = {
                    Switch(
                        checked = lockEnabled && lockAvailable,
                        onCheckedChange = onLockChange,
                        enabled = lockAvailable,
                        modifier = Modifier.testTag("lockSwitch"),
                    )
                },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Bloquear capturas de pantalla") },
                supportingContent = {
                    Text("Impide capturas y grabaciones de pantalla, y oculta los códigos en la vista de apps recientes.")
                },
                trailingContent = {
                    Switch(
                        checked = blockScreenshots,
                        onCheckedChange = onBlockScreenshotsChange,
                        modifier = Modifier.testTag("screenshotSwitch"),
                    )
                },
            )
        }
    }
}
