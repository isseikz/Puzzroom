package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
 * Manages the terminal display target based on input states.
 *
 * This class implements the state management architecture described in
 * DesignDocument.md Section 4.4 (Secondary Display Control).
 *
 * Architecture:
 * - Input State 1: isDisplayConnected (system event from DisplayManager)
 * - Input State 2: useExternalDisplay (user preference)
 * - Derived State: terminalDisplayTarget (computed from input states)
 *
 * State Transition Matrix:
 * | isDisplayConnected | useExternalDisplay | terminalDisplayTarget |
 * |:------------------:|:------------------:|:---------------------:|
 * | false              | false              | MAIN                  |
 * | false              | true               | MAIN                  |
 * | true               | false              | MAIN                  |
 * | true               | true               | SECONDARY             |
 *
 * The derived state triggers side effects:
 * - SECONDARY: Start TerminalService, hide terminal on main display
 * - MAIN: Stop TerminalService, show terminal on main display
 */
class TerminalDisplayManager(
    @Suppress("UNUSED_PARAMETER") scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    // ========== Input State 1: Display Connection Status ==========

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

    // ========== Input State 2: User Preference ==========

    private val _useExternalDisplay = MutableStateFlow(false)

    /**
     * Whether the user wants to use the external display for terminal output.
     * Updated by user action (button press, settings toggle, etc.)
     */
    val useExternalDisplay: StateFlow<Boolean> = _useExternalDisplay.asStateFlow()

    /**
     * Update the user's preference for using external display.
     * Should be called when user toggles the external display option.
     *
     * @param use true if user wants to use external display, false otherwise
     */
    fun setUseExternalDisplay(use: Boolean) {
        _useExternalDisplay.value = use
        updateDerivedState()
    }

    // ========== Derived State: Terminal Display Target ==========

    private val _terminalDisplayTarget = MutableStateFlow(DisplayTarget.MAIN)

    /**
     * The computed target display for terminal output.
     *
     * This is a derived state that combines [isDisplayConnected] and [useExternalDisplay].
     * Returns [DisplayTarget.SECONDARY] only when both conditions are true:
     * - External display is physically connected
     * - User has enabled external display usage
     *
     * Otherwise, returns [DisplayTarget.MAIN].
     *
     * Observers of this state should trigger appropriate side effects:
     * - On MAIN: Stop TerminalService, show terminal on main display
     * - On SECONDARY: Start TerminalService, hide terminal on main display
     */
    val terminalDisplayTarget: StateFlow<DisplayTarget> = _terminalDisplayTarget.asStateFlow()

    /**
     * Update the derived state based on current input states.
     * Called whenever an input state changes.
     */
    private fun updateDerivedState() {
        val newTarget = if (_isDisplayConnected.value && _useExternalDisplay.value) {
            DisplayTarget.SECONDARY
        } else {
            DisplayTarget.MAIN
        }
        _terminalDisplayTarget.value = newTarget
    }
}
