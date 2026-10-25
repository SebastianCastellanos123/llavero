package io.github.sebastiancastellanos123.llavero.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.sebastiancastellanos123.llavero.R
import io.github.sebastiancastellanos123.llavero.scan.QrCameraPreview

/**
 * @param onQrFound devuelve true si el QR sirvió; si no, se sigue escaneando
 *   y ese mismo texto se ignora para no repetir el aviso de error.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onQrFound: (String) -> Boolean,
    onManualEntry: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }
    val rejected = remember { mutableSetOf<String>() }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        denied = !it
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear código QR") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                granted && !cameraFailed -> {
                    QrCameraPreview(
                        onQrFound = { text ->
                            if (text !in rejected && !onQrFound(text)) rejected += text
                        },
                        onCameraError = { cameraFailed = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(260.dp)
                            .border(3.dp, Color.White, RoundedCornerShape(24.dp)),
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                    ) {
                        Text(
                            "Apunta al código QR que muestra el servicio",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
                else -> CameraUnavailable(
                    message = when {
                        cameraFailed -> "No se pudo abrir la cámara de este teléfono."
                        else -> "Llavero usa la cámara solo para leer el código QR. " +
                            "Las imágenes se analizan en el teléfono y no se guardan."
                    },
                    showPermissionButtons = !cameraFailed,
                    denied = denied,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        )
                    },
                    onManualEntry = onManualEntry,
                )
            }
        }
    }
}

@Composable
private fun CameraUnavailable(
    message: String,
    showPermissionButtons: Boolean,
    denied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onManualEntry: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (showPermissionButtons) {
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text("Permitir cámara") }
            // Si el usuario marcó "no volver a preguntar", el sistema ya no muestra el diálogo:
            // solo queda cambiarlo en los ajustes.
            if (denied) {
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("Abrir ajustes") }
            }
        }
        TextButton(onClick = onManualEntry) { Text("Ingresar la clave manualmente") }
    }
}
