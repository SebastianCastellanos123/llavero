package io.github.sebastiancastellanos123.llavero.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.sebastiancastellanos123.llavero.otp.OtpAccount
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

    fun add(account: OtpAccount) = update("Cuenta agregada") { it + account }

    fun delete(account: OtpAccount) = update("Cuenta eliminada") { list -> list.filterNot { it === account } }

    private fun update(successMessage: String, transform: (List<OtpAccount>) -> List<OtpAccount>) {
        viewModelScope.launch {
            mutex.withLock {
                val current = (_state.value as? AccountsState.Ready)?.accounts ?: return@withLock
                val next = transform(current)
                try {
                    withContext(Dispatchers.IO) { store.save(next) }
                    _state.value = AccountsState.Ready(next)
                    _messages.send(successMessage)
                } catch (e: Exception) {
                    _messages.send("No se pudo guardar el cambio")
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
