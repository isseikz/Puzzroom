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
import tokyo.isseikuzumaki.vibeterminal.domain.model.ConnectionConfig
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository

data class TerminalState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val logLines: List<String> = emptyList(),
    val errorMessage: String? = null
)

class TerminalScreenModel(
    private val config: ConnectionConfig,
    private val sshRepository: SshRepository
) : ScreenModel {

    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()

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

    override fun onDispose() {
        super.onDispose()
        screenModelScope.launch {
            sshRepository.disconnect()
        }
    }
}
