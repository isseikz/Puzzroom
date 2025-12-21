package tokyo.isseikuzumaki.vibeterminal.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import tokyo.isseikuzumaki.vibeterminal.domain.model.ConnectionConfig
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import java.io.File

data class TerminalState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val logLines: List<String> = emptyList(),
    val errorMessage: String? = null,
    val detectedApkPath: String? = null,
    val isDownloadingApk: Boolean = false
)

class TerminalScreenModel(
    private val config: ConnectionConfig,
    private val sshRepository: SshRepository,
    private val apkInstaller: ApkInstaller
) : ScreenModel {

    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    // Regex pattern for detecting APK deployment marker
    private val deployPattern = Regex(""">> VIBE_DEPLOY:\s*(.+\.apk)""")

    init {
        println("=== TerminalScreenModel init ===")
        println("Config: $config")
        println("Repository: $sshRepository")
        try {
            connectToServer()
        } catch (e: Exception) {
            println("=== ERROR in TerminalScreenModel init ===")
            e.printStackTrace()
        }
    }

    private fun connectToServer() {
        println("=== connectToServer called ===")
        screenModelScope.launch {
            try {
                println("Starting connection...")
                _state.update { it.copy(isConnecting = true, errorMessage = null) }

                println("Calling sshRepository.connect...")
                val result = withContext(Dispatchers.IO) {
                    sshRepository.connect(
                        host = config.host,
                        port = config.port,
                        username = config.username,
                        password = config.password
                    )
                }
                println("Connection result: $result")

                result.fold(
                    onSuccess = {
                        println("Connection successful!")
                        _state.update { it.copy(isConnecting = false, isConnected = true) }
                        addLog("Connected to ${config.host}:${config.port}")
                        startOutputListener()
                    },
                    onFailure = { error ->
                        println("Connection failed: ${error.message}")
                        error.printStackTrace()
                        _state.update {
                            it.copy(
                                isConnecting = false,
                                isConnected = false,
                                errorMessage = "Connection failed: ${error.message}"
                            )
                        }
                        addLog("ERROR: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                println("=== EXCEPTION in connectToServer ===")
                e.printStackTrace()
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        errorMessage = "Exception: ${e.message}"
                    )
                }
                addLog("EXCEPTION: ${e.message}")
            }
        }
    }

    private fun startOutputListener() {
        screenModelScope.launch(Dispatchers.IO) {
            try {
                sshRepository.getOutputStream().collect { line ->
                    addLog(line)

                    // Check for Magic Deploy pattern
                    deployPattern.find(line)?.let { matchResult ->
                        val apkPath = matchResult.groupValues[1].trim()
                        println("=== Magic Deploy Detected ===")
                        println("APK Path: $apkPath")
                        _state.update { it.copy(detectedApkPath = apkPath) }
                        addLog("🚀 Deploy Ready: $apkPath")
                    }
                }
            } catch (e: Exception) {
                addLog("Output stream error: ${e.message}")
                _state.update { it.copy(isConnected = false, errorMessage = "Stream error: ${e.message}") }
            }
        }
    }

    fun sendCommand(command: String) {
        if (command.isBlank()) return

        screenModelScope.launch {
            addLog("> $command")
            sshRepository.sendInput(command)
        }
    }

    private fun addLog(line: String) {
        _state.update { currentState ->
            currentState.copy(
                logLines = currentState.logLines + line
            )
        }
    }

    fun disconnect() {
        screenModelScope.launch {
            sshRepository.disconnect()
            _state.update { it.copy(isConnected = false) }
            addLog("Disconnected")
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
                addLog("📥 Downloading APK: $apkPath")

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

                println("Original path: $apkPath")
                println("Expanded path: $expandedPath")
                addLog("→ Resolving path: $expandedPath")

                val cacheDir = apkInstaller.getCacheDir()
                val localApkFile = File(cacheDir, "downloaded_app.apk")

                // Download via SFTP
                println("Starting SFTP download...")
                val downloadResult = withContext(Dispatchers.IO) {
                    sshRepository.downloadFile(expandedPath, localApkFile)
                }

                downloadResult.fold(
                    onSuccess = {
                        addLog("✅ Download complete: ${localApkFile.length()} bytes")
                        println("Download successful, installing APK...")

                        // Install APK
                        val installResult = apkInstaller.installApk(localApkFile)
                        installResult.fold(
                            onSuccess = {
                                addLog("🚀 Opening installer...")
                                _state.update { it.copy(isDownloadingApk = false, detectedApkPath = null) }
                            },
                            onFailure = { error ->
                                addLog("❌ Install failed: ${error.message}")
                                _state.update { it.copy(isDownloadingApk = false, errorMessage = "Install error: ${error.message}") }
                            }
                        )
                    },
                    onFailure = { error ->
                        addLog("❌ Download failed: ${error.message}")
                        _state.update { it.copy(isDownloadingApk = false, errorMessage = "Download error: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                addLog("❌ Exception: ${e.message}")
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
