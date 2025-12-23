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
    val deployPattern: String? = ">> VIBE_DEPLOY: (.*)"
)
