package tokyo.isseikuzumaki.vibeterminal.domain.model

/**
 * Domain model for a saved server connection
 */
data class SavedConnection(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: String, // "password" or "key"
    val createdAt: Long,
    val lastUsedAt: Long? = null,
    val deployPattern: String? = ">> VIBE_DEPLOY: (.*)",
    val startupCommand: String? = null, // Command to execute on shell startup (e.g., "tmux attach || tmux new")
    val isAutoReconnect: Boolean = false, // Enable automatic reconnection on app restart
    val monitorFilePath: String? = null
)
