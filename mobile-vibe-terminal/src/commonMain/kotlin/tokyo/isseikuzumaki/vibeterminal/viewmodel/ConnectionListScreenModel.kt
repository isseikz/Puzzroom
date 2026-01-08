package tokyo.isseikuzumaki.vibeterminal.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.vibeterminal.domain.model.ConnectionConfig
import tokyo.isseikuzumaki.vibeterminal.domain.model.SavedConnection
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyInfoCommon
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyProvider

data class ConnectionListState(
    val connections: List<SavedConnection> = emptyList(),
    val showAddDialog: Boolean = false,
    val editingConnection: SavedConnection? = null,
    val errorMessage: String? = null,
    val availableKeys: List<SshKeyInfoCommon> = emptyList()
)

class ConnectionListScreenModel(
    private val connectionRepository: ConnectionRepository,
    private val sshKeyProvider: SshKeyProvider? = null
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
        refreshAvailableKeys()
        _state.update { it.copy(showAddDialog = true, editingConnection = null) }
    }

    fun hideDialog() {
        _state.update { it.copy(showAddDialog = false, editingConnection = null) }
    }

    fun showEditDialog(connection: SavedConnection) {
        refreshAvailableKeys()
        _state.update { it.copy(editingConnection = connection, showAddDialog = true) }
    }

    fun refreshAvailableKeys() {
        val keys = sshKeyProvider?.listKeys() ?: emptyList()
        _state.update { it.copy(availableKeys = keys) }
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

    fun loadPassword(connectionId: Long, onLoaded: (String?) -> Unit) {
        screenModelScope.launch {
            try {
                val password = connectionRepository.getPassword(connectionId)
                onLoaded(password)
            } catch (e: Exception) {
                onLoaded(null)
            }
        }
    }

    fun savePassword(connectionId: Long, password: String) {
        screenModelScope.launch {
            try {
                connectionRepository.savePassword(connectionId, password)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Failed to save password: ${e.message}") }
            }
        }
    }

    fun deletePassword(connectionId: Long) {
        screenModelScope.launch {
            try {
                connectionRepository.deletePassword(connectionId)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    fun checkAndRestoreLastSession(onResult: (ConnectionConfig?) -> Unit) {
        screenModelScope.launch {
            try {
                // Get last active connection ID
                val lastConnectionId = connectionRepository.getLastActiveConnectionId()
                if (lastConnectionId == null) {
                    onResult(null)
                    return@launch
                }

                // Get connection details
                val connection = connectionRepository.getConnectionById(lastConnectionId)
                if (connection == null || !connection.isAutoReconnect) {
                    onResult(null)
                    return@launch
                }

                // Check authentication method
                if (connection.authType == "key" && connection.keyAlias != null) {
                    // Public key auth: can auto-restore without password
                    val config = ConnectionConfig(
                        host = connection.host,
                        port = connection.port,
                        username = connection.username,
                        password = null,
                        keyAlias = connection.keyAlias,
                        connectionId = connection.id,
                        startupCommand = connection.startupCommand,
                        deployPattern = connection.deployPattern,
                        monitorFilePath = connection.monitorFilePath
                    )
                    onResult(config)
                } else {
                    // Password auth: need saved password
                    val password = connectionRepository.getPassword(lastConnectionId)
                    if (password == null) {
                        // No saved password, cannot auto-restore
                        onResult(null)
                        return@launch
                    }

                    // Create ConnectionConfig for auto-restore
                    val config = ConnectionConfig(
                        host = connection.host,
                        port = connection.port,
                        username = connection.username,
                        password = password,
                        keyAlias = null,
                        connectionId = connection.id,
                        startupCommand = connection.startupCommand,
                        deployPattern = connection.deployPattern,
                        monitorFilePath = connection.monitorFilePath
                    )
                    onResult(config)
                }
            } catch (e: Exception) {
                // If anything fails, just show connection list
                onResult(null)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
