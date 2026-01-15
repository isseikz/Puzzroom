package tokyo.isseikuzumaki.vibeterminal.terminal

import tokyo.isseikuzumaki.vibeterminal.util.Logger

/**
 * Handles mouse input events and converts them to escape sequences.
 *
 * This class manages mouse tracking and encoding modes, and generates
 * appropriate escape sequences when mouse events occur.
 *
 * Communication flow:
 * 1. Server sends ESC[?1002h (enable tracking) and ESC[?1006h (enable SGR)
 * 2. User performs scroll gesture
 * 3. This handler generates ESC[<Cb;Cx;CyM and sends to server
 * 4. Server processes and updates screen
 */
class MouseInputHandler(
    private val sendToRemote: (String) -> Unit
) {
    /** Current mouse tracking mode (WHAT events to report) */
    var trackingMode: MouseTrackingMode = MouseTrackingMode.NONE
        private set

    /** Current mouse encoding mode (HOW events are formatted) */
    var encodingMode: MouseEncodingMode = MouseEncodingMode.NORMAL
        private set

    /** Whether the terminal is in alternate screen mode */
    var isAlternateScreen: Boolean = false

    /**
     * Enable a mouse tracking mode.
     * Called when receiving DECSET commands (ESC[?{mode}h)
     */
    fun enableTrackingMode(mode: Int) {
        trackingMode = when (mode) {
            1000 -> MouseTrackingMode.NORMAL
            1002 -> MouseTrackingMode.BUTTON_EVENT
            1003 -> MouseTrackingMode.ANY_EVENT
            else -> return
        }
        Logger.d("MouseInputHandler: Tracking mode enabled: $trackingMode (mode=$mode)")
    }

    /**
     * Disable a mouse tracking mode.
     * Called when receiving DECRST commands (ESC[?{mode}l)
     */
    fun disableTrackingMode(mode: Int) {
        when (mode) {
            1000, 1002, 1003 -> {
                Logger.d("MouseInputHandler: Tracking mode disabled (was: $trackingMode)")
                trackingMode = MouseTrackingMode.NONE
            }
        }
    }

    /**
     * Enable SGR extended mouse encoding.
     * Called when receiving ESC[?1006h
     */
    fun enableSgrEncoding() {
        encodingMode = MouseEncodingMode.SGR
        Logger.d("MouseInputHandler: SGR encoding enabled")
    }

    /**
     * Disable SGR extended mouse encoding.
     * Called when receiving ESC[?1006l
     */
    fun disableSgrEncoding() {
        encodingMode = MouseEncodingMode.NORMAL
        Logger.d("MouseInputHandler: SGR encoding disabled")
    }

    /**
     * Handle scroll event from UI.
     *
     * @param direction Scroll direction (UP or DOWN)
     * @param col Column position (1-based)
     * @param row Row position (1-based)
     * @return true if event was handled and sent to remote, false otherwise
     */
    fun onScroll(direction: ScrollDirection, col: Int, row: Int): Boolean {
        // Only send mouse events if tracking is enabled
        if (trackingMode == MouseTrackingMode.NONE) {
            Logger.d("MouseInputHandler: Scroll ignored - tracking mode is NONE")
            return false
        }

        // In primary screen, we might want to handle scrollback locally (Phase 2)
        // For now, we send to remote if tracking is enabled
        val buttonCode = when (direction) {
            ScrollDirection.UP -> 64
            ScrollDirection.DOWN -> 65
        }

        sendMouseEvent(buttonCode, col, row, pressed = true)
        return true
    }

    /**
     * Handle button press event from UI.
     *
     * @param button Button number (0=left, 1=middle, 2=right)
     * @param col Column position (1-based)
     * @param row Row position (1-based)
     * @param modifiers Modifier keys (bit flags: 4=shift, 8=meta, 16=ctrl)
     */
    fun onButtonPress(button: Int, col: Int, row: Int, modifiers: Int = 0) {
        if (trackingMode == MouseTrackingMode.NONE) return

        val buttonCode = button + modifiers
        sendMouseEvent(buttonCode, col, row, pressed = true)
    }

    /**
     * Handle button release event from UI.
     *
     * @param button Button number (0=left, 1=middle, 2=right)
     * @param col Column position (1-based)
     * @param row Row position (1-based)
     * @param modifiers Modifier keys (bit flags: 4=shift, 8=meta, 16=ctrl)
     */
    fun onButtonRelease(button: Int, col: Int, row: Int, modifiers: Int = 0) {
        if (trackingMode == MouseTrackingMode.NONE) return

        val buttonCode = button + modifiers
        sendMouseEvent(buttonCode, col, row, pressed = false)
    }

    /**
     * Handle mouse motion event from UI.
     * Only sent when trackingMode is BUTTON_EVENT (with button held) or ANY_EVENT.
     *
     * @param col Column position (1-based)
     * @param row Row position (1-based)
     * @param buttonHeld Button currently held (-1 if none)
     * @param modifiers Modifier keys
     */
    fun onMotion(col: Int, row: Int, buttonHeld: Int, modifiers: Int = 0) {
        when (trackingMode) {
            MouseTrackingMode.NONE, MouseTrackingMode.NORMAL -> return
            MouseTrackingMode.BUTTON_EVENT -> {
                if (buttonHeld < 0) return // No button held, don't report
            }
            MouseTrackingMode.ANY_EVENT -> {
                // Report all motion
            }
        }

        // Motion events add 32 to button code
        val buttonCode = (if (buttonHeld >= 0) buttonHeld else 3) + 32 + modifiers
        sendMouseEvent(buttonCode, col, row, pressed = true)
    }

    /**
     * Send a mouse event to the remote server.
     */
    private fun sendMouseEvent(buttonCode: Int, col: Int, row: Int, pressed: Boolean) {
        val sequence = when (encodingMode) {
            MouseEncodingMode.SGR -> {
                // SGR format: ESC [ < Cb ; Cx ; Cy M/m
                val suffix = if (pressed) "M" else "m"
                "\u001b[<$buttonCode;$col;$row$suffix"
            }
            MouseEncodingMode.NORMAL -> {
                // Legacy format: ESC [ M Cb Cx Cy
                // Add 32 to each value for legacy encoding
                val cb = (buttonCode + 32).toChar()
                val cx = (col + 32).coerceAtMost(255).toChar()
                val cy = (row + 32).coerceAtMost(255).toChar()
                "\u001b[M$cb$cx$cy"
            }
        }

        Logger.d("MouseInputHandler: Sending mouse event: ${sequence.replace("\u001b", "ESC")}")
        sendToRemote(sequence)
    }

    /**
     * Reset all mouse modes to default state.
     * Called when terminal is reset or connection is closed.
     */
    fun reset() {
        trackingMode = MouseTrackingMode.NONE
        encodingMode = MouseEncodingMode.NORMAL
        isAlternateScreen = false
        Logger.d("MouseInputHandler: Reset to default state")
    }
}
