package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that provides terminal state to multiple consumers.
 *
 * This allows both the main terminal screen and secondary display
 * to access the same terminal buffer and cursor position.
 *
 * The TerminalScreenModel updates this provider whenever the terminal
 * state changes, and the secondary display presentation reads from it.
 *
 * Also handles resize requests from secondary display to terminal.
 */
object TerminalStateProvider {

    data class TerminalDisplayState(
        val buffer: Array<Array<TerminalCell>> = emptyArray(),
        val cursorRow: Int = 0,
        val cursorCol: Int = 0,
        val isAlternateScreen: Boolean = false,
        val isConnected: Boolean = false,
        val bufferUpdateCounter: Int = 0  // Force recomposition on every update
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as TerminalDisplayState

            // Use bufferUpdateCounter for fast change detection instead of deep array comparison
            if (bufferUpdateCounter != other.bufferUpdateCounter) return false
            if (cursorRow != other.cursorRow) return false
            if (cursorCol != other.cursorCol) return false
            if (isAlternateScreen != other.isAlternateScreen) return false
            if (isConnected != other.isConnected) return false

            return true
        }

        override fun hashCode(): Int {
            var result = bufferUpdateCounter
            result = 31 * result + cursorRow
            result = 31 * result + cursorCol
            result = 31 * result + isAlternateScreen.hashCode()
            result = 31 * result + isConnected.hashCode()
            return result
        }
    }

    private val _state = MutableStateFlow(TerminalDisplayState())
    val state: StateFlow<TerminalDisplayState> = _state.asStateFlow()

    /**
     * Callback for resize requests from secondary display.
     * Set by TerminalScreenModel to handle resize requests.
     */
    var onResizeRequest: ((cols: Int, rows: Int, widthPx: Int, heightPx: Int) -> Unit)? = null

    /**
     * Update the terminal state.
     * Should be called by TerminalScreenModel whenever the terminal state changes.
     */
    fun updateState(
        buffer: Array<Array<TerminalCell>>,
        cursorRow: Int,
        cursorCol: Int,
        isAlternateScreen: Boolean,
        isConnected: Boolean
    ) {
        val currentCounter = _state.value.bufferUpdateCounter
        _state.value = TerminalDisplayState(
            buffer = buffer,
            cursorRow = cursorRow,
            cursorCol = cursorCol,
            isAlternateScreen = isAlternateScreen,
            isConnected = isConnected,
            bufferUpdateCounter = currentCounter + 1
        )
    }

    /**
     * Request terminal resize.
     * Called by secondary display when its size is calculated.
     */
    fun requestResize(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        onResizeRequest?.invoke(cols, rows, widthPx, heightPx)
    }

    /**
     * Clear the terminal state.
     * Should be called when disconnecting.
     */
    fun clear() {
        _state.value = TerminalDisplayState()
    }

    // **New**: セカンダリディスプレイの接続状態
    private val _isSecondaryDisplayConnected = MutableStateFlow(false)
    val isSecondaryDisplayConnected: StateFlow<Boolean> = _isSecondaryDisplayConnected.asStateFlow()

    // セカンダリディスプレイのサイズ情報
    data class DisplayMetrics(
        val cols: Int,
        val rows: Int,
        val widthPx: Int,
        val heightPx: Int
    )

    private val _secondaryDisplayMetrics = MutableStateFlow<DisplayMetrics?>(null)
    val secondaryDisplayMetrics: StateFlow<DisplayMetrics?> = _secondaryDisplayMetrics.asStateFlow()

    /**
     * セカンダリディスプレイの接続状態を更新
     */
    fun setSecondaryDisplayConnected(isConnected: Boolean) {
        _isSecondaryDisplayConnected.value = isConnected
    }

    /**
     * セカンダリディスプレイのサイズを設定
     */
    fun setSecondaryDisplayMetrics(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        _secondaryDisplayMetrics.value = DisplayMetrics(cols, rows, widthPx, heightPx)
    }

    /**
     * セカンダリディスプレイのサイズ情報をクリア
     */
    fun clearSecondaryDisplayMetrics() {
        _secondaryDisplayMetrics.value = null
    }

    // ========== Secondary Display Input ==========

    /**
     * Callback for input from secondary display.
     * Set by TerminalScreenModel to handle input from secondary display's TerminalInputContainer.
     */
    var onSecondaryDisplayInput: ((String) -> Unit)? = null

    /**
     * Send input from secondary display to main terminal.
     * Called by TerminalPresentation when input is received.
     */
    fun sendInputFromSecondaryDisplay(input: String) {
        onSecondaryDisplayInput?.invoke(input)
    }

    // ========== Hardware Keyboard Input ==========

    /**
     * Whether a hardware keyboard is connected.
     * Updated by MainActivity when keyboard configuration changes.
     */
    private val _isHardwareKeyboardConnected = MutableStateFlow(false)
    val isHardwareKeyboardConnected: StateFlow<Boolean> = _isHardwareKeyboardConnected.asStateFlow()

    /**
     * Update hardware keyboard connection state.
     */
    fun setHardwareKeyboardConnected(connected: Boolean) {
        _isHardwareKeyboardConnected.value = connected
    }

    /**
     * Whether the terminal is in command mode (RAW mode).
     * Updated by TerminalScreenModel when input mode changes.
     */
    private val _isCommandMode = MutableStateFlow(true)
    val isCommandMode: StateFlow<Boolean> = _isCommandMode.asStateFlow()

    /**
     * Update command mode state.
     */
    fun setCommandMode(isCommand: Boolean) {
        _isCommandMode.value = isCommand
    }

    /**
     * Callback for hardware keyboard input.
     * Set by TerminalScreenModel to handle direct keyboard input.
     */
    var onHardwareKeyboardInput: ((String) -> Unit)? = null

    /**
     * Send input from hardware keyboard to terminal.
     * Called by MainActivity when a key event is processed.
     */
    fun sendHardwareKeyboardInput(input: String) {
        onHardwareKeyboardInput?.invoke(input)
    }
}
