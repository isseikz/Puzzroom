package tokyo.isseikuzumaki.vibeterminal.domain.repository

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.vibeterminal.domain.model.SavedConnection

/**
 * Repository for managing saved server connections
 */
interface ConnectionRepository {
    /**
     * Get all saved connections as a Flow
     */
    fun getAllConnections(): Flow<List<SavedConnection>>

    /**
     * Get a connection by ID
     */
    suspend fun getConnectionById(id: Long): SavedConnection?

    /**
     * Insert a new connection
     * @return the ID of the inserted connection
     */
    suspend fun insertConnection(connection: SavedConnection): Long

    /**
     * Update an existing connection
     */
    suspend fun updateConnection(connection: SavedConnection)

    /**
     * Delete a connection
     */
    suspend fun deleteConnection(connection: SavedConnection)

    /**
     * Update the last used timestamp for a connection
     */
    suspend fun updateLastUsed(connectionId: Long)
}
