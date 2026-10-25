package io.github.sebastiancastellanos123.llavero.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decide cuándo la app debe pedir huella o PIN.
 *
 * - Al abrir la app (proceso nuevo) empieza bloqueada.
 * - Si sale a segundo plano y vuelve después de [graceMillis], se bloquea de nuevo.
 *   El margen evita pedir la huella al volver del selector de fotos o del
 *   diálogo de permisos, que también sacan la app de primer plano.
 *
 * [now] debe ser un reloj monotónico (SystemClock.elapsedRealtime): si se usara la
 * hora del teléfono, bastaría con atrasarla para saltarse el bloqueo.
 */
class LockPolicy(
    lockEnabled: Boolean,
    private val graceMillis: Long,
    private val now: () -> Long,
) {
    private val _locked = MutableStateFlow(lockEnabled)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundedAt: Long? = null

    fun onBackground() {
        // Si ya está bloqueada (por ejemplo, mientras se muestra la pantalla del PIN), no hay nada que medir.
        if (!_locked.value) backgroundedAt = now()
    }

    fun onForeground(lockEnabled: Boolean) {
        val since = backgroundedAt
        backgroundedAt = null
        when {
            !lockEnabled -> _locked.value = false
            since != null && now() - since >= graceMillis -> _locked.value = true
        }
    }

    fun unlock() {
        _locked.value = false
    }
}
