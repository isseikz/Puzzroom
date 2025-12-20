package tokyo.isseikuzumaki.vibeterminal.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.vibeterminal.data.database.entity.ServerConnection

@Dao
interface ServerConnectionDao {
    @Query("SELECT * FROM server_connections ORDER BY lastUsedAt DESC")
    fun getAllConnections(): Flow<List<ServerConnection>>

    @Query("SELECT * FROM server_connections WHERE id = :id")
    suspend fun getConnectionById(id: Long): ServerConnection?

    @Insert
    suspend fun insertConnection(connection: ServerConnection): Long

    @Update
    suspend fun updateConnection(connection: ServerConnection)

    @Delete
    suspend fun deleteConnection(connection: ServerConnection)
}
