package tokyo.isseikuzumaki.vibeterminal.domain.repository

import kotlinx.coroutines.flow.Flow
import java.io.File

interface SshRepository {
    suspend fun connect(host: String, port: Int, username: String, password: String): Result<Unit>
    suspend fun disconnect()
    fun isConnected(): Boolean
    suspend fun executeCommand(command: String): Result<String>
    fun getOutputStream(): Flow<String>
    suspend fun sendInput(input: String)

    /**
     * Download a file from the remote server via SFTP
     * @param remotePath Path to the file on the remote server
     * @param localFile Local file to save the downloaded content
     * @return Result indicating success or failure
     */
    suspend fun downloadFile(remotePath: String, localFile: File): Result<Unit>
}
