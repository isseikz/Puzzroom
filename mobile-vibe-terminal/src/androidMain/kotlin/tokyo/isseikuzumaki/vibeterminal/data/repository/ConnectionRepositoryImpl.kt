package tokyo.isseikuzumaki.vibeterminal.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tokyo.isseikuzumaki.vibeterminal.data.database.dao.ServerConnectionDao
import tokyo.isseikuzumaki.vibeterminal.data.database.entity.ServerConnection
import tokyo.isseikuzumaki.vibeterminal.domain.model.SavedConnection
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository

class ConnectionRepositoryImpl(
    private val dao: ServerConnectionDao,
    private val dataStore: DataStore<Preferences>
) : ConnectionRepository {

    companion object {
        private val LAST_ACTIVE_CONNECTION_ID = longPreferencesKey("last_active_connection_id")
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
        isAutoReconnect = isAutoReconnect
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
        isAutoReconnect = isAutoReconnect
    )
}
