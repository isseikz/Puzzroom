package tokyo.isseikuzumaki.vibeterminal.domain.model

data class ConnectionConfig(
    val host: String,
    val port: Int = 22,
    val username: String,
    val password: String,
    val connectionId: Long? = null,
    val startupCommand: String? = null,
    val deployPattern: String? = null,
    val monitorFilePath: String? = null
)
