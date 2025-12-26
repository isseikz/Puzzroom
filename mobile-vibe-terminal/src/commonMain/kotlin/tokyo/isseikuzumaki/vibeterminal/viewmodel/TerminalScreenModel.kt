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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import tokyo.isseikuzumaki.vibeterminal.domain.model.ConnectionConfig
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.terminal.AnsiEscapeParser
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalScreenBuffer
import tokyo.isseikuzumaki.vibeterminal.ui.components.macro.MacroTab
import tokyo.isseikuzumaki.vibeterminal.util.Logger

enum class InputMode {
    COMMAND, TEXT
}

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
    val bufferUpdateCounter: Int = 0,  // Trigger UI updates
    val selectedMacroTab: MacroTab = MacroTab.BASIC,
    val hasAutoSwitchedToNav: Boolean = false,
    val inputMode: InputMode = InputMode.COMMAND,
    val isCtrlActive: Boolean = false,
    val isAltActive: Boolean = false
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
                bufferUpdateCounter == other.bufferUpdateCounter &&
                selectedMacroTab == other.selectedMacroTab &&
                hasAutoSwitchedToNav == other.hasAutoSwitchedToNav &&
                inputMode == other.inputMode &&
                isCtrlActive == other.isCtrlActive &&
                isAltActive == other.isAltActive
    }

    override fun hashCode(): Int {
        var result = bufferUpdateCounter
        result = 31 * result + inputMode.hashCode()
        result = 31 * result + isCtrlActive.hashCode()
        result = 31 * result + isAltActive.hashCode()
        return result
    }
}

class TerminalScreenModel(
    private val config: ConnectionConfig,
    private val sshRepository: SshRepository,
    private val apkInstaller: ApkInstaller,
    private val connectionRepository: tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository
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

    // Keep track of output listener job to monitor its lifecycle
    private var outputListenerJob: kotlinx.coroutines.Job? = null

    init {
        Logger.d("=== TerminalScreenModel init ===")
        Logger.d("Config: $config")
        Logger.d("Repository: $sshRepository")
        Logger.d("Waiting for terminal size before connecting...")
    }

    /**
     * Connect to SSH server with the measured terminal size
     */
    fun connect(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        Logger.d("=== connect called with size ${cols}x${rows} ===")
        screenModelScope.launch {
            try {
                Logger.d("Starting connection...")
                _state.update { it.copy(isConnecting = true, errorMessage = null) }

                Logger.d("Calling sshRepository.connect with terminal size...")
                val result = withContext(Dispatchers.IO) {
                    sshRepository.connect(
                        host = config.host,
                        port = config.port,
                        username = config.username,
                        password = config.password,
                        initialCols = cols,
                        initialRows = rows,
                        initialWidthPx = widthPx,
                        initialHeightPx = heightPx,
                        startupCommand = config.startupCommand
                    )
                }
                Logger.d("Connection result: $result")

                // Update terminal buffer size to match
                terminalBuffer.resize(cols, rows)

                result.fold(
                    onSuccess = {
                        Logger.d("Connection successful!")
                        _state.update { it.copy(isConnecting = false, isConnected = true) }
                        processOutput("Connected to ${config.host}:${config.port}\n")

                        // Save as last active connection for auto-restore
                        config.connectionId?.let { id ->
                            screenModelScope.launch {
                                connectionRepository.setLastActiveConnectionId(id)
                                connectionRepository.updateLastUsed(id)
                            }
                        }

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
        // Cancel any existing listener job
        outputListenerJob?.cancel()

        Logger.d("=== Starting Output Listener ===")
        outputListenerJob = screenModelScope.launch(Dispatchers.IO) {
            try {
                Logger.d("Output listener: Collecting from SSH output stream...")
                sshRepository.getOutputStream().collect { line ->
                    // Log received data (RX)
                    Logger.d("SSH_RX: $line")

                    // Check if job is still active
                    if (!isActive) {
                        Logger.w("Output listener: Job is no longer active, stopping collection")
                        return@collect
                    }

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
                Logger.w("Output listener: Collection completed normally (stream ended)")
            } catch (e: kotlinx.coroutines.CancellationException) {
                Logger.w("Output listener: Job was cancelled: ${e.message}")
                throw e  // Re-throw to allow proper cancellation
            } catch (e: Exception) {
                Logger.e(e, "Output listener: Stream error: ${e.message}")
                processOutput("Output stream error: ${e.message}\n")
                _state.update { it.copy(isConnected = false, errorMessage = "Stream error: ${e.message}") }
            }
        }

        // Add completion handler to detect when job finishes
        outputListenerJob?.invokeOnCompletion { cause ->
            when (cause) {
                null -> Logger.d("Output listener: Job completed successfully")
                is kotlinx.coroutines.CancellationException -> Logger.w("Output listener: Job was cancelled")
                else -> Logger.e(cause, "Output listener: Job failed with exception")
            }
        }
    }

    fun sendInput(text: String, appendNewline: Boolean = false) {
        if (text.isEmpty()) return

        screenModelScope.launch {
            var toSend = if (appendNewline) text + "\n" else text
            
            val currentState = _state.value
            var modified = false
            
            if (toSend.length == 1) {
                var char = toSend[0]
                
                if (currentState.isCtrlActive) {
                    if (char in 'a'..'z' || char in 'A'..'Z') {
                        // Convert to control character (A=1, B=2, ...)
                        char = (char.uppercaseChar().code - 'A'.code + 1).toChar()
                        toSend = char.toString()
                        modified = true
                    }
                }
                
                if (currentState.isAltActive) {
                    toSend = "\u001B" + toSend
                    modified = true
                }
            }
            
            if (modified) {
                _state.update { it.copy(isCtrlActive = false, isAltActive = false) }
            }
            
            Logger.d("SSH_TX: $toSend")
            sshRepository.sendInput(toSend)
        }
    }

    fun setInputMode(mode: InputMode) {
        _state.update { it.copy(inputMode = mode) }
    }

    fun toggleInputMode() {
        _state.update {
            val nextMode = if (it.inputMode == InputMode.COMMAND) InputMode.TEXT else InputMode.COMMAND
            it.copy(inputMode = nextMode)
        }
    }

    fun toggleCtrl() {
        _state.update { it.copy(isCtrlActive = !it.isCtrlActive) }
    }

    fun toggleAlt() {
        _state.update { it.copy(isAltActive = !it.isAltActive) }
    }

    /**
     * Select a macro tab
     */
    fun selectMacroTab(tab: MacroTab) {
        _state.update { it.copy(selectedMacroTab = tab) }
    }

    /**
     * Ensure output listener is running. Restart if needed.
     */
    private fun ensureOutputListenerActive() {
        val job = outputListenerJob
        if (job == null || !job.isActive) {
            Logger.w("=== Output Listener Not Active ===")
            Logger.w("Job state: ${job?.let { "exists but inactive (isCancelled=${it.isCancelled}, isCompleted=${it.isCompleted})" } ?: "null"}")
            if (_state.value.isConnected) {
                Logger.w("Connection is still active, restarting output listener...")
                startOutputListener()
            }
        } else {
            Logger.d("Output listener is active")
        }
    }

    /**
     * Resize the terminal
     */
    fun resize(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        Logger.d("=== Terminal Resize Request ===")
        Logger.d("Size: ${cols}x${rows} (${widthPx}x${heightPx}px)")

        // Check output listener status before resize
        Logger.d("Before resize - checking output listener status...")
        ensureOutputListenerActive()

        terminalBuffer.resize(cols, rows)
        updateScreenState()

        screenModelScope.launch {
            sshRepository.resizeTerminal(cols, rows, widthPx, heightPx)

            // Check output listener status after resize
            Logger.d("After resize - checking output listener status...")
            ensureOutputListenerActive()
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
            val newIsAlternateScreen = terminalBuffer.isAlternateScreen
            val shouldAutoSwitch = newIsAlternateScreen &&
                                   !currentState.isAlternateScreen &&
                                   !currentState.hasAutoSwitchedToNav

            currentState.copy(
                screenBuffer = terminalBuffer.getBuffer(),
                cursorRow = terminalBuffer.cursorRow,
                cursorCol = terminalBuffer.cursorCol,
                isAlternateScreen = newIsAlternateScreen,
                bufferUpdateCounter = currentState.bufferUpdateCounter + 1,
                selectedMacroTab = if (shouldAutoSwitch) MacroTab.VIM else currentState.selectedMacroTab,
                hasAutoSwitchedToNav = shouldAutoSwitch || currentState.hasAutoSwitchedToNav
            )
        }
    }

    fun disconnect() {
        screenModelScope.launch {
            Logger.d("=== Disconnecting ===")
            outputListenerJob?.cancel()
            outputListenerJob = null
            sshRepository.disconnect()
            _state.update { it.copy(isConnected = false) }
            processOutput("Disconnected\n")
        }
    }

    /**
     * Disconnect and clear last active session, then execute callback.
     * This ensures the session is cleared BEFORE navigating back to ConnectionList.
     */
    fun disconnectAndClearSession(onComplete: () -> Unit) {
        screenModelScope.launch {
            Logger.d("=== Disconnecting and clearing session ===")

            // First, clear the last active connection ID
            connectionRepository.setLastActiveConnectionId(null)
            Logger.d("Cleared last active connection ID")

            // Then disconnect SSH
            outputListenerJob?.cancel()
            outputListenerJob = null
            sshRepository.disconnect()
            _state.update { it.copy(isConnected = false) }
            processOutput("Disconnected\n")

            // Finally, execute callback (navigate back)
            onComplete()
        }
    }

    /**
     * Download APK from remote server and install it
     */
    fun downloadAndInstallApk(path: String? = null) {
        val apkPath = path ?: _state.value.detectedApkPath ?: return

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
                                _state.update { it.copy(isDownloadingApk = false) }
                                // Only clear detected path if we installed the detected one
                                if (path == null) {
                                    _state.update { it.copy(detectedApkPath = null) }
                                }
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
