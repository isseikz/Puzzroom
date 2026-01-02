package tokyo.isseikuzumaki.vibeterminal.input

import android.view.KeyEvent
import tokyo.isseikuzumaki.vibeterminal.util.Logger

/**
 * Handler for hardware keyboard input, optimized for Apple Magic Keyboard JIS.
 * Converts Android KeyEvents to terminal escape sequences.
 */
object HardwareKeyboardHandler {

    /**
     * Result of key event processing.
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
        // Only handle key down events
        if (event.action != KeyEvent.ACTION_DOWN) {
            return KeyResult.Ignored
        }

        // Skip pure modifier key presses
        if (isModifierKey(event.keyCode)) {
            return KeyResult.Ignored
        }

        val hasCtrl = event.isCtrlPressed
        val hasAlt = event.isAltPressed
        val hasShift = event.isShiftPressed
        val hasMeta = event.isMetaPressed // Command key on Mac keyboards

        Logger.d("HardwareKeyboardHandler: keyCode=${event.keyCode}, ctrl=$hasCtrl, alt=$hasAlt, shift=$hasShift, meta=$hasMeta")

        // Handle special keys first
        val specialSequence = getSpecialKeySequence(event.keyCode, hasShift, hasCtrl, hasAlt)
        if (specialSequence != null) {
            return KeyResult.Handled(specialSequence)
        }

        // Handle Ctrl+key combinations
        if (hasCtrl) {
            val ctrlSequence = getCtrlKeySequence(event.keyCode)
            if (ctrlSequence != null) {
                return KeyResult.Handled(ctrlSequence)
            }
        }

        // Handle Alt+key combinations (send ESC prefix)
        if (hasAlt) {
            val char = getCharFromKeyCode(event.keyCode, hasShift)
            if (char != null) {
                return KeyResult.Handled("\u001B$char")
            }
        }

        // Regular characters are handled by the system/Compose (InputConnection)
        // to avoid double input when TerminalInputContainer is active.
        return KeyResult.PassThrough
    }

    /**
     * Check if the key code is a modifier key only.
     */
    private fun isModifierKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_META_LEFT,
            KeyEvent.KEYCODE_META_RIGHT,
            KeyEvent.KEYCODE_CAPS_LOCK,
            KeyEvent.KEYCODE_NUM_LOCK,
            KeyEvent.KEYCODE_SCROLL_LOCK,
            KeyEvent.KEYCODE_FUNCTION -> true
            else -> false
        }
    }

    /**
     * Get terminal escape sequence for special keys.
     */
    private fun getSpecialKeySequence(keyCode: Int, hasShift: Boolean, hasCtrl: Boolean, hasAlt: Boolean): String? {
        return when (keyCode) {
            // Arrow keys
            KeyEvent.KEYCODE_DPAD_UP -> "\u001B[A"
            KeyEvent.KEYCODE_DPAD_DOWN -> "\u001B[B"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "\u001B[C"
            KeyEvent.KEYCODE_DPAD_LEFT -> "\u001B[D"

            // Navigation keys
            KeyEvent.KEYCODE_MOVE_HOME -> "\u001B[H"
            KeyEvent.KEYCODE_MOVE_END -> "\u001B[F"
            KeyEvent.KEYCODE_PAGE_UP -> "\u001B[5~"
            KeyEvent.KEYCODE_PAGE_DOWN -> "\u001B[6~"
            KeyEvent.KEYCODE_INSERT -> "\u001B[2~"
            KeyEvent.KEYCODE_FORWARD_DEL -> "\u001B[3~"  // Delete key

            // Enter/Return
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> "\r"

            // Tab
            KeyEvent.KEYCODE_TAB -> if (hasShift) "\u001B[Z" else "\t"

            // Escape
            KeyEvent.KEYCODE_ESCAPE -> "\u001B"

            // Backspace
            KeyEvent.KEYCODE_DEL -> "\u007F"  // DEL character for backspace

            // Function keys (F1-F12)
            KeyEvent.KEYCODE_F1 -> "\u001BOP"
            KeyEvent.KEYCODE_F2 -> "\u001BOQ"
            KeyEvent.KEYCODE_F3 -> "\u001BOR"
            KeyEvent.KEYCODE_F4 -> "\u001BOS"
            KeyEvent.KEYCODE_F5 -> "\u001B[15~"
            KeyEvent.KEYCODE_F6 -> "\u001B[17~"
            KeyEvent.KEYCODE_F7 -> "\u001B[18~"
            KeyEvent.KEYCODE_F8 -> "\u001B[19~"
            KeyEvent.KEYCODE_F9 -> "\u001B[20~"
            KeyEvent.KEYCODE_F10 -> "\u001B[21~"
            KeyEvent.KEYCODE_F11 -> "\u001B[23~"
            KeyEvent.KEYCODE_F12 -> "\u001B[24~"

            else -> null
        }
    }

    /**
     * Get Ctrl+key sequence (ASCII control characters).
     */
    private fun getCtrlKeySequence(keyCode: Int): String? {
        // Ctrl+A through Ctrl+Z map to ASCII 1-26
        val char = when (keyCode) {
            KeyEvent.KEYCODE_A -> 1
            KeyEvent.KEYCODE_B -> 2
            KeyEvent.KEYCODE_C -> 3  // SIGINT
            KeyEvent.KEYCODE_D -> 4  // EOF
            KeyEvent.KEYCODE_E -> 5
            KeyEvent.KEYCODE_F -> 6
            KeyEvent.KEYCODE_G -> 7  // Bell
            KeyEvent.KEYCODE_H -> 8  // Backspace
            KeyEvent.KEYCODE_I -> 9  // Tab
            KeyEvent.KEYCODE_J -> 10 // Line feed
            KeyEvent.KEYCODE_K -> 11
            KeyEvent.KEYCODE_L -> 12 // Form feed (clear screen)
            KeyEvent.KEYCODE_M -> 13 // Carriage return
            KeyEvent.KEYCODE_N -> 14
            KeyEvent.KEYCODE_O -> 15
            KeyEvent.KEYCODE_P -> 16
            KeyEvent.KEYCODE_Q -> 17 // XON
            KeyEvent.KEYCODE_R -> 18 // Redo in some apps
            KeyEvent.KEYCODE_S -> 19 // XOFF
            KeyEvent.KEYCODE_T -> 20
            KeyEvent.KEYCODE_U -> 21 // Kill line
            KeyEvent.KEYCODE_V -> 22
            KeyEvent.KEYCODE_W -> 23 // Kill word
            KeyEvent.KEYCODE_X -> 24
            KeyEvent.KEYCODE_Y -> 25
            KeyEvent.KEYCODE_Z -> 26 // SIGTSTP (suspend)
            KeyEvent.KEYCODE_LEFT_BRACKET -> 27  // Ctrl+[ = ESC
            KeyEvent.KEYCODE_BACKSLASH -> 28
            KeyEvent.KEYCODE_RIGHT_BRACKET -> 29
            KeyEvent.KEYCODE_6 -> 30  // Ctrl+^ (Ctrl+6)
            KeyEvent.KEYCODE_MINUS -> 31  // Ctrl+_
            KeyEvent.KEYCODE_SPACE -> 0  // Ctrl+Space = NUL
            else -> null
        }
        return char?.toChar()?.toString()
    }

    /**
     * Get character from key code, considering shift state.
     * Optimized for Apple Magic Keyboard JIS layout.
     */
    private fun getCharFromKeyCode(keyCode: Int, hasShift: Boolean): Char? {
        return when (keyCode) {
            // Letters
            KeyEvent.KEYCODE_A -> if (hasShift) 'A' else 'a'
            KeyEvent.KEYCODE_B -> if (hasShift) 'B' else 'b'
            KeyEvent.KEYCODE_C -> if (hasShift) 'C' else 'c'
            KeyEvent.KEYCODE_D -> if (hasShift) 'D' else 'd'
            KeyEvent.KEYCODE_E -> if (hasShift) 'E' else 'e'
            KeyEvent.KEYCODE_F -> if (hasShift) 'F' else 'f'
            KeyEvent.KEYCODE_G -> if (hasShift) 'G' else 'g'
            KeyEvent.KEYCODE_H -> if (hasShift) 'H' else 'h'
            KeyEvent.KEYCODE_I -> if (hasShift) 'I' else 'i'
            KeyEvent.KEYCODE_J -> if (hasShift) 'J' else 'j'
            KeyEvent.KEYCODE_K -> if (hasShift) 'K' else 'k'
            KeyEvent.KEYCODE_L -> if (hasShift) 'L' else 'l'
            KeyEvent.KEYCODE_M -> if (hasShift) 'M' else 'm'
            KeyEvent.KEYCODE_N -> if (hasShift) 'N' else 'n'
            KeyEvent.KEYCODE_O -> if (hasShift) 'O' else 'o'
            KeyEvent.KEYCODE_P -> if (hasShift) 'P' else 'p'
            KeyEvent.KEYCODE_Q -> if (hasShift) 'Q' else 'q'
            KeyEvent.KEYCODE_R -> if (hasShift) 'R' else 'r'
            KeyEvent.KEYCODE_S -> if (hasShift) 'S' else 's'
            KeyEvent.KEYCODE_T -> if (hasShift) 'T' else 't'
            KeyEvent.KEYCODE_U -> if (hasShift) 'U' else 'u'
            KeyEvent.KEYCODE_V -> if (hasShift) 'V' else 'v'
            KeyEvent.KEYCODE_W -> if (hasShift) 'W' else 'w'
            KeyEvent.KEYCODE_X -> if (hasShift) 'X' else 'x'
            KeyEvent.KEYCODE_Y -> if (hasShift) 'Y' else 'y'
            KeyEvent.KEYCODE_Z -> if (hasShift) 'Z' else 'z'

            // Numbers (JIS layout: Shift produces symbols)
            KeyEvent.KEYCODE_1 -> if (hasShift) '!' else '1'
            KeyEvent.KEYCODE_2 -> if (hasShift) '"' else '2'  // JIS: Shift+2 = "
            KeyEvent.KEYCODE_3 -> if (hasShift) '#' else '3'
            KeyEvent.KEYCODE_4 -> if (hasShift) '$' else '4'
            KeyEvent.KEYCODE_5 -> if (hasShift) '%' else '5'
            KeyEvent.KEYCODE_6 -> if (hasShift) '&' else '6'  // JIS: Shift+6 = &
            KeyEvent.KEYCODE_7 -> if (hasShift) '\'' else '7' // JIS: Shift+7 = '
            KeyEvent.KEYCODE_8 -> if (hasShift) '(' else '8'  // JIS: Shift+8 = (
            KeyEvent.KEYCODE_9 -> if (hasShift) ')' else '9'  // JIS: Shift+9 = )
            KeyEvent.KEYCODE_0 -> if (hasShift) '~' else '0'  // JIS: Shift+0 = ~ (varies)

            // Symbols (JIS layout)
            KeyEvent.KEYCODE_MINUS -> if (hasShift) '=' else '-'
            KeyEvent.KEYCODE_EQUALS -> if (hasShift) '+' else '^'  // JIS: ^ key
            KeyEvent.KEYCODE_LEFT_BRACKET -> if (hasShift) '{' else '['
            KeyEvent.KEYCODE_RIGHT_BRACKET -> if (hasShift) '}' else ']'
            KeyEvent.KEYCODE_BACKSLASH -> if (hasShift) '|' else '\\'
            KeyEvent.KEYCODE_SEMICOLON -> if (hasShift) '+' else ';'  // JIS layout
            KeyEvent.KEYCODE_APOSTROPHE -> if (hasShift) '*' else ':'  // JIS: : and *
            KeyEvent.KEYCODE_COMMA -> if (hasShift) '<' else ','
            KeyEvent.KEYCODE_PERIOD -> if (hasShift) '>' else '.'
            KeyEvent.KEYCODE_SLASH -> if (hasShift) '?' else '/'
            KeyEvent.KEYCODE_GRAVE -> if (hasShift) '~' else '`'

            // JIS-specific keys
            KeyEvent.KEYCODE_YEN -> if (hasShift) '|' else '\\'  // ¥ key on JIS
            KeyEvent.KEYCODE_RO -> if (hasShift) '_' else '\\'   // Backslash/underscore on JIS

            else -> null
        }
    }
}
