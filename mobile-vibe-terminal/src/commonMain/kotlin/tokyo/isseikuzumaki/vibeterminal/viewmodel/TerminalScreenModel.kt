package tokyo.isseikuzumaki.vibeterminal.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import tokyo.isseikuzumaki.vibeterminal.domain.model.ConnectionConfig
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.terminal.AnsiEscapeParser
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalScreenBuffer
import tokyo.isseikuzumaki.vibeterminal.util.Logger

data class TerminalState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val screenBuffer: Array<Array<TerminalCell>> = emptyArray(),
    val cursorRow: Int = 0,
    val cursorCol: Int = 0,
    val errorMessage: String? = null,
    val detectedApkPath: String? = null,
    val isDownloadingApk: Boolean = false,
    val isAlternateScreen: Boolean = false,
    val bufferUpdateCounter: Int = 0  // Trigger UI updates
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TerminalState) return false
        return isConnecting == other.isConnecting &&
                isConnected == other.isConnected &&
                errorMessage == other.errorMessage &&
                detectedApkPath == other.detectedApkPath &&
                isDownloadingApk == other.isDownloadingApk &&
                isAlternateScreen == other.isAlternateScreen &&
                bufferUpdateCounter == other.bufferUpdateCounter
    }

    override fun hashCode(): Int {
        return bufferUpdateCounter
    }
}

class TerminalScreenModel(
    private val config: ConnectionConfig,
    private val sshRepository: SshRepository,
    private val apkInstaller: ApkInstaller
) : ScreenModel {

    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    // Terminal emulator components
    private val terminalBuffer = TerminalScreenBuffer(cols = 80, rows = 24)
    private val escapeParser = AnsiEscapeParser(terminalBuffer)

    // Regex pattern for detecting APK deployment marker
    private val deployPattern = Regex(""">> VIBE_DEPLOY:\s*(.+\.apk)""")

    // Raw output accumulator for pattern detection
    private val outputAccumulator = StringBuilder()

    init {
        Logger.d("=== TerminalScreenModel init ===")
        Logger.d("Config: $config")
        Logger.d("Repository: $sshRepository")
        try {
            connectToServer()
        } catch (e: Exception) {
            Logger.e(e, "=== ERROR in TerminalScreenModel init ===")
        }
    }

    private fun connectToServer() {
        Logger.d("=== connectToServer called ===")
        screenModelScope.launch {
            try {
                Logger.d("Starting connection...")
                _state.update { it.copy(isConnecting = true, errorMessage = null) }

                Logger.d("Calling sshRepository.connect...")
                val result = withContext(Dispatchers.IO) {
                    sshRepository.connect(
                        host = config.host,
                        port = config.port,
                        username = config.username,
                        password = config.password
                    )
                }
                Logger.d("Connection result: $result")

                result.fold(
                    onSuccess = {
                        Logger.d("Connection successful!")
                        _state.update { it.copy(isConnecting = false, isConnected = true) }
                        processOutput("Connected to ${config.host}:${config.port}\n")
                        startOutputListener()
                    },
                    onFailure = { error ->
                        Logger.e(error, "Connection failed: ${error.message}")
                        _state.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = false,
                                errorMessage = "Connection failed: ${error.message}"
                            )
                        }
                        processOutput("ERROR: ${error.message}\n")
                    }
                )
            } catch (e: Exception) {
                Logger.e(e, "=== EXCEPTION in connectToServer ===")
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        errorMessage = "Exception: ${e.message}"
                    )
                }
                processOutput("EXCEPTION: ${e.message}\n")
            }
        }
    }

    private fun startOutputListener() {
        screenModelScope.launch(Dispatchers.IO) {
            try {
                sshRepository.getOutputStream().collect { line ->
                    // Log received data (RX)
                    Logger.d("SSH_RX: $line")
                    processOutput(line)

                    // Accumulate output for pattern detection
                    outputAccumulator.append(line)

                    // Keep only last 10000 characters to prevent memory issues
                    if (outputAccumulator.length > 10000) {
                        outputAccumulator.delete(0, outputAccumulator.length - 10000)
                    }

                    // Check for Magic Deploy pattern
                    deployPattern.find(outputAccumulator.toString())?.let { matchResult ->
                        val apkPath = matchResult.groupValues[1].trim()
                        Logger.d("=== Magic Deploy Detected ===")
                        Logger.d("APK Path: $apkPath")
                        _state.update { it.copy(detectedApkPath = apkPath) }
                    }
                }
            } catch (e: Exception) {
                processOutput("Output stream error: ${e.message}\n")
                _state.update { it.copy(isConnected = false, errorMessage = "Stream error: ${e.message}") }
            }
        }
    }

    fun sendCommand(command: String) {

        if (command.isBlank()) return

        screenModelScope.launch {
            // Log transmitted data (TX)
            Logger.d("SSH_TX: $command")
            sshRepository.sendInput(command)
        }
    }

    /**
     * Resize the terminal
     */
    fun resize(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        terminalBuffer.resize(cols, rows)
        updateScreenState()
        
        screenModelScope.launch {
            sshRepository.resizeTerminal(cols, rows, widthPx, heightPx)
        }
    }

    /**
     * Process terminal output through the ANSI escape parser
     * and update the screen buffer
     */
    private fun processOutput(text: String) {
        escapeParser.processText(text)
        updateScreenState()
    }

    /**
     * Update the state with the current screen buffer
     */
    private fun updateScreenState() {
        _state.update { currentState ->
            currentState.copy(
                screenBuffer = terminalBuffer.getBuffer(),
                cursorRow = terminalBuffer.cursorRow,
                cursorCol = terminalBuffer.cursorCol,
                isAlternateScreen = terminalBuffer.isAlternateScreen,
                bufferUpdateCounter = currentState.bufferUpdateCounter + 1
            )
        }
    }

    fun disconnect() {
        screenModelScope.launch {
            sshRepository.disconnect()
            _state.update { it.copy(isConnected = false) }
            processOutput("Disconnected\n")
        }
    }

    /**
     * Download APK from remote server and install it
     */
    fun downloadAndInstallApk() {
        val apkPath = _state.value.detectedApkPath ?: return

        screenModelScope.launch {
            try {
                _state.update { it.copy(isDownloadingApk = true) }
                processOutput("📥 Downloading APK: $apkPath\n")

                // Expand tilde (~) to home directory path
                val expandedPath = if (apkPath.startsWith("~/")) {
                    // Get home directory from SSH session by executing 'echo $HOME'
                    val homeResult = withContext(Dispatchers.IO) {
                        sshRepository.executeCommand("echo \$HOME")
                    }
                    val homePath = homeResult.getOrNull()?.trim() ?: "/home/${config.username}"
                    apkPath.replaceFirst("~/", "$homePath/")
                } else {
                    apkPath
                }

                Logger.d("Original path: $apkPath")
                Logger.d("Expanded path: $expandedPath")
                processOutput("→ Resolving path: $expandedPath\n")

                val cacheDir = apkInstaller.getCacheDir()
                val localApkFile = File(cacheDir, "downloaded_app.apk")

                // Download via SFTP
                Logger.d("Starting SFTP download...")
                val downloadResult = withContext(Dispatchers.IO) {
                    sshRepository.downloadFile(expandedPath, localApkFile)
                }

                downloadResult.fold(
                    onSuccess = {
                        processOutput("✅ Download complete: ${localApkFile.length()} bytes\n")
                        Logger.d("Download successful, installing APK...")

                        // Install APK
                        val installResult = apkInstaller.installApk(localApkFile)
                        installResult.fold(
                            onSuccess = {
                                processOutput("🚀 Opening installer...\n")
                                _state.update { it.copy(isDownloadingApk = false, detectedApkPath = null) }
                            },
                            onFailure = { error ->
                                processOutput("❌ Install failed: ${error.message}\n")
                                _state.update { it.copy(isDownloadingApk = false, errorMessage = "Install error: ${error.message}") }
                            }
                        )
                    },
                    onFailure = { error ->
                        processOutput("❌ Download failed: ${error.message}\n")
                        _state.update { it.copy(isDownloadingApk = false, errorMessage = "Download error: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                processOutput("❌ Exception: ${e.message}\n")
                _state.update { it.copy(isDownloadingApk = false, errorMessage = "Exception: ${e.message}") }
            }
        }
    }

    /**
     * Clear the deploy notification
     */
    fun clearDeployNotification() {
        _state.update { it.copy(detectedApkPath = null) }
    }

    override fun onDispose() {
        super.onDispose()
        screenModelScope.launch {
            sshRepository.disconnect()
        }
    }
}
