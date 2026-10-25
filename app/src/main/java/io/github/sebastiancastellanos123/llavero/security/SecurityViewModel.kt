package io.github.sebastiancastellanos123.llavero.security

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferencias de seguridad (no son secretas: solo dos interruptores) y el
 * estado de bloqueo. Vive en un ViewModel para que rotar la pantalla no bloquee
 * la app; si Android cierra el proceso, al volver empieza bloqueada.
 */
class SecurityViewModel(
    private val prefs: SharedPreferences,
    private val deviceCanAuthenticate: () -> Boolean,
    now: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {

    companion object {
        const val GRACE_MILLIS = 60_000L
        private const val KEY_LOCK = "lock_enabled"
        private const val KEY_BLOCK_SCREENSHOTS = "block_screenshots"

        fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences("security", Context.MODE_PRIVATE)

        /** Se lee antes de dibujar la primera pantalla para que nunca se vea sin protección. */
        fun blockScreenshots(prefs: SharedPreferences) = prefs.getBoolean(KEY_BLOCK_SCREENSHOTS, true)

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                SecurityViewModel(prefs(app), { DeviceAuth.isAvailable(app) })
            }
        }
    }

    private val _lockAvailable = MutableStateFlow(deviceCanAuthenticate())
    /** El teléfono tiene bloqueo de pantalla; sin eso no hay con qué pedir verificación. */
    val lockAvailable: StateFlow<Boolean> = _lockAvailable.asStateFlow()

    private val _lockEnabled = MutableStateFlow(prefs.getBoolean(KEY_LOCK, true))
    val lockEnabled: StateFlow<Boolean> = _lockEnabled.asStateFlow()

    private val _blockScreenshots = MutableStateFlow(blockScreenshots(prefs))
    val blockScreenshots: StateFlow<Boolean> = _blockScreenshots.asStateFlow()

    private val policy = LockPolicy(isLockActive(), GRACE_MILLIS, now)
    val locked: StateFlow<Boolean> = policy.locked

    private fun isLockActive() = _lockEnabled.value && _lockAvailable.value

    fun onForeground() {
        // El usuario pudo quitar el bloqueo de pantalla del teléfono mientras la app estaba en segundo plano.
        _lockAvailable.value = deviceCanAuthenticate()
        policy.onForeground(isLockActive())
    }

    fun onBackground() = policy.onBackground()

    fun unlock() = policy.unlock()

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_LOCK, enabled) }
        _lockEnabled.value = enabled
        if (!enabled) policy.unlock()
    }

    fun setBlockScreenshots(block: Boolean) {
        prefs.edit { putBoolean(KEY_BLOCK_SCREENSHOTS, block) }
        _blockScreenshots.value = block
    }
}
