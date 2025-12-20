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
            println("=== MinaSshdRepository.connect ===")
            println("Host: $host, Port: $port, Username: $username")

            // 1. Create and start SSH client
            println("1. Creating SSH client...")
            val sshClient = SshClient.setUpDefaultClient()
            println("2. Starting SSH client...")
            sshClient.start()
            client = sshClient
            println("3. SSH client started")

            // 2. Connect to server with timeout
            println("4. Connecting to server...")
            val futureSession = sshClient.connect(username, host, port)
            println("5. Verifying connection...")
            val clientSession = futureSession.verify(10, TimeUnit.SECONDS).session
            session = clientSession
            println("6. Connection verified")

            // 3. Authenticate with password
            println("7. Adding password identity...")
            clientSession.addPasswordIdentity(password)
            println("8. Authenticating...")
            clientSession.auth().verify(10, TimeUnit.SECONDS)
            println("9. Authentication successful")

            // 4. Open shell channel
            println("10. Creating shell channel...")
            val shellChannel = clientSession.createShellChannel()

            // 5. Set up piped streams to capture output
            println("11. Setting up piped streams for output...")
            val pipedIn = PipedInputStream()
            val pipedOut = PipedOutputStream(pipedIn)
            shellChannel.out = pipedOut
            shellChannel.err = pipedOut
            outputReader = pipedIn

            // 6. Set up input stream for sending commands
            println("12. Setting up piped streams for input...")
            val inputPipedOut = PipedOutputStream()
            val inputPipedIn = PipedInputStream(inputPipedOut)
            shellChannel.`in` = inputPipedIn
            outputWriter = PrintWriter(inputPipedOut, true)

            // 7. Open the channel
            println("13. Opening shell channel...")
            shellChannel.open().verify(5, TimeUnit.SECONDS)
            channel = shellChannel
            println("14. Shell channel opened successfully")

            println("=== SSH Connection Complete ===")
            Result.success(Unit)
        } catch (e: Exception) {
            println("=== SSH Connection Failed ===")
            println("Error: ${e.message}")
            e.printStackTrace()
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
