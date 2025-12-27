package tokyo.isseikuzumaki.vibeterminal.domain.model

import java.io.Serializable

data class ConnectionConfig(
    val host: String,
    val port: Int = 22,
    val username: String,
    val password: String,
    val connectionId: Long? = null,
    val startupCommand: String? = null
) : Serializable
