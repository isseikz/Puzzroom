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
 */
object TerminalStateProvider {

    data class TerminalDisplayState(
        val buffer: Array<Array<TerminalCell>> = emptyArray(),
        val cursorRow: Int = 0,
        val cursorCol: Int = 0,
        val isAlternateScreen: Boolean = false,
        val isConnected: Boolean = false
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as TerminalDisplayState

            if (!buffer.contentDeepEquals(other.buffer)) return false
            if (cursorRow != other.cursorRow) return false
            if (cursorCol != other.cursorCol) return false
            if (isAlternateScreen != other.isAlternateScreen) return false
            if (isConnected != other.isConnected) return false

            return true
        }

        override fun hashCode(): Int {
            var result = buffer.contentDeepHashCode()
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
        _state.value = TerminalDisplayState(
            buffer = buffer,
            cursorRow = cursorRow,
            cursorCol = cursorCol,
            isAlternateScreen = isAlternateScreen,
            isConnected = isConnected
        )
    }

    /**
     * Clear the terminal state.
     * Should be called when disconnecting.
     */
    fun clear() {
        _state.value = TerminalDisplayState()
    }
}
