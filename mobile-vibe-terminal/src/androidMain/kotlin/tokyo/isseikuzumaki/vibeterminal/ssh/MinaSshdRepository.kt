package tokyo.isseikuzumaki.vibeterminal.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ChannelShell
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.channel.PtyMode
import org.apache.sshd.common.forward.PortForwardingEventListener
import org.apache.sshd.common.channel.Channel
import org.apache.sshd.common.channel.ChannelListener
import org.apache.sshd.common.session.Session
import org.apache.sshd.common.session.SessionListener
import org.apache.sshd.common.util.net.SshdSocketAddress
import org.apache.sshd.sftp.client.SftpClient
import org.apache.sshd.sftp.client.SftpClientFactory
import timber.log.Timber
import tokyo.isseikuzumaki.vibeterminal.domain.model.FileEntry
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import java.io.File
import java.io.FileOutputStream
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

    // Use Channel instead of PipedInputStream for non-blocking data reception
    // This channel is recreated on each connect() to ensure it's fresh after disconnect()
    private var rxChannel = kotlinx.coroutines.channels.Channel<ByteArray>(
        capacity = kotlinx.coroutines.channels.Channel.UNLIMITED
    )

    // Store credentials for creating separate SFTP sessions
    private var connectionHost: String? = null
    private var connectionPort: Int? = null
    private var connectionUsername: String? = null
    private var connectionPassword: String? = null

    // Remote port forwarding for trigger channel
    private var triggerForwardingEnabled = false
    private val triggerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Custom OutputStream that writes to Channel (non-blocking)
    private inner class ChannelOutputStream : java.io.OutputStream() {
        override fun write(b: Int) {
            write(byteArrayOf(b.toByte()), 0, 1)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            val copy = b.copyOfRange(off, off + len)
            Timber.d("SSH_CHANNEL_WRITE: Writing ${copy.size} bytes to rxChannel")
            val result = rxChannel.trySend(copy)
            if (result.isFailure) {
                Timber.e("SSH_CHANNEL_WRITE: Failed to write to channel: ${result.exceptionOrNull()?.message}")
            } else {
                Timber.d("SSH_CHANNEL_WRITE: Successfully wrote to channel")
            }
        }
    }

    override suspend fun connect(
        host: String,
        port: Int,
        username: String,
        password: String,
        initialCols: Int,
        initialRows: Int,
        initialWidthPx: Int,
        initialHeightPx: Int,
        startupCommand: String?
    ): Result<Unit> {
        return try {
            Timber.d("=== MinaSshdRepository.connect ===")
            Timber.d("Host: $host, Port: $port, Username: $username")
            Timber.d("Initial terminal size: ${initialCols}x${initialRows} (${initialWidthPx}x${initialHeightPx}px)")

            // Recreate rxChannel to ensure it's fresh (previous channel may have been closed)
            Timber.d("0. Recreating rxChannel for new connection...")
            try {
                rxChannel.close() // Close old channel if it exists
            } catch (e: Exception) {
                Timber.d("Old rxChannel already closed or not initialized: ${e.message}")
            }
            rxChannel = kotlinx.coroutines.channels.Channel(
                capacity = kotlinx.coroutines.channels.Channel.UNLIMITED
            )
            Timber.d("0. rxChannel recreated successfully")

            // Store credentials for SFTP sessions
            connectionHost = host
            connectionPort = port
            connectionUsername = username
            connectionPassword = password

            // 1. Create and start SSH client
            Timber.d("1. Creating SSH client...")
            val sshClient = SshClient.setUpDefaultClient()

            // Register TriggerForwardingChannel factory to intercept forwarded-tcpip requests
            Timber.d("1a. Registering TriggerForwardingChannel factory...")
            val factories = sshClient.channelFactories.toMutableList()
            factories.removeIf { it.name == "forwarded-tcpip"}
            factories.add(object : org.apache.sshd.common.channel.ChannelFactory {
                override fun createChannel(session: Session): org.apache.sshd.common.channel.Channel = TriggerForwardingChannel()
                override fun getName(): String = "forwarded-tcpip"
            })
            sshClient.channelFactories = factories

            // Add session listener for debug events
            Timber.d("1b. Adding session listener...")
            sshClient.addSessionListener(object : SessionListener {
                override fun sessionCreated(session: Session) {
                    val msg = "Session created: ${session.javaClass.simpleName}"
                    Timber.d(msg)
                    triggerScope.launch { TriggerChannel.getInstance().sendDebugMessage(msg) }
                }

                override fun sessionEvent(session: Session, event: SessionListener.Event) {
                    val msg = "Session event: $event"
                    Timber.d(msg)
                    triggerScope.launch { TriggerChannel.getInstance().sendDebugMessage(msg) }
                }

                override fun sessionException(session: Session, t: Throwable) {
                    val msg = "Session exception: ${t.message}"
                    Timber.e(t, msg)
                    triggerScope.launch { TriggerChannel.getInstance().sendDebugMessage(msg) }
                }

                override fun sessionDisconnect(
                    session: Session,
                    reason: Int,
                    msg: String,
                    language: String,
                    initiator: Boolean
                ) {
                    val debugMsg = "Session disconnect: reason=$reason, msg=$msg, initiator=$initiator"
                    Timber.d(debugMsg)
                    triggerScope.launch { TriggerChannel.getInstance().sendDebugMessage(debugMsg) }
                }

                override fun sessionClosed(session: Session) {
                    val msg = "Session closed"
                    Timber.d(msg)
                    triggerScope.launch { TriggerChannel.getInstance().sendDebugMessage(msg) }
                }
            })

            // Add channel listener for debug events
            Timber.d("1c. Adding channel listener...")
            sshClient.addChannelListener(object : ChannelListener {
                override fun channelInitialized(channel: Channel) {
                    val msg = "Channel initialized: ${channel.javaClass.simpleName} (id=${channel.channelId})"
                    Timber.d(msg)
                    triggerScope.launch { TriggerChannel.getInstance().sendDebugMessage(msg) }
                }

                override fun channelOpenSuccess(channel: Channel) {
                    val msg = "Channel open success: ${channel.javaClass.simpleName} (id=${channel.channelId})"
                    Timber.d(msg)
                    triggerScope.launch { TriggerChannel.getInstance().sendDebugMessage(msg) }
                }

                override fun channelOpenFailure(channel: Channel, reason: Throwable) {
                    val msg = "Channel open failure: ${channel.javaClass.simpleName} - ${reason.message}"
                    Timber.e(reason, msg)
                    triggerScope.launch { TriggerChannel.getInstance().sendDebugMessage(msg) }
                }

                override fun channelStateChanged(channel: Channel, hint: String?) {
                    val msg = "Channel state changed: ${channel.javaClass.simpleName} - hint=$hint"
                    Timber.d(msg)
                    triggerScope.launch { TriggerChannel.getInstance().sendDebugMessage(msg) }
                }

                override fun channelClosed(channel: Channel, reason: Throwable?) {
                    val msg = "Channel closed: ${channel.javaClass.simpleName} (id=${channel.channelId}), reason=${reason?.message}"
                    Timber.d(msg)
                    triggerScope.launch { TriggerChannel.getInstance().sendDebugMessage(msg) }
                }
            })

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

            // Set initial window size from parameters
            shellChannel.ptyColumns = initialCols
            shellChannel.ptyLines = initialRows
            shellChannel.ptyWidth = initialWidthPx
            shellChannel.ptyHeight = initialHeightPx

            Timber.d("10b. PTY configured: xterm-256color, ${initialCols}x${initialRows}")

            // 4b. Log channel window information
            Timber.d("10c. Channel window info - Local: ${shellChannel.localWindow}, Remote: ${shellChannel.remoteWindow}")

            // 5. Set up Channel-based output (non-blocking)
            Timber.d("11. Setting up Channel stream for output...")
            val channelStream = ChannelOutputStream()
            shellChannel.out = channelStream
            shellChannel.err = channelStream

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

            // 8. Execute startup command if provided
            if (!startupCommand.isNullOrBlank()) {
                Timber.d("15. Executing startup command: $startupCommand")
                // Small delay to ensure shell is ready
                kotlinx.coroutines.delay(100)
                outputWriter?.println(startupCommand)
                outputWriter?.flush()
                Timber.d("16. Startup command sent")
            }

            Timber.d("=== SSH Connection Complete ===")

            // 9. Start remote port forwarding for trigger channel (optional)
            startTriggerPortForwarding()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "=== SSH Connection Failed ===")
            Timber.e("Error: ${e.message}")
            disconnect()
            Result.failure(e)
        }
    }

    /**
     * リモートポートフォワーディングを開始してトリガーチャネルを設定する
     *
     * サーバー上のポート58080への接続を受け付け、TriggerForwardingChannel（インターセプト）に転送する
     */
    private fun startTriggerPortForwarding() {
        val currentSession = session ?: return

        try {
            Timber.d("=== Starting Remote Port Forwarding (Interceptor Mode) ===")
            Timber.d("Trigger port: ${SshConstants.TRIGGER_PORT}")

            // リモートポートフォワーディングを設定
            // サーバーの58080ポートへの接続を要求するが、
            // 実際には TriggerForwardingChannel がインターセプトするため、
            // ローカルアドレスはダミーで良い。
            val remoteAddress = SshdSocketAddress("0.0.0.0", SshConstants.TRIGGER_PORT)
            val localAddress = SshdSocketAddress("localhost", 9999) // Dummy

            // リモートポートフォワーディングを開始
            val tracker = currentSession.startRemotePortForwarding(remoteAddress, localAddress)
            triggerForwardingEnabled = true

            val msg = "Remote port forwarding started (Interceptor): tracker=$tracker"
            Timber.d(msg)
            triggerScope.launch { TriggerChannel.getInstance().sendDebugMessage(msg) }
            Timber.d("=== Remote Port Forwarding Setup Complete ===")
        } catch (e: Exception) {
            Timber.w(e, "Failed to start remote port forwarding")
            triggerForwardingEnabled = false
            triggerScope.launch { TriggerChannel.getInstance().sendDebugMessage("Forwarding setup failed: ${e.message}") }
        }
    }

    /**
     * トリガーポートフォワーディングが有効かどうかを返す
     */
    fun isTriggerForwardingEnabled(): Boolean = triggerForwardingEnabled

    /**
     * TriggerChannelのシングルトンインスタンスを取得する
     */
    fun getTriggerChannel(): TriggerChannel = TriggerChannel.getInstance()

    override suspend fun disconnect() {
        Timber.d("=== MinaSshdRepository.disconnect() called ===")
        try {
            outputWriter?.close()
            channel?.close()
            session?.close()
            client?.stop()
            rxChannel.close()
        } finally {
            outputWriter = null
            channel = null
            session = null
            client = null
            connectionHost = null
            connectionPort = null
            connectionUsername = null
            connectionPassword = null
            triggerForwardingEnabled = false
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
        // Launch a coroutine to consume from rxChannel
        val job = launch {
            try {
                for (bytes in rxChannel) {
                    val text = String(bytes, Charsets.UTF_8)
                    Timber.d("SSH_RX_RAW: Received ${bytes.size} bytes from channel")
                    val result = trySend(text)
                    if (result.isFailure) {
                        Timber.e("SSH_RX_RAW: Failed to send to flow: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "SSH_RX_RAW: Exception in output stream: ${e.message}")
                close(e)
            }
        }

        awaitClose {
            Timber.d("SSH_RX_RAW: Flow closed, cancelling job")
            job.cancel()
        }
    }

    override suspend fun sendInput(input: String) {
        Timber.d("SSH_TX_RAW: Sending ${input.length} bytes")
        Timber.d("SSH_TX_RAW: Content hex: ${input.toByteArray().joinToString(" ") { "%02x".format(it) }}")
        outputWriter?.print(input)
        outputWriter?.flush()
        Timber.d("SSH_TX_RAW: Sent and flushed")
    }



    override suspend fun resizeTerminal(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        try {
            val currentChannel = channel
            if (currentChannel != null && currentChannel.isOpen) {
                Timber.d("=== SSH Terminal Resize ===")
                Timber.d("SSH_RESIZE: Sending window change to ${cols}x${rows} (${widthPx}x${heightPx}px)")
                Timber.d("SSH_RESIZE: Channel open=${currentChannel.isOpen}, Closing=${currentChannel.isClosing}")

                currentChannel.sendWindowChange(cols, rows, widthPx, heightPx)

                Timber.d("SSH_RESIZE: Window change command sent successfully")
                Timber.d("SSH_RESIZE: After resize - Channel open=${currentChannel.isOpen}")
            } else {
                Timber.w("SSH_RESIZE: Cannot resize - channel is null or not open (channel=${currentChannel}, open=${currentChannel?.isOpen})")
            }
        } catch (e: Exception) {
            Timber.e(e, "SSH_RESIZE: Failed to send window change - ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
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
