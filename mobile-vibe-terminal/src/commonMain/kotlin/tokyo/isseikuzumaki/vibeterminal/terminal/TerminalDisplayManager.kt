package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the target display for terminal output.
 */
enum class DisplayTarget {
    /** Terminal is displayed on the main (phone) display */
    MAIN,
    /** Terminal is displayed on the secondary (external) display */
    SECONDARY
}

/**
 * Singleton that manages the terminal display target based on display connection state.
 *
 * Simplified Architecture:
 * - When secondary display is connected → display on SECONDARY only
 * - When secondary display is disconnected → display on MAIN only
 *
 * The derived state triggers side effects:
 * - SECONDARY: Show terminal on secondary display, hide on main display
 * - MAIN: Show terminal on main display
 */
object TerminalDisplayManager {

    // ========== Input State: Display Connection Status ==========

    private val _isDisplayConnected = MutableStateFlow(false)

    /**
     * Whether an external display is physically connected.
     * Updated by DisplayManager.DisplayListener.
     */
    val isDisplayConnected: StateFlow<Boolean> = _isDisplayConnected.asStateFlow()

    /**
     * Update the display connection status.
     * Should be called from DisplayManager.DisplayListener callbacks.
     *
     * @param connected true if external display is connected, false otherwise
     */
    fun setDisplayConnected(connected: Boolean) {
        _isDisplayConnected.value = connected
        updateDerivedState()
    }

    // ========== Input State: User Preference ==========

    private val _useExternalDisplay = MutableStateFlow(true)

    /**
     * Whether the user wants to use the external display if connected.
     * Defaults to true.
     */
    val useExternalDisplay: StateFlow<Boolean> = _useExternalDisplay.asStateFlow()

    /**
     * Update the user preference for external display usage.
     *
     * @param enabled true to use external display when available, false to ignore it
     */
    fun setUseExternalDisplay(enabled: Boolean) {
        _useExternalDisplay.value = enabled
        updateDerivedState()
    }

    // ========== Derived State: Terminal Display Target ==========

    private val _terminalDisplayTarget = MutableStateFlow(DisplayTarget.MAIN)

    /**
     * The computed target display for terminal output.
     *
     * Returns [DisplayTarget.SECONDARY] when external display is connected AND enabled by user,
     * [DisplayTarget.MAIN] otherwise.
     */
    val terminalDisplayTarget: StateFlow<DisplayTarget> = _terminalDisplayTarget.asStateFlow()

    /**
     * Update the derived state based on display connection and user preference.
     */
    private fun updateDerivedState() {
        val newTarget = if (_isDisplayConnected.value && _useExternalDisplay.value) {
            DisplayTarget.SECONDARY
        } else {
            DisplayTarget.MAIN
        }
        _terminalDisplayTarget.value = newTarget
    }

    /**
     * Reset all states to initial values.
     * Useful for testing.
     */
    fun reset() {
        _isDisplayConnected.value = false
        _useExternalDisplay.value = true
        _terminalDisplayTarget.value = DisplayTarget.MAIN
    }
}
