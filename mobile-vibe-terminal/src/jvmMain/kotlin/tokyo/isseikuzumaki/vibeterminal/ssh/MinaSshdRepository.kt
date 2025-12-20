package tokyo.isseikuzumaki.vibeterminal.ssh

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ChannelShell
import org.apache.sshd.client.session.ClientSession
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import java.io.BufferedReader
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
        return Result.failure(NotImplementedError("Use sendInput() for shell interaction"))
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
}
