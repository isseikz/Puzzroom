package tokyo.isseikuzumaki.vibeterminal.ssh

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import libssh2.LIBSSH2_CHANNEL
import libssh2.LIBSSH2_CHANNEL_PACKET_DEFAULT
import libssh2.LIBSSH2_CHANNEL_WINDOW_DEFAULT
import libssh2.LIBSSH2_ERROR_EAGAIN
import libssh2.LIBSSH2_SESSION
import libssh2.LIBSSH2_SFTP
import libssh2.LIBSSH2_SFTP_ATTRIBUTES
import libssh2.LIBSSH2_SFTP_HANDLE
import libssh2.LIBSSH2_SFTP_OPENDIR
import libssh2.LIBSSH2_SFTP_S_IFDIR
import libssh2.libssh2_channel_close
import libssh2.libssh2_channel_free
import libssh2.libssh2_channel_open_ex
import libssh2.libssh2_channel_process_startup
import libssh2.libssh2_channel_read_ex
import libssh2.libssh2_channel_request_pty_ex
import libssh2.libssh2_channel_request_pty_size_ex
import libssh2.libssh2_channel_send_eof
import libssh2.libssh2_channel_setenv_ex
import libssh2.libssh2_channel_write_ex
import libssh2.libssh2_exit
import libssh2.libssh2_init
import libssh2.libssh2_session_disconnect_ex
import libssh2.libssh2_session_free
import libssh2.libssh2_session_init_ex
import libssh2.libssh2_session_last_error
import libssh2.libssh2_session_set_blocking
import libssh2.libssh2_session_handshake
import libssh2.libssh2_sftp_close_handle
import libssh2.libssh2_sftp_init
import libssh2.libssh2_sftp_open_ex
import libssh2.libssh2_sftp_read
import libssh2.libssh2_sftp_readdir_ex
import libssh2.libssh2_sftp_shutdown
import libssh2.libssh2_userauth_password_ex
import libssh2.libssh2_userauth_publickey_frommemory
import libssh2.set_socket_blocking
import libssh2.set_socket_nonblocking
import okio.FileSystem
import okio.Path
import okio.buffer
import platform.posix.AF_INET
import platform.posix.POLLIN
import platform.posix.SOL_SOCKET
import platform.posix.SO_RCVTIMEO
import platform.posix.SOCK_STREAM
import platform.posix.addrinfo
import platform.posix.close
import platform.posix.connect
import platform.posix.freeaddrinfo
import platform.posix.gai_strerror
import platform.posix.getaddrinfo
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.setsockopt
import platform.posix.socket
import platform.posix.timeval
import tokyo.isseikuzumaki.vibeterminal.domain.model.FileEntry
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.security.KeychainHelper

private const val BUFFER_SIZE = 32768
private const val KEY_PREFIX = "vibe_ssh_"
private const val SSH_DISCONNECT_BY_APPLICATION = 11

@OptIn(ExperimentalForeignApi::class, ExperimentalCoroutinesApi::class)
class SshRepositoryIos : SshRepository {

    private val sshContext = newSingleThreadContext("ssh-ios")

    private var socketFd: Int = -1
    private var session: CPointer<LIBSSH2_SESSION>? = null
    private var shellChannel: CPointer<LIBSSH2_CHANNEL>? = null
    private var sftpSession: CPointer<LIBSSH2_SFTP>? = null
    private var connected = false

    private val _outputFlow = MutableSharedFlow<String>(extraBufferCapacity = 1000)
    private var readJob: Job? = null

    // ─── Connect / disconnect ────────────────────────────────────────────────

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
    ): Result<Unit> = withContext(sshContext) {
        runCatching {
            initSession(host, port)
            authenticateWithPassword(username, password)
            openShellChannel(initialCols, initialRows, initialWidthPx, initialHeightPx, startupCommand)
            // Switch to non-blocking libssh2 mode and make the socket non-blocking
            // so libssh2_channel_read_ex never blocks the single sshContext thread.
            // set_socket_nonblocking() is a C helper in libssh2.def — it calls fcntl(2)
            // correctly from C, bypassing Kotlin/Native's vararg limitation on ARM64.
            set_socket_nonblocking(socketFd)
            session?.let { libssh2_session_set_blocking(it, 0) }
            // SO_RCVTIMEO is a belt-and-suspenders fallback: if somehow the socket
            // is still blocking, recv() returns EAGAIN after 100 ms at most.
            memScoped {
                val tv = alloc<timeval>()
                tv.tv_sec = 0
                tv.tv_usec = 100000  // 100 ms
                setsockopt(socketFd, SOL_SOCKET, SO_RCVTIMEO, tv.ptr, sizeOf<timeval>().toUInt())
            }
            connected = true
            startReadLoop(CoroutineScope(sshContext))
        }
    }

    override suspend fun connectWithKey(
        host: String,
        port: Int,
        username: String,
        keyAlias: String,
        initialCols: Int,
        initialRows: Int,
        initialWidthPx: Int,
        initialHeightPx: Int,
        startupCommand: String?
    ): Result<Unit> = withContext(sshContext) {
        runCatching {
            initSession(host, port)
            authenticateWithKey(username, keyAlias)
            openShellChannel(initialCols, initialRows, initialWidthPx, initialHeightPx, startupCommand)
            set_socket_nonblocking(socketFd)
            session?.let { libssh2_session_set_blocking(it, 0) }
            memScoped {
                val tv = alloc<timeval>()
                tv.tv_sec = 0
                tv.tv_usec = 100000  // 100 ms
                setsockopt(socketFd, SOL_SOCKET, SO_RCVTIMEO, tv.ptr, sizeOf<timeval>().toUInt())
            }
            connected = true
            startReadLoop(CoroutineScope(sshContext))
        }
    }

    override suspend fun disconnect() = withContext(sshContext) {
        connected = false
        readJob?.cancel()
        readJob = null

        // Restore blocking socket for clean teardown
        if (socketFd >= 0) {
            set_socket_blocking(socketFd)
            memScoped {
                val tv = alloc<timeval>()
                tv.tv_sec = 0
                tv.tv_usec = 0  // remove timeout
                setsockopt(socketFd, SOL_SOCKET, SO_RCVTIMEO, tv.ptr, sizeOf<timeval>().toUInt())
            }
        }
        session?.let { libssh2_session_set_blocking(it, 1) }

        shellChannel?.let {
            libssh2_channel_send_eof(it)
            libssh2_channel_close(it)
            libssh2_channel_free(it)
        }
        shellChannel = null

        sftpSession?.let { libssh2_sftp_shutdown(it) }
        sftpSession = null

        session?.let {
            libssh2_session_disconnect_ex(it, SSH_DISCONNECT_BY_APPLICATION, "Normal shutdown", "")
            libssh2_session_free(it)
        }
        session = null

        if (socketFd >= 0) {
            close(socketFd)
            socketFd = -1
        }

        libssh2_exit()
    }

    override fun isConnected(): Boolean = connected

    // ─── Terminal I/O ────────────────────────────────────────────────────────

    override fun getOutputStream(): Flow<String> = _outputFlow

    override suspend fun sendInput(input: String): Unit = withContext(sshContext) {
        val channel = shellChannel ?: return@withContext
        val bytes = input.encodeToByteArray()
        while (true) {
            val n = libssh2_channel_write_ex(channel, 0, input, bytes.size.toULong())
            when {
                n >= 0 -> break
                n == LIBSSH2_ERROR_EAGAIN.toLong() -> delay(10)
                else -> break
            }
        }
    }

    override suspend fun executeCommand(command: String): Result<String> = withContext(sshContext) {
        val sess = session ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        runCatching {
            val execChannel = libssh2_channel_open_ex(
                sess, "session", "session".length.toUInt(),
                LIBSSH2_CHANNEL_WINDOW_DEFAULT.toUInt(), LIBSSH2_CHANNEL_PACKET_DEFAULT.toUInt(),
                null, 0u
            ) ?: throw Exception("Failed to open exec channel: ${lastError(sess)}")

            val cmdBytes = command.encodeToByteArray()
            libssh2_channel_write_ex(execChannel, 0, command, cmdBytes.size.toULong())

            val output = StringBuilder()
            val buffer = ByteArray(BUFFER_SIZE)
            buffer.usePinned { pinned ->
                while (true) {
                    val n = libssh2_channel_read_ex(execChannel, 0, pinned.addressOf(0), BUFFER_SIZE.toULong())
                    if (n <= 0) break
                    output.append(buffer.decodeToString(0, n.toInt()))
                }
            }

            libssh2_channel_close(execChannel)
            libssh2_channel_free(execChannel)
            output.toString()
        }
    }

    override suspend fun resizeTerminal(cols: Int, rows: Int, widthPx: Int, heightPx: Int) =
        withContext(sshContext) {
            val channel = shellChannel ?: return@withContext
            libssh2_channel_request_pty_size_ex(channel, cols, rows, widthPx, heightPx)
        }

    // ─── SFTP ────────────────────────────────────────────────────────────────

    override suspend fun listFiles(remotePath: String): Result<List<FileEntry>> =
        withContext(sshContext) {
            val sess = session ?: return@withContext Result.failure(IllegalStateException("Not connected"))
            // Stop the shell read loop so it does not compete for the libssh2
            // session while SFTP operates in blocking mode.  cancelAndJoin()
            // suspends here (freeing the sshContext thread), allowing the read
            // loop to process the cancellation before we continue.
            val job = readJob
            readJob = null
            job?.cancelAndJoin()
            libssh2_session_set_blocking(sess, 1)
            try {
                runCatching {
                    val sftp = getOrInitSftp(sess)
                    val dirHandle = libssh2_sftp_open_ex(
                        sftp, remotePath, remotePath.length.toUInt(),
                        0u, 0, LIBSSH2_SFTP_OPENDIR
                    ) ?: throw Exception("Failed to open SFTP directory: $remotePath")

                    val entries = mutableListOf<FileEntry>()
                    val nameBuf = ByteArray(512)
                    val longBuf = ByteArray(512)

                    memScoped {
                        val attrs = alloc<LIBSSH2_SFTP_ATTRIBUTES>()
                        nameBuf.usePinned { namePinned ->
                            longBuf.usePinned { longPinned ->
                                while (true) {
                                    val rc = libssh2_sftp_readdir_ex(
                                        dirHandle,
                                        namePinned.addressOf(0),
                                        nameBuf.size.toULong(),
                                        longPinned.addressOf(0),
                                        longBuf.size.toULong(),
                                        attrs.ptr
                                    )
                                    if (rc <= 0) break
                                    val name = nameBuf.decodeToString(0, rc)
                                    if (name == "." || name == "..") continue
                                    val isDir = (attrs.permissions.toInt() and 0xF000) == LIBSSH2_SFTP_S_IFDIR.toInt()
                                    val entryPath = if (remotePath.endsWith("/")) "$remotePath$name" else "$remotePath/$name"
                                    entries.add(FileEntry(
                                        name = name, path = entryPath, isDirectory = isDir,
                                        size = attrs.filesize.toLong(), lastModified = attrs.mtime.toLong() * 1000L
                                    ))
                                }
                            }
                        }
                    }

                    libssh2_sftp_close_handle(dirHandle)
                    entries.sortedWith(compareByDescending<FileEntry> { it.isDirectory }.thenBy { it.name })
                }
            } finally {
                libssh2_session_set_blocking(sess, 0)
                startReadLoop(CoroutineScope(sshContext))
            }
        }

    override suspend fun readFileContent(remotePath: String): Result<String> =
        withContext(sshContext) {
            val sess = session ?: return@withContext Result.failure(IllegalStateException("Not connected"))
            val job = readJob
            readJob = null
            job?.cancelAndJoin()
            libssh2_session_set_blocking(sess, 1)
            try {
                runCatching {
                    val sftp = getOrInitSftp(sess)
                    val fileHandle = libssh2_sftp_open_ex(
                        sftp, remotePath, remotePath.length.toUInt(),
                        libssh2.LIBSSH2_FXF_READ.toULong(), 0, libssh2.LIBSSH2_SFTP_OPENFILE
                    ) ?: throw Exception("Failed to open SFTP file: $remotePath")

                    val content = StringBuilder()
                    val buffer = ByteArray(BUFFER_SIZE)
                    buffer.usePinned { pinned ->
                        while (true) {
                            val n = libssh2_sftp_read(fileHandle, pinned.addressOf(0), BUFFER_SIZE.toULong())
                            if (n <= 0) break
                            content.append(buffer.decodeToString(0, n.toInt()))
                        }
                    }

                    libssh2_sftp_close_handle(fileHandle)
                    content.toString()
                }
            } finally {
                libssh2_session_set_blocking(sess, 0)
                startReadLoop(CoroutineScope(sshContext))
            }
        }

    override suspend fun downloadFile(remotePath: String, localFile: Path): Result<Unit> =
        downloadFileWithProgress(remotePath, localFile, 0L) { _, _ -> }

    override suspend fun downloadFileWithProgress(
        remotePath: String,
        localFile: Path,
        totalBytes: Long,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(sshContext) {
        val sess = session ?: return@withContext Result.failure(IllegalStateException("Not connected"))
        val job = readJob
        readJob = null
        job?.cancelAndJoin()
        libssh2_session_set_blocking(sess, 1)
        try {
            runCatching {
                val sftp = getOrInitSftp(sess)
                val fileHandle = libssh2_sftp_open_ex(
                    sftp, remotePath, remotePath.length.toUInt(),
                    libssh2.LIBSSH2_FXF_READ.toULong(), 0, libssh2.LIBSSH2_SFTP_OPENFILE
                ) ?: throw Exception("Failed to open SFTP file: $remotePath")

                val sink = FileSystem.SYSTEM.sink(localFile).buffer()
                var bytesTransferred = 0L
                val buffer = ByteArray(BUFFER_SIZE)

                try {
                    buffer.usePinned { pinned ->
                        while (true) {
                            val n = libssh2_sftp_read(fileHandle, pinned.addressOf(0), BUFFER_SIZE.toULong())
                            if (n <= 0) break
                            sink.write(buffer, 0, n.toInt())
                            bytesTransferred += n
                            onProgress(bytesTransferred, totalBytes)
                        }
                    }
                    sink.flush()
                } finally {
                    sink.close()
                }

                libssh2_sftp_close_handle(fileHandle)
                Unit
            }
        } finally {
            libssh2_session_set_blocking(sess, 0)
            startReadLoop(CoroutineScope(sshContext))
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private fun initSession(host: String, port: Int) {
        libssh2_init(0)

        socketFd = createSocket(host, port)

        val sess = libssh2_session_init_ex(null, null, null, null)
            ?: throw Exception("Failed to create libssh2 session")
        session = sess

        libssh2_session_set_blocking(sess, 1)

        val rc = libssh2_session_handshake(sess, socketFd)
        if (rc != 0) throw Exception("SSH handshake failed (code $rc): ${lastError(sess)}")
    }

    private fun authenticateWithPassword(username: String, password: String) {
        val sess = session ?: throw IllegalStateException("No session")
        val rc = libssh2_userauth_password_ex(
            sess,
            username, username.length.toUInt(),
            password, password.length.toUInt(),
            null
        )
        if (rc != 0) throw Exception("Password authentication failed: ${lastError(sess)}")
    }

    private fun authenticateWithKey(username: String, keyAlias: String) {
        val sess = session ?: throw IllegalStateException("No session")
        val fullAlias = "$KEY_PREFIX$keyAlias"
        val privateKeyPem = KeychainHelper.load("${fullAlias}_private")
            ?: throw Exception("SSH private key not found for alias: $keyAlias")
        val publicKeyPem = KeychainHelper.load("${fullAlias}_public")

        val privBytes = privateKeyPem.encodeToByteArray()
        val pubBytes = publicKeyPem?.encodeToByteArray()

        val rc = if (publicKeyPem != null && pubBytes != null) {
            libssh2_userauth_publickey_frommemory(
                sess, username, username.length.toULong(),
                publicKeyPem, pubBytes.size.toULong(),
                privateKeyPem, privBytes.size.toULong(),
                null
            )
        } else {
            libssh2_userauth_publickey_frommemory(
                sess, username, username.length.toULong(),
                null, 0u,
                privateKeyPem, privBytes.size.toULong(),
                null
            )
        }
        if (rc != 0) throw Exception("Key authentication failed: ${lastError(sess)}")
    }

    private fun openShellChannel(
        cols: Int, rows: Int, widthPx: Int, heightPx: Int, startupCommand: String?
    ) {
        val sess = session ?: throw IllegalStateException("No session")
        val ch = libssh2_channel_open_ex(
            sess, "session", "session".length.toUInt(),
            LIBSSH2_CHANNEL_WINDOW_DEFAULT.toUInt(), LIBSSH2_CHANNEL_PACKET_DEFAULT.toUInt(),
            null, 0u
        ) ?: throw Exception("Failed to open channel: ${lastError(sess)}")

        val term = "TERM"
        val termVal = "xterm-256color"
        libssh2_channel_setenv_ex(ch, term, term.length.toUInt(), termVal, termVal.length.toUInt())

        val ptyRc = libssh2_channel_request_pty_ex(
            ch, "xterm-256color", "xterm-256color".length.toUInt(),
            null, 0u, cols, rows, widthPx, heightPx
        )
        if (ptyRc != 0) throw Exception("PTY request failed: ${lastError(sess)}")

        val shellRc = libssh2_channel_process_startup(ch, "shell", "shell".length.toUInt(), null, 0u)
        if (shellRc != 0) throw Exception("Shell request failed: ${lastError(sess)}")

        if (startupCommand != null) {
            val cmd = startupCommand + "\n"
            libssh2_channel_write_ex(ch, 0, cmd, cmd.encodeToByteArray().size.toULong())
        }

        shellChannel = ch
    }

    private fun startReadLoop(scope: CoroutineScope) {
        readJob = scope.launch {
            val buffer = ByteArray(BUFFER_SIZE)
            while (isActive && connected) {
                // poll(timeout=0): non-blocking check — avoids blocking recv() on the
                // sshContext thread when no shell data has arrived yet.
                val readable = socketFd >= 0 && memScoped {
                    val pfd = alloc<pollfd>()
                    pfd.fd = socketFd
                    pfd.events = POLLIN.toShort()
                    pfd.revents = 0
                    poll(pfd.ptr, 1u, 0) > 0
                }
                if (!readable) {
                    delay(10)
                    continue
                }

                val ch = shellChannel ?: break
                val n = buffer.usePinned { pinned ->
                    libssh2_channel_read_ex(ch, 0, pinned.addressOf(0), BUFFER_SIZE.toULong())
                }
                when {
                    n > 0 -> {
                        _outputFlow.tryEmit(buffer.decodeToString(0, n.toInt()))
                        // yield() allows other sshContext coroutines (e.g. sendInput)
                        // to run if they were queued while we were reading.
                        yield()
                    }
                    n == LIBSSH2_ERROR_EAGAIN.toLong() -> delay(10)
                    n < 0 -> {
                        connected = false
                        break
                    }
                    else -> delay(10)
                }
            }
        }
    }

    private fun getOrInitSftp(sess: CPointer<LIBSSH2_SESSION>): CPointer<LIBSSH2_SFTP> {
        return sftpSession ?: run {
            val sftp = libssh2_sftp_init(sess)
                ?: throw Exception("Failed to initialize SFTP: ${lastError(sess)}")
            sftpSession = sftp
            sftp
        }
    }

    private fun createSocket(host: String, port: Int): Int = memScoped {
        val hints = alloc<addrinfo>()
        hints.ai_family = AF_INET
        hints.ai_socktype = SOCK_STREAM

        val resultPtr = allocPointerTo<addrinfo>()
        val gaiResult = getaddrinfo(host, port.toString(), hints.ptr, resultPtr.ptr)
        if (gaiResult != 0) {
            throw Exception("DNS resolution failed for $host: ${gai_strerror(gaiResult)?.toKString()}")
        }

        val addrResult: CPointer<addrinfo> = resultPtr.value
            ?: throw Exception("No address found for $host")

        val fd = socket(AF_INET, SOCK_STREAM, 0)
        if (fd < 0) {
            freeaddrinfo(addrResult)
            throw Exception("Failed to create socket")
        }

        val sockAddr = addrResult.pointed.ai_addr
        val sockAddrLen = addrResult.pointed.ai_addrlen
        val connResult = connect(fd, sockAddr, sockAddrLen)
        freeaddrinfo(addrResult)

        if (connResult < 0) {
            close(fd)
            throw Exception("Connection refused to $host:$port")
        }

        fd
    }

    private fun lastError(sess: CPointer<LIBSSH2_SESSION>): String {
        return memScoped {
            val msgPtr = allocPointerTo<ByteVar>()
            libssh2_session_last_error(sess, msgPtr.ptr, null, 0)
            msgPtr.value?.toKString() ?: "unknown error"
        }
    }
}
