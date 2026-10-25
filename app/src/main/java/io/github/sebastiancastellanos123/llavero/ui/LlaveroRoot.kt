package io.github.sebastiancastellanos123.llavero.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.sebastiancastellanos123.llavero.security.SecurityViewModel

/** Pide al sistema que verifique al usuario (huella, rostro o PIN). */
fun interface Authenticator {
    fun authenticate(title: String, subtitle: String, onSuccess: () -> Unit, onError: (String) -> Unit)
}

/**
 * Raíz de la interfaz: decide si se muestra la app o la pantalla de bloqueo,
 * y aplica la protección contra capturas de pantalla.
 */
@Composable
fun LlaveroRoot(
    authenticator: Authenticator,
    setScreenSecure: (Boolean) -> Unit,
    security: SecurityViewModel = viewModel(factory = SecurityViewModel.Factory),
) {
    val locked by security.locked.collectAsStateWithLifecycle()
    val blockScreenshots by security.blockScreenshots.collectAsStateWithLifecycle()
    var lockError by remember { mutableStateOf<String?>(null) }

    LifecycleStartEffect(security) {
        security.onForeground()
        onStopOrDispose { security.onBackground() }
    }

    LaunchedEffect(blockScreenshots) { setScreenSecure(blockScreenshots) }

    if (locked) {
        LockScreen(
            onUnlockRequest = {
                authenticator.authenticate(
                    "Desbloquear Llavero",
                    "Verifica que eres tú para ver tus códigos",
                    { lockError = null; security.unlock() },
                    { lockError = it },
                )
            },
            errorMessage = lockError,
        )
    } else {
        LlaveroApp(security = security, authenticator = authenticator)
    }
}
