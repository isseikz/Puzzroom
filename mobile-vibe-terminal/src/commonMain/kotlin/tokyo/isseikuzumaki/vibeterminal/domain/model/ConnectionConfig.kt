package tokyo.isseikuzumaki.vibeterminal.domain.model

import java.io.Serializable

data class ConnectionConfig(
    val host: String,
    val port: Int = 22,
    val username: String,
    val password: String? = null, // For password authentication
    val keyAlias: String? = null, // For public key authentication (Android KeyStore alias)
    val connectionId: Long? = null,
    val startupCommand: String? = null,
    val deployPattern: String? = null,
    val monitorFilePath: String? = null
) : Serializable
