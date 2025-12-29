package tokyo.isseikuzumaki.vibeterminal.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tokyo.isseikuzumaki.vibeterminal.data.database.dao.ServerConnectionDao
import tokyo.isseikuzumaki.vibeterminal.data.database.entity.ServerConnection
import tokyo.isseikuzumaki.vibeterminal.domain.model.SavedConnection
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository
import tokyo.isseikuzumaki.vibeterminal.security.PasswordEncryptionHelper

class ConnectionRepositoryImpl(
    private val dao: ServerConnectionDao,
    private val dataStore: DataStore<Preferences>,
    private val passwordEncryption: PasswordEncryptionHelper
) : ConnectionRepository {

    companion object {
        private val LAST_ACTIVE_CONNECTION_ID = longPreferencesKey("last_active_connection_id")
        private fun passwordKey(connectionId: Long) = stringPreferencesKey("password_$connectionId")
    }

    override fun getAllConnections(): Flow<List<SavedConnection>> {
        return dao.getAllConnections().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getConnectionById(id: Long): SavedConnection? {
        return dao.getConnectionById(id)?.toDomain()
    }

    override suspend fun insertConnection(connection: SavedConnection): Long {
        return dao.insertConnection(connection.toEntity())
    }

    override suspend fun updateConnection(connection: SavedConnection) {
        dao.updateConnection(connection.toEntity())
    }

    override suspend fun deleteConnection(connection: SavedConnection) {
        dao.deleteConnection(connection.toEntity())
    }

    override suspend fun updateLastUsed(connectionId: Long) {
        val connection = dao.getConnectionById(connectionId)
        if (connection != null) {
            dao.updateConnection(connection.copy(lastUsedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun getLastActiveConnectionId(): Long? {
        return dataStore.data.first()[LAST_ACTIVE_CONNECTION_ID]
    }

    override suspend fun setLastActiveConnectionId(connectionId: Long?) {
        dataStore.edit { preferences ->
            if (connectionId != null) {
                preferences[LAST_ACTIVE_CONNECTION_ID] = connectionId
            } else {
                preferences.remove(LAST_ACTIVE_CONNECTION_ID)
            }
        }
    }

    override suspend fun savePassword(connectionId: Long, password: String) {
        val encryptedPassword = passwordEncryption.encryptPassword(password)
        dataStore.edit { preferences ->
            preferences[passwordKey(connectionId)] = encryptedPassword
        }
    }

    override suspend fun getPassword(connectionId: Long): String? {
        val encryptedPassword = dataStore.data.first()[passwordKey(connectionId)]
        return encryptedPassword?.let {
            try {
                passwordEncryption.decryptPassword(it)
            } catch (e: Exception) {
                // If decryption fails, return null
                null
            }
        }
    }

    override suspend fun deletePassword(connectionId: Long) {
        dataStore.edit { preferences ->
            preferences.remove(passwordKey(connectionId))
        }
    }

    override suspend fun updateLastFileExplorerPath(connectionId: Long, path: String?) {
        dao.updateLastFileExplorerPath(connectionId, path)
    }

    override suspend fun getLastFileExplorerPath(connectionId: Long): String? {
        return dao.getConnectionById(connectionId)?.lastFileExplorerPath
    }

    // Mappers
    private fun ServerConnection.toDomain() = SavedConnection(
        id = id,
        name = name,
        host = host,
        port = port,
        username = username,
        authType = authType,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        deployPattern = deployPattern,
        startupCommand = startupCommand,
        isAutoReconnect = isAutoReconnect,
        monitorFilePath = monitorFilePath,
        lastFileExplorerPath = lastFileExplorerPath
    )

    private fun SavedConnection.toEntity() = ServerConnection(
        id = id,
        name = name,
        host = host,
        port = port,
        username = username,
        authType = authType,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        deployPattern = deployPattern,
        startupCommand = startupCommand,
        isAutoReconnect = isAutoReconnect,
        monitorFilePath = monitorFilePath,
        lastFileExplorerPath = lastFileExplorerPath
    )
}
