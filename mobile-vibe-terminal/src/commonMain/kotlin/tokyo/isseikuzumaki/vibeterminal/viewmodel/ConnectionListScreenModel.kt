package tokyo.isseikuzumaki.vibeterminal.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.vibeterminal.domain.model.SavedConnection
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository

data class ConnectionListState(
    val connections: List<SavedConnection> = emptyList(),
    val showAddDialog: Boolean = false,
    val editingConnection: SavedConnection? = null,
    val errorMessage: String? = null
)

class ConnectionListScreenModel(
    private val connectionRepository: ConnectionRepository
) : ScreenModel {

    private val _state = MutableStateFlow(ConnectionListState())
    val state: StateFlow<ConnectionListState> = _state.asStateFlow()

    init {
        loadConnections()
    }

    private fun loadConnections() {
        screenModelScope.launch {
            connectionRepository.getAllConnections().collect { connections ->
                _state.update { it.copy(connections = connections) }
            }
        }
    }

    fun showAddDialog() {
        _state.update { it.copy(showAddDialog = true, editingConnection = null) }
    }

    fun hideDialog() {
        _state.update { it.copy(showAddDialog = false, editingConnection = null) }
    }

    fun showEditDialog(connection: SavedConnection) {
        _state.update { it.copy(editingConnection = connection, showAddDialog = true) }
    }

    fun saveConnection(connection: SavedConnection) {
        screenModelScope.launch {
            try {
                if (connection.id == 0L) {
                    // New connection
                    connectionRepository.insertConnection(connection)
                } else {
                    // Update existing
                    connectionRepository.updateConnection(connection)
                }
                hideDialog()
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    fun deleteConnection(connection: SavedConnection) {
        screenModelScope.launch {
            try {
                connectionRepository.deleteConnection(connection)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun updateLastUsed(connectionId: Long) {
        screenModelScope.launch {
            connectionRepository.updateLastUsed(connectionId)
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
