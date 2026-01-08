package tokyo.isseikuzumaki.vibeterminal.domain.repository

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.vibeterminal.domain.model.FileEntry
import java.io.File

interface SshRepository {
    suspend fun connect(
        host: String,
        port: Int,
        username: String,
        password: String,
        initialCols: Int = 80,
        initialRows: Int = 24,
        initialWidthPx: Int = 640,
        initialHeightPx: Int = 384,
        startupCommand: String? = null
    ): Result<Unit>

    /**
     * Connect to SSH server using public key authentication.
     * @param host SSH server hostname
     * @param port SSH server port
     * @param username SSH username
     * @param keyAlias Android KeyStore key alias for the SSH key pair
     * @param initialCols Initial terminal width in columns
     * @param initialRows Initial terminal height in rows
     * @param initialWidthPx Initial terminal width in pixels
     * @param initialHeightPx Initial terminal height in pixels
     * @param startupCommand Command to execute on shell startup
     * @return Result indicating success or failure
     */
    suspend fun connectWithKey(
        host: String,
        port: Int,
        username: String,
        keyAlias: String,
        initialCols: Int = 80,
        initialRows: Int = 24,
        initialWidthPx: Int = 640,
        initialHeightPx: Int = 384,
        startupCommand: String? = null
    ): Result<Unit>
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

    /**
     * List files in a directory on the remote server via SFTP
     * @param remotePath Path to the directory on the remote server
     * @return Result containing list of file entries or error
     */
    suspend fun listFiles(remotePath: String): Result<List<FileEntry>>

    /**
     * Read file content from the remote server via SFTP
     * @param remotePath Path to the file on the remote server
     * @return Result containing file content as string or error
     */
    suspend fun readFileContent(remotePath: String): Result<String>

    /**
     * Notify the server of terminal window size changes
     * This is required for proper display of TUI applications like byobu, tmux, vim
     * @param cols Terminal width in columns
     * @param rows Terminal height in rows
     * @param widthPx Terminal width in pixels (optional, for precise rendering)
     * @param heightPx Terminal height in pixels (optional, for precise rendering)
     */
    suspend fun resizeTerminal(cols: Int, rows: Int, widthPx: Int = 0, heightPx: Int = 0)
}
