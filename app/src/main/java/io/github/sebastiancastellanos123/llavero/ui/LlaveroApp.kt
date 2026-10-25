package io.github.sebastiancastellanos123.llavero.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.sebastiancastellanos123.llavero.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlaveroApp(viewModel: AccountsViewModel = viewModel(factory = AccountsViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    if (adding) {
        BackHandler { adding = false }
        AddAccountScreen(
            onSave = { viewModel.add(it); adding = false },
            onBack = { adding = false },
        )
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Llavero") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (state is AccountsState.Ready) {
                ExtendedFloatingActionButton(
                    onClick = { adding = true },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text("Agregar") },
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
