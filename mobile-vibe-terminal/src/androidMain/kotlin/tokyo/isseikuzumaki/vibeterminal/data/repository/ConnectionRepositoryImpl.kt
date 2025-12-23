package tokyo.isseikuzumaki.vibeterminal.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tokyo.isseikuzumaki.vibeterminal.data.database.dao.ServerConnectionDao
import tokyo.isseikuzumaki.vibeterminal.data.database.entity.ServerConnection
import tokyo.isseikuzumaki.vibeterminal.domain.model.SavedConnection
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository

class ConnectionRepositoryImpl(
    private val dao: ServerConnectionDao
) : ConnectionRepository {

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
        deployPattern = deployPattern
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
        deployPattern = deployPattern
    )
}
