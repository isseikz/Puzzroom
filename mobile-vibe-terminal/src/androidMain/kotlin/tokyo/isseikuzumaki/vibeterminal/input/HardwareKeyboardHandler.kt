package tokyo.isseikuzumaki.vibeterminal.input

import android.view.KeyEvent
import tokyo.isseikuzumaki.vibeterminal.util.Logger

/**
 * Android-specific adapter for hardware keyboard input.
 * Converts Android KeyEvents to platform-independent KeyEventData
 * and delegates processing to KeyboardInputProcessor.
 */
object HardwareKeyboardHandler {

    /**
     * Result of key event processing.
     * Wraps KeyboardInputProcessor.KeyResult for Android compatibility.
     */
    sealed class KeyResult {
        /** Key was handled, send this sequence to terminal */
        data class Handled(val sequence: String) : KeyResult()
        /** Key should be ignored (modifier key only, etc.) */
        object Ignored : KeyResult()
        /** Key should be passed to default handler */
        object PassThrough : KeyResult()
    }

    /**
     * Process a key event and return the appropriate terminal sequence.
     *
     * @param event The Android KeyEvent
     * @param isCommandMode Whether the terminal is in command mode (RAW mode)
     * @return KeyResult indicating how the key was handled
     */
    fun processKeyEvent(event: KeyEvent, isCommandMode: Boolean): KeyResult {
        Logger.d("HardwareKeyboardHandler: keyCode=${event.keyCode}, ctrl=${event.isCtrlPressed}, alt=${event.isAltPressed}, shift=${event.isShiftPressed}, meta=${event.isMetaPressed}")

        // Convert Android KeyEvent to platform-independent KeyEventData
        val keyEventData = event.toKeyEventData()

        // Delegate to platform-independent processor
        return when (val result = KeyboardInputProcessor.processKeyEvent(keyEventData)) {
            is KeyboardInputProcessor.KeyResult.Handled -> KeyResult.Handled(result.sequence)
            is KeyboardInputProcessor.KeyResult.Ignored -> KeyResult.Ignored
            is KeyboardInputProcessor.KeyResult.PassThrough -> KeyResult.PassThrough
        }
    }

    /**
     * Convert Android KeyEvent to platform-independent KeyEventData.
     */
    private fun KeyEvent.toKeyEventData(): KeyEventData {
        return KeyEventData(
            keyCode = this.keyCode,
            action = when (this.action) {
                KeyEvent.ACTION_DOWN -> KeyAction.DOWN
                else -> KeyAction.UP
            },
            isCtrlPressed = this.isCtrlPressed,
            isAltPressed = this.isAltPressed,
            isShiftPressed = this.isShiftPressed,
            isMetaPressed = this.isMetaPressed
        )
    }
}
