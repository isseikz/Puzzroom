package tokyo.isseikuzumaki.vibeterminal.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import tokyo.isseikuzumaki.vibeterminal.domain.model.SavedConnection
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository

class ConnectionRepositoryStub : ConnectionRepository {
    override fun getAllConnections(): Flow<List<SavedConnection>> = flowOf(emptyList())
    override suspend fun getConnectionById(id: Long): SavedConnection? = null
    override suspend fun insertConnection(connection: SavedConnection): Long = 0
    override suspend fun updateConnection(connection: SavedConnection) {}
    override suspend fun deleteConnection(connection: SavedConnection) {}
    override suspend fun updateLastUsed(connectionId: Long) {}
}
