package tokyo.isseikuzumaki.vibeterminal.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.apache.sshd.client.SshClient
import timber.log.Timber
import org.apache.sshd.client.channel.ChannelShell
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.channel.PtyMode
import org.apache.sshd.sftp.client.SftpClient
import org.apache.sshd.sftp.client.SftpClientFactory
import tokyo.isseikuzumaki.vibeterminal.domain.model.FileEntry
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintWriter
import java.util.EnumMap
import java.util.concurrent.TimeUnit

class MinaSshdRepository : SshRepository {
    private var client: SshClient? = null
    private var session: ClientSession? = null
    private var channel: ChannelShell? = null
    private var outputWriter: PrintWriter? = null
    private var outputReader: PipedInputStream? = null

    // Store credentials for creating separate SFTP sessions
    private var connectionHost: String? = null
    private var connectionPort: Int? = null
    private var connectionUsername: String? = null
    private var connectionPassword: String? = null

    override suspend fun connect(
        host: String,
        port: Int,
        username: String,
        password: String
    ): Result<Unit> {
        return try {
            Timber.d("=== MinaSshdRepository.connect ===")
            Timber.d("Host: $host, Port: $port, Username: $username")

            // Store credentials for SFTP sessions
            connectionHost = host
            connectionPort = port
            connectionUsername = username
            connectionPassword = password

            // 1. Create and start SSH client
            Timber.d("1. Creating SSH client...")
            val sshClient = SshClient.setUpDefaultClient()
            Timber.d("2. Starting SSH client...")
            sshClient.start()
            client = sshClient
            Timber.d("3. SSH client started")

            // 2. Connect to server with timeout
            Timber.d("4. Connecting to server...")
            val futureSession = sshClient.connect(username, host, port)
            Timber.d("5. Verifying connection...")
            val clientSession = futureSession.verify(10, TimeUnit.SECONDS).session
            session = clientSession
            Timber.d("6. Connection verified")

            // 3. Authenticate with password
            Timber.d("7. Adding password identity...")
            clientSession.addPasswordIdentity(password)
            Timber.d("8. Authenticating...")
            clientSession.auth().verify(10, TimeUnit.SECONDS)
            Timber.d("9. Authentication successful")

            // 4. Open shell channel with PTY configuration
            Timber.d("10. Creating shell channel...")
            val shellChannel = clientSession.createShellChannel()

            // 4a. Configure PTY for proper terminal emulation (byobu, tmux, vim support)
            Timber.d("10a. Configuring PTY settings...")

            // Set terminal type to xterm-256color (modern standard)
            shellChannel.ptyType = "xterm-256color"

            // Set environment variables
            shellChannel.setEnv("TERM", "xterm-256color")
            shellChannel.setEnv("LANG", "en_US.UTF-8")

            // Configure PTY modes for interactive shell behavior
            val ptyModes = EnumMap<PtyMode, Int>(PtyMode::class.java)
            ptyModes[PtyMode.ECHO] = 1       // Echo back characters
            ptyModes[PtyMode.ICANON] = 1     // Canonical mode (line editing)
            ptyModes[PtyMode.ISIG] = 1       // Signal handling (Ctrl+C, etc.)
            shellChannel.ptyModes = ptyModes

            // Set initial window size to standard 80x24 (safe default)
            // This prevents race conditions with shell initialization
            shellChannel.ptyColumns = 80
            shellChannel.ptyLines = 24
            shellChannel.ptyWidth = 640  // 80 * 8px (approximate char width)
            shellChannel.ptyHeight = 384 // 24 * 16px (approximate char height)

            Timber.d("10b. PTY configured: xterm-256color, 80x24")

            // 5. Set up piped streams to capture output
            Timber.d("11. Setting up piped streams for output...")
            val pipedIn = PipedInputStream()
            val pipedOut = PipedOutputStream(pipedIn)
            shellChannel.out = pipedOut
            shellChannel.err = pipedOut
            outputReader = pipedIn

            // 6. Set up input stream for sending commands
            Timber.d("12. Setting up piped streams for input...")
            val inputPipedOut = PipedOutputStream()
            val inputPipedIn = PipedInputStream(inputPipedOut)
            shellChannel.`in` = inputPipedIn
            outputWriter = PrintWriter(inputPipedOut, true)

            // 7. Open the channel
            Timber.d("13. Opening shell channel...")
            shellChannel.open().verify(5, TimeUnit.SECONDS)
            channel = shellChannel
            Timber.d("14. Shell channel opened successfully")

            Timber.d("=== SSH Connection Complete ===")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "=== SSH Connection Failed ===")
            Timber.e("Error: ${e.message}")
            disconnect()
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        Timber.d("=== MinaSshdRepository.disconnect() called ===")
        try {
            outputWriter?.close()
            channel?.close()
            session?.close()
            client?.stop()
        } finally {
            outputWriter = null
            channel = null
            session = null
            client = null
            connectionHost = null
            connectionPort = null
            connectionUsername = null
            connectionPassword = null
        }
    }


    override fun isConnected(): Boolean {
        return session?.isOpen == true && channel?.isOpen == true
    }

    /**
     * Creates a separate SSH session for SFTP operations.
     * This prevents SFTP operations from interfering with the shell channel.
     * Runs on IO dispatcher to avoid network on main thread exceptions.
     */
    private suspend fun <T> withSftpSession(block: suspend (SftpClient) -> T): T = withContext(Dispatchers.IO) {
        val host = connectionHost ?: throw IllegalStateException("Not connected")
        val port = connectionPort ?: throw IllegalStateException("Not connected")
        val username = connectionUsername ?: throw IllegalStateException("Not connected")
        val password = connectionPassword ?: throw IllegalStateException("Not connected")

        val sftpSshClient = SshClient.setUpDefaultClient()
        sftpSshClient.start()

        try {
            val futureSession = sftpSshClient.connect(username, host, port)
            val sftpSession = futureSession.verify(10, TimeUnit.SECONDS).session

            try {
                sftpSession.addPasswordIdentity(password)
                sftpSession.auth().verify(10, TimeUnit.SECONDS)

                val sftpClient = SftpClientFactory.instance().createSftpClient(sftpSession)
                try {
                    return@withContext block(sftpClient)
                } finally {
                    sftpClient.close()
                }
            } finally {
                sftpSession.close()
            }
        } finally {
            sftpSshClient.stop()
        }
    }

    override suspend fun executeCommand(command: String): Result<String> {
        return try {
            val currentSession = session ?: return Result.failure(
                IllegalStateException("Not connected to SSH server")
            )

            // Execute command and get output
            val execChannel = currentSession.createExecChannel(command)
            val outputStream = java.io.ByteArrayOutputStream()
            execChannel.out = outputStream
            execChannel.err = outputStream

            execChannel.open().verify(5, TimeUnit.SECONDS)
            execChannel.waitFor(
                java.util.EnumSet.of(
                    org.apache.sshd.client.channel.ClientChannelEvent.CLOSED
                ),
                TimeUnit.SECONDS.toMillis(10)
            )

            val output = outputStream.toString("UTF-8")
            execChannel.close()

            Result.success(output)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getOutputStream(): Flow<String> = callbackFlow {
        val reader = outputReader ?: run {
            close()
            return@callbackFlow
        }

        val inputStreamReader = InputStreamReader(reader, Charsets.UTF_8)
        val buffer = CharArray(1024)

        try {
            while (isConnected()) {
                val charsRead = inputStreamReader.read(buffer)
                if (charsRead == -1) break

                val chunk = String(buffer, 0, charsRead)
                trySend(chunk)
            }
        } catch (e: Exception) {
            // Connection closed or read error
        } finally {
            inputStreamReader.close()
            close()
        }

        awaitClose { inputStreamReader.close() }
    }

    override suspend fun sendInput(input: String) {
        outputWriter?.println(input)
    }



    override suspend fun resizeTerminal(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        try {
            val currentChannel = channel
            if (currentChannel != null && currentChannel.isOpen) {
                Timber.d("=== Terminal Resize ===")
                Timber.d("New size: ${cols}x${rows} (${widthPx}x${heightPx}px)")
                currentChannel.sendWindowChange(cols, rows, widthPx, heightPx)
                Timber.d("Window change sent successfully")
            } else {
                Timber.w("Cannot resize: channel is not open")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to send window change: ${e.message}")
        }
    }

    override suspend fun downloadFile(remotePath: String, localFile: File): Result<Unit> {
        return try {
            Timber.d("=== SFTP Download Start ===")
            Timber.d("Remote: $remotePath")
            Timber.d("Local: ${localFile.absolutePath}")

            withSftpSession { sftpClient ->
                Timber.d("Opening remote file for reading...")
                sftpClient.read(remotePath).use { inputStream ->
                    Timber.d("Creating local file...")
                    FileOutputStream(localFile).use { outputStream ->
                        Timber.d("Copying file content...")
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                            if (totalBytes % (1024 * 1024) == 0L) { // Log every MB
                                Timber.d("Downloaded: ${totalBytes / (1024 * 1024)} MB")
                            }
                        }

                        Timber.d("=== SFTP Download Complete ===")
                        Timber.d("Total bytes: $totalBytes")
                    }
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "=== SFTP Download Failed ===")
            Timber.e("Error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun listFiles(remotePath: String): Result<List<FileEntry>> {
        return try {
            withSftpSession { sftpClient ->
                val entries = mutableListOf<FileEntry>()
                val dirEntries = sftpClient.readDir(remotePath)

                for (entry in dirEntries) {
                    val attrs = entry.attributes
                    // Skip . and ..
                    if (entry.filename == "." || entry.filename == "..") {
                        continue
                    }

                    entries.add(
                        FileEntry(
                            name = entry.filename,
                            path = if (remotePath.endsWith("/")) {
                                remotePath + entry.filename
                            } else {
                                "$remotePath/${entry.filename}"
                            },
                            isDirectory = attrs.isDirectory,
                            size = attrs.size,
                            lastModified = attrs.modifyTime.toMillis()
                        )
                    )
                }

                Result.success(entries.sortedWith(
                    compareByDescending<FileEntry> { it.isDirectory }
                        .thenBy { it.name.lowercase() }
                ))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readFileContent(remotePath: String): Result<String> {
        return try {
            withSftpSession { sftpClient ->
                val content = sftpClient.read(remotePath).use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
