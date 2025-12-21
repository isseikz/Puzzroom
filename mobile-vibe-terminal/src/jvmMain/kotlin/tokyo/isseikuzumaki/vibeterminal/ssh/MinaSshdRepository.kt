package tokyo.isseikuzumaki.vibeterminal.ssh

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ChannelShell
import org.apache.sshd.client.session.ClientSession
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
import java.util.concurrent.TimeUnit

class MinaSshdRepository : SshRepository {
    private var client: SshClient? = null
    private var session: ClientSession? = null
    private var channel: ChannelShell? = null
    private var outputWriter: PrintWriter? = null
    private var outputReader: PipedInputStream? = null

    override suspend fun connect(
        host: String,
        port: Int,
        username: String,
        password: String
    ): Result<Unit> {
        return try {
            // 1. Create and start SSH client
            val sshClient = SshClient.setUpDefaultClient()
            sshClient.start()
            client = sshClient

            // 2. Connect to server with timeout
            val futureSession = sshClient.connect(username, host, port)
            val clientSession = futureSession.verify(10, TimeUnit.SECONDS).session
            session = clientSession

            // 3. Authenticate with password
            clientSession.addPasswordIdentity(password)
            clientSession.auth().verify(10, TimeUnit.SECONDS)

            // 4. Open shell channel
            val shellChannel = clientSession.createShellChannel()

            // 5. Set up piped streams to capture output
            val pipedIn = PipedInputStream()
            val pipedOut = PipedOutputStream(pipedIn)
            shellChannel.out = pipedOut
            shellChannel.err = pipedOut
            outputReader = pipedIn

            // 6. Set up input stream for sending commands
            val inputPipedOut = PipedOutputStream()
            val inputPipedIn = PipedInputStream(inputPipedOut)
            shellChannel.`in` = inputPipedIn
            outputWriter = PrintWriter(inputPipedOut, true)

            // 7. Open the channel
            shellChannel.open().verify(5, TimeUnit.SECONDS)
            channel = shellChannel

            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
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
        }
    }

    override fun isConnected(): Boolean {
        return session?.isOpen == true && channel?.isOpen == true
    }

    override suspend fun executeCommand(command: String): Result<String> {
        return try {
            val currentSession = session ?: return Result.failure(
                IllegalStateException("Not connected to SSH server")
            )

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

        val bufferedReader = BufferedReader(InputStreamReader(reader))

        try {
            while (isConnected()) {
                val line = bufferedReader.readLine() ?: break
                trySend(line)
            }
        } catch (e: Exception) {
            // Connection closed or read error
        } finally {
            bufferedReader.close()
            close()
        }

        awaitClose { bufferedReader.close() }
    }

    override suspend fun sendInput(input: String) {
        outputWriter?.println(input)
    }

    override suspend fun downloadFile(remotePath: String, localFile: File): Result<Unit> {
        return try {
            val currentSession = session ?: return Result.failure(
                IllegalStateException("Not connected to SSH server")
            )

            val sftpClient: SftpClient = SftpClientFactory.instance().createSftpClient(currentSession)

            try {
                sftpClient.read(remotePath).use { inputStream ->
                    FileOutputStream(localFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                    }
                }
                Result.success(Unit)
            } finally {
                sftpClient.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listFiles(remotePath: String): Result<List<FileEntry>> {
        return try {
            val currentSession = session ?: return Result.failure(
                IllegalStateException("Not connected to SSH server")
            )

            val sftpClient: SftpClient = SftpClientFactory.instance().createSftpClient(currentSession)

            try {
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
            } finally {
                sftpClient.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readFileContent(remotePath: String): Result<String> {
        return try {
            val currentSession = session ?: return Result.failure(
                IllegalStateException("Not connected to SSH server")
            )

            val sftpClient: SftpClient = SftpClientFactory.instance().createSftpClient(currentSession)

            try {
                val content = sftpClient.read(remotePath).use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                Result.success(content)
            } finally {
                sftpClient.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
