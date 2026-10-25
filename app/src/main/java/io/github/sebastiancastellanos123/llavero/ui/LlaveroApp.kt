package io.github.sebastiancastellanos123.llavero.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.sebastiancastellanos123.llavero.R
import io.github.sebastiancastellanos123.llavero.backup.BackupCodec
import io.github.sebastiancastellanos123.llavero.scan.decodeQrFromImage
import io.github.sebastiancastellanos123.llavero.security.SecurityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate

private enum class Screen { List, Scan, Add, Settings }

private enum class BackupDialog { Export, Import }

/** Lee el archivo completo, pero devuelve null si pasa de [maxBytes]. */
private fun readLimited(context: Context, uri: Uri, maxBytes: Int): ByteArray? {
    context.contentResolver.openInputStream(uri)?.use { input ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > maxBytes) return null
        }
        return out.toByteArray()
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlaveroApp(
    security: SecurityViewModel,
    authenticator: Authenticator,
    viewModel: AccountsViewModel = viewModel(factory = AccountsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingForm by viewModel.pendingForm.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.List) }
    var showAddOptions by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun goToList() {
        viewModel.clearPendingForm()
        screen = Screen.List
    }

    // Respaldo: primero se cifra en memoria y después se elige dónde guardarlo,
    // así la contraseña no tiene que esperar a que el usuario navegue entre carpetas.
    var backupDialog by remember { mutableStateOf<BackupDialog?>(null) }
    var backupBusy by remember { mutableStateOf(false) }
    var backupError by remember { mutableStateOf<String?>(null) }
    var encryptedBackup by remember { mutableStateOf<ByteArray?>(null) }
    var backupToImport by remember { mutableStateOf<ByteArray?>(null) }

    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val data = encryptedBackup
        encryptedBackup = null
        if (uri == null || data == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(data) } }.isSuccess
            }
            snackbar.showSnackbar(
                if (saved) "Respaldo guardado. Sin la contraseña no se puede abrir: guárdala en un lugar seguro."
                else "No se pudo guardar el respaldo"
            )
        }
    }

    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val data = withContext(Dispatchers.IO) {
                runCatching { readLimited(context, uri, BackupCodec.MAX_FILE_BYTES) }.getOrNull()
            }
            if (data == null) {
                snackbar.showSnackbar("No se pudo leer el archivo o es demasiado grande")
            } else {
                backupToImport = data
                backupError = null
                backupDialog = BackupDialog.Import
            }
        }
    }

    when (backupDialog) {
        BackupDialog.Export -> PasswordDialog(
            title = "Exportar respaldo",
            message = "Elige una contraseña para el archivo. Si la olvidas, el respaldo no se puede recuperar.",
            confirmLabel = "Cifrar y guardar",
            confirm = true,
            busy = backupBusy,
            error = null,
            onConfirm = { password ->
                backupBusy = true
                scope.launch {
                    encryptedBackup = viewModel.createBackup(password)
                    backupBusy = false
                    backupDialog = null
                    if (encryptedBackup != null) saveBackup.launch("llavero-respaldo-${LocalDate.now()}.llavero")
                }
            },
            onDismiss = { backupDialog = null },
        )
        BackupDialog.Import -> PasswordDialog(
            title = "Importar respaldo",
            message = "Escribe la contraseña con la que se creó el respaldo.",
            confirmLabel = "Importar",
            confirm = false,
            busy = backupBusy,
            error = backupError,
            onConfirm = { password ->
                val data = backupToImport
                if (data != null) {
                    backupBusy = true
                    scope.launch {
                        when (val result = viewModel.importBackup(data, password)) {
                            AccountsViewModel.ImportResult.Done -> backupDialog = null
                            AccountsViewModel.ImportResult.WrongPassword ->
                                backupError = "Contraseña incorrecta o el archivo fue alterado"
                            is AccountsViewModel.ImportResult.Invalid -> {
                                backupDialog = null
                                snackbar.showSnackbar(result.message)
                            }
                        }
                        backupBusy = false
                        if (backupDialog == null) backupToImport = null
                    }
                }
            },
            onDismiss = { backupDialog = null; backupToImport = null },
        )
        null -> Unit
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { decodeQrFromImage(context, uri) }.getOrNull()
            }
            when {
                text == null -> snackbar.showSnackbar("No se encontró un código QR en esa imagen")
                viewModel.onQrScanned(text) -> screen = Screen.Add
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    when (screen) {
        Screen.Scan -> {
            BackHandler { goToList() }
            ScanScreen(
                onQrFound = { text -> viewModel.onQrScanned(text).also { if (it) screen = Screen.Add } },
                onManualEntry = { screen = Screen.Add },
                onBack = { goToList() },
            )
            // Los avisos de QR inválido se muestran encima de la cámara.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { SnackbarHost(snackbar) }
            return
        }
        Screen.Add -> {
            BackHandler { goToList() }
            AddAccountScreen(
                onSave = { viewModel.add(it); goToList() },
                onBack = { goToList() },
                scannedForm = pendingForm,
            )
            return
        }
        Screen.Settings -> {
            BackHandler { goToList() }
            val lockAvailable by security.lockAvailable.collectAsStateWithLifecycle()
            val lockEnabled by security.lockEnabled.collectAsStateWithLifecycle()
            val blockScreenshots by security.blockScreenshots.collectAsStateWithLifecycle()

            // Bajar la protección exige verificar que quien tiene el teléfono es el dueño.
            fun confirmThen(title: String, action: () -> Unit) {
                if (lockAvailable) {
                    authenticator.authenticate(title, "Confirma que eres tú", action) { scope.launch { snackbar.showSnackbar(it) } }
                } else {
                    action()
                }
            }

            SettingsScreen(
                onExport = {
                    if ((state as? AccountsState.Ready)?.accounts.isNullOrEmpty()) {
                        scope.launch { snackbar.showSnackbar("No hay cuentas para exportar") }
                    } else {
                        // Exportar saca los secretos del teléfono: se confirma que es el dueño.
                        confirmThen("Exportar respaldo") { backupDialog = BackupDialog.Export }
                    }
                },
                onImport = { openBackup.launch(arrayOf("*/*")) },
                lockAvailable = lockAvailable,
                lockEnabled = lockEnabled,
                blockScreenshots = blockScreenshots,
                onLockChange = { enable ->
                    if (enable) security.setLockEnabled(true)
                    else confirmThen("Desactivar el bloqueo") { security.setLockEnabled(false) }
                },
                onBlockScreenshotsChange = { block ->
                    if (block) security.setBlockScreenshots(true)
                    else confirmThen("Permitir capturas de pantalla") { security.setBlockScreenshots(false) }
                },
                onBack = { goToList() },
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { SnackbarHost(snackbar) }
            return
        }
        Screen.List -> Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Llavero") },
                actions = {
                    IconButton(onClick = { screen = Screen.Settings }) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = "Ajustes")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (state is AccountsState.Ready) {
                ExtendedFloatingActionButton(
                    onClick = { showAddOptions = true },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text("Agregar") },
                    modifier = Modifier.testTag("add"),
                )
            }
        },
    ) { padding ->
        when (val current = state) {
            AccountsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is AccountsState.Failed -> Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(current.message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
            }
            is AccountsState.Ready -> AccountList(
                accounts = current.accounts,
                nowSeconds = rememberEpochSeconds(),
                onCopy = { code ->
                    copySensitive(context, code)
                    // Desde Android 13 el sistema ya muestra su propio aviso al copiar.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        scope.launch { snackbar.showSnackbar("Código copiado") }
                    }
                },
                onDelete = viewModel::delete,
                contentPadding = padding,
            )
        }
    }

    if (showAddOptions) {
        ModalBottomSheet(onDismissRequest = { showAddOptions = false }) {
            AddOption(R.drawable.ic_qr_scanner, "Escanear código QR", "Con la cámara") {
                showAddOptions = false
                screen = Screen.Scan
            }
            AddOption(R.drawable.ic_image, "Elegir imagen con QR", "Por ejemplo, una captura de pantalla") {
                showAddOptions = false
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            AddOption(R.drawable.ic_keyboard, "Ingresar clave manualmente", "Si el servicio te muestra la clave como texto") {
                showAddOptions = false
                screen = Screen.Add
            }
            Box(Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun AddOption(icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Segundo Unix actual; se actualiza justo al cambiar de segundo. */
@Composable
private fun rememberEpochSeconds(): Long {
    val now by produceState(System.currentTimeMillis() / 1000) {
        while (true) {
            delay(1000 - System.currentTimeMillis() % 1000)
            value = System.currentTimeMillis() / 1000
        }
    }
    return now
}

/**
 * Marca el texto como sensible: Android 13+ no lo muestra en la vista previa
 * del portapapeles ni en las sugerencias del teclado.
 */
private fun copySensitive(context: Context, text: String) {
    val clip = ClipData.newPlainText("Código de verificación", text)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
}
