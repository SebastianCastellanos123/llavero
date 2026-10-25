package io.github.sebastiancastellanos123.llavero.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount
import io.github.sebastiancastellanos123.llavero.otp.OtpAuthUri
import io.github.sebastiancastellanos123.llavero.storage.CorruptStoreException
import io.github.sebastiancastellanos123.llavero.storage.EncryptedAccountStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface AccountsState {
    data object Loading : AccountsState
    data class Ready(val accounts: List<OtpAccount>) : AccountsState

    /** No se borra nada automáticamente: el archivo se queda como está por si se puede recuperar. */
    data class Failed(val message: String) : AccountsState
}

class AccountsViewModel(private val store: EncryptedAccountStore) : ViewModel() {
    private val _state = MutableStateFlow<AccountsState>(AccountsState.Loading)
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    /**
     * Datos leídos de un QR que esperan la revisión del usuario. Viven en el
     * ViewModel (sobreviven a una rotación) y nunca se escriben a disco.
     */
    private val _pendingForm = MutableStateFlow<AccountForm?>(null)
    val pendingForm: StateFlow<AccountForm?> = _pendingForm.asStateFlow()

    // Un cambio a la vez: así dos guardados seguidos no se pisan.
    private val mutex = Mutex()

    init {
        viewModelScope.launch {
            _state.value = try {
                AccountsState.Ready(withContext(Dispatchers.IO) { store.load() })
            } catch (e: CorruptStoreException) {
                AccountsState.Failed("No se pudieron leer tus cuentas: el archivo está dañado o la clave del teléfono cambió.")
            } catch (e: Exception) {
                AccountsState.Failed("No se pudieron leer tus cuentas.")
            }
        }
    }

    /** @return true si el QR traía una cuenta válida y quedó lista para revisar. */
    fun onQrScanned(text: String): Boolean {
        if (text.startsWith("otpauth-migration://", ignoreCase = true)) {
            _messages.trySend("Las exportaciones de Google Authenticator aún no se pueden importar. Agrega las cuentas una por una.")
            return false
        }
        return try {
            _pendingForm.value = AccountForm.from(OtpAuthUri.parse(text))
            true
        } catch (e: IllegalArgumentException) {
            _messages.trySend("Este QR no es de una cuenta de verificación: ${e.message}")
            false
        }
    }

    fun clearPendingForm() {
        _pendingForm.value = null
    }

    fun add(account: OtpAccount) = update { list ->
        if (list.any { it.isSameAs(account) }) {
            Change.Rejected("Esa cuenta ya está en Llavero")
        } else {
            Change.Save(list + account, "Cuenta agregada")
        }
    }

    fun delete(account: OtpAccount) = update { list ->
        Change.Save(list.filterNot { it === account }, "Cuenta eliminada")
    }

    private sealed interface Change {
        class Save(val accounts: List<OtpAccount>, val message: String) : Change
        class Rejected(val message: String) : Change
    }

    private fun update(transform: (List<OtpAccount>) -> Change) {
        viewModelScope.launch {
            mutex.withLock {
                val current = (_state.value as? AccountsState.Ready)?.accounts ?: return@withLock
                when (val change = transform(current)) {
                    is Change.Rejected -> _messages.send(change.message)
                    is Change.Save -> try {
                        withContext(Dispatchers.IO) { store.save(change.accounts) }
                        _state.value = AccountsState.Ready(change.accounts)
                        _messages.send(change.message)
                    } catch (e: Exception) {
                        _messages.send("No se pudo guardar el cambio")
                    }
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AccountsViewModel(EncryptedAccountStore.create(this[APPLICATION_KEY]!!)) }
        }
    }
}
