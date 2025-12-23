package tokyo.isseikuzumaki.vibeterminal.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_connections")
data class ServerConnection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: String, // "password" or "key"
    val createdAt: Long,
    val lastUsedAt: Long? = null,
    val deployPattern: String? = ">> VIBE_DEPLOY: (.*)" // Default regex pattern for deploy detection
)
