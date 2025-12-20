package tokyo.isseikuzumaki.vibeterminal.domain.repository

import kotlinx.coroutines.flow.Flow

interface SshRepository {
    suspend fun connect(host: String, port: Int, username: String, password: String): Result<Unit>
    suspend fun disconnect()
    fun isConnected(): Boolean
    suspend fun executeCommand(command: String): Result<String>
    fun getOutputStream(): Flow<String>
    suspend fun sendInput(input: String)
}
