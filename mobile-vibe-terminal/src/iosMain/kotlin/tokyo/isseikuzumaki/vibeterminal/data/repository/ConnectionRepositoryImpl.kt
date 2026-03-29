package tokyo.isseikuzumaki.vibeterminal.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import tokyo.isseikuzumaki.vibeterminal.data.database.dao.ServerConnectionDao
import tokyo.isseikuzumaki.vibeterminal.data.database.entity.ServerConnection
import tokyo.isseikuzumaki.vibeterminal.domain.model.SavedConnection
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository
import tokyo.isseikuzumaki.vibeterminal.security.KeychainHelper

private const val PASSWORD_KEYCHAIN_PREFIX = "vibe_password_"

/**
 * iOS implementation of ConnectionRepository.
 * Uses Room (with BundledSQLiteDriver) for connection records.
 * Passwords are stored securely in the iOS Keychain via KeychainHelper.
 */
@OptIn(ExperimentalForeignApi::class)
class ConnectionRepositoryImpl(
    private val dao: ServerConnectionDao,
    private val dataStore: DataStore<Preferences>
) : ConnectionRepository {

    companion object {
        private val LAST_ACTIVE_CONNECTION_ID = longPreferencesKey("last_active_connection_id")
    }

    override fun getAllConnections(): Flow<List<SavedConnection>> =
        dao.getAllConnections().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getConnectionById(id: Long): SavedConnection? =
        dao.getConnectionById(id)?.toDomain()

    override suspend fun insertConnection(connection: SavedConnection): Long =
        dao.insertConnection(connection.toEntity())

    override suspend fun updateConnection(connection: SavedConnection) =
        dao.updateConnection(connection.toEntity())

    override suspend fun deleteConnection(connection: SavedConnection) =
        dao.deleteConnection(connection.toEntity())

    override suspend fun updateLastUsed(connectionId: Long) {
        val connection = dao.getConnectionById(connectionId) ?: return
        val nowMs = (NSDate().timeIntervalSince1970 * 1000).toLong()
        dao.updateConnection(connection.copy(lastUsedAt = nowMs))
    }

    override suspend fun getLastActiveConnectionId(): Long? =
        dataStore.data.first()[LAST_ACTIVE_CONNECTION_ID]

    override suspend fun setLastActiveConnectionId(connectionId: Long?) {
        dataStore.edit { prefs ->
            if (connectionId != null) prefs[LAST_ACTIVE_CONNECTION_ID] = connectionId
            else prefs.remove(LAST_ACTIVE_CONNECTION_ID)
        }
    }

    override suspend fun savePassword(connectionId: Long, password: String) {
        KeychainHelper.save("$PASSWORD_KEYCHAIN_PREFIX$connectionId", password)
    }

    override suspend fun getPassword(connectionId: Long): String? =
        KeychainHelper.load("$PASSWORD_KEYCHAIN_PREFIX$connectionId")

    override suspend fun deletePassword(connectionId: Long) {
        KeychainHelper.delete("$PASSWORD_KEYCHAIN_PREFIX$connectionId")
    }

    override suspend fun updateLastFileExplorerPath(connectionId: Long, path: String?) =
        dao.updateLastFileExplorerPath(connectionId, path)

    override suspend fun getLastFileExplorerPath(connectionId: Long): String? =
        dao.getConnectionById(connectionId)?.lastFileExplorerPath

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private fun ServerConnection.toDomain() = SavedConnection(
        id = id,
        name = name,
        host = host,
        port = port,
        username = username,
        authType = authType,
        keyAlias = keyAlias,
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
        keyAlias = keyAlias,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        deployPattern = deployPattern,
        startupCommand = startupCommand,
        isAutoReconnect = isAutoReconnect,
        monitorFilePath = monitorFilePath,
        lastFileExplorerPath = lastFileExplorerPath
    )
}
