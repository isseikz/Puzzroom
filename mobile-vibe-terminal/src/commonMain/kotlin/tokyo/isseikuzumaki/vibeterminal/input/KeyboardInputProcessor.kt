package tokyo.isseikuzumaki.vibeterminal.input

/**
 * Platform-independent keyboard input processor.
 * Converts key events to terminal escape sequences.
 *
 * This class contains all the business logic for keyboard handling,
 * allowing it to be tested without Android dependencies.
 */
object KeyboardInputProcessor {

    /**
     * Result of key event processing.
     */
    sealed class KeyResult {
        /** Key was handled, send this sequence to terminal */
        data class Handled(val sequence: String) : KeyResult()
        /** Key should be ignored (modifier key only, etc.) */
        data object Ignored : KeyResult()
        /** Key should be passed to default handler */
        data object PassThrough : KeyResult()
    }

    /**
     * Process a key event and return the appropriate terminal sequence.
     *
     * @param event The platform-independent KeyEventData
     * @return KeyResult indicating how the key was handled
     */
    fun processKeyEvent(event: KeyEventData): KeyResult {
        // Only handle key down events
        if (event.action != KeyAction.DOWN) {
            return KeyResult.Ignored
        }

        // Skip pure modifier key presses
        if (isModifierKey(event.keyCode)) {
            return KeyResult.Ignored
        }

        // Handle special keys first
        val specialSequence = getSpecialKeySequence(event.keyCode, event.isShiftPressed)
        if (specialSequence != null) {
            return KeyResult.Handled(specialSequence)
        }

        // Handle Ctrl+key combinations
        if (event.isCtrlPressed) {
            val ctrlSequence = getCtrlKeySequence(event.keyCode)
            if (ctrlSequence != null) {
                return KeyResult.Handled(ctrlSequence)
            }
        }

        // Handle Alt+key combinations (send ESC prefix)
        if (event.isAltPressed) {
            val char = getCharFromKeyCode(event.keyCode, event.isShiftPressed)
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
    fun isModifierKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyCodes.SHIFT_LEFT,
            KeyCodes.SHIFT_RIGHT,
            KeyCodes.CTRL_LEFT,
            KeyCodes.CTRL_RIGHT,
            KeyCodes.ALT_LEFT,
            KeyCodes.ALT_RIGHT,
            KeyCodes.META_LEFT,
            KeyCodes.META_RIGHT,
            KeyCodes.CAPS_LOCK,
            KeyCodes.NUM_LOCK,
            KeyCodes.SCROLL_LOCK,
            KeyCodes.FUNCTION -> true
            else -> false
        }
    }

    /**
     * Get terminal escape sequence for special keys.
     */
    fun getSpecialKeySequence(keyCode: Int, hasShift: Boolean): String? {
        return when (keyCode) {
            // Arrow keys
            KeyCodes.DPAD_UP -> "\u001B[A"
            KeyCodes.DPAD_DOWN -> "\u001B[B"
            KeyCodes.DPAD_RIGHT -> "\u001B[C"
            KeyCodes.DPAD_LEFT -> "\u001B[D"

            // Navigation keys
            KeyCodes.MOVE_HOME -> "\u001B[H"
            KeyCodes.MOVE_END -> "\u001B[F"
            KeyCodes.PAGE_UP -> "\u001B[5~"
            KeyCodes.PAGE_DOWN -> "\u001B[6~"
            KeyCodes.INSERT -> "\u001B[2~"
            KeyCodes.FORWARD_DEL -> "\u001B[3~"  // Delete key

            // Enter/Return
            KeyCodes.ENTER,
            KeyCodes.NUMPAD_ENTER -> "\r"

            // Tab
            KeyCodes.TAB -> if (hasShift) "\u001B[Z" else "\t"

            // Escape
            KeyCodes.ESCAPE -> "\u001B"

            // Backspace
            KeyCodes.DEL -> "\u007F"  // DEL character for backspace

            // Function keys (F1-F12)
            KeyCodes.F1 -> "\u001BOP"
            KeyCodes.F2 -> "\u001BOQ"
            KeyCodes.F3 -> "\u001BOR"
            KeyCodes.F4 -> "\u001BOS"
            KeyCodes.F5 -> "\u001B[15~"
            KeyCodes.F6 -> "\u001B[17~"
            KeyCodes.F7 -> "\u001B[18~"
            KeyCodes.F8 -> "\u001B[19~"
            KeyCodes.F9 -> "\u001B[20~"
            KeyCodes.F10 -> "\u001B[21~"
            KeyCodes.F11 -> "\u001B[23~"
            KeyCodes.F12 -> "\u001B[24~"

            else -> null
        }
    }

    /**
     * Get Ctrl+key sequence (ASCII control characters).
     */
    fun getCtrlKeySequence(keyCode: Int): String? {
        // Ctrl+A through Ctrl+Z map to ASCII 1-26
        val char = when (keyCode) {
            KeyCodes.A -> 1
            KeyCodes.B -> 2
            KeyCodes.C -> 3  // SIGINT
            KeyCodes.D -> 4  // EOF
            KeyCodes.E -> 5
            KeyCodes.F -> 6
            KeyCodes.G -> 7  // Bell
            KeyCodes.H -> 8  // Backspace
            KeyCodes.I -> 9  // Tab
            KeyCodes.J -> 10 // Line feed
            KeyCodes.K -> 11
            KeyCodes.L -> 12 // Form feed (clear screen)
            KeyCodes.M -> 13 // Carriage return
            KeyCodes.N -> 14
            KeyCodes.O -> 15
            KeyCodes.P -> 16
            KeyCodes.Q -> 17 // XON
            KeyCodes.R -> 18 // Redo in some apps
            KeyCodes.S -> 19 // XOFF
            KeyCodes.T -> 20
            KeyCodes.U -> 21 // Kill line
            KeyCodes.V -> 22
            KeyCodes.W -> 23 // Kill word
            KeyCodes.X -> 24
            KeyCodes.Y -> 25
            KeyCodes.Z -> 26 // SIGTSTP (suspend)
            KeyCodes.LEFT_BRACKET -> 27  // Ctrl+[ = ESC
            KeyCodes.BACKSLASH -> 28
            KeyCodes.RIGHT_BRACKET -> 29
            KeyCodes.NUM_6 -> 30  // Ctrl+^ (Ctrl+6)
            KeyCodes.MINUS -> 31  // Ctrl+_
            KeyCodes.SPACE -> 0  // Ctrl+Space = NUL
            else -> null
        }
        return char?.toChar()?.toString()
    }

    /**
     * Get character from key code, considering shift state.
     * Optimized for Apple Magic Keyboard JIS layout.
     */
    fun getCharFromKeyCode(keyCode: Int, hasShift: Boolean): Char? {
        return when (keyCode) {
            // Letters
            KeyCodes.A -> if (hasShift) 'A' else 'a'
            KeyCodes.B -> if (hasShift) 'B' else 'b'
            KeyCodes.C -> if (hasShift) 'C' else 'c'
            KeyCodes.D -> if (hasShift) 'D' else 'd'
            KeyCodes.E -> if (hasShift) 'E' else 'e'
            KeyCodes.F -> if (hasShift) 'F' else 'f'
            KeyCodes.G -> if (hasShift) 'G' else 'g'
            KeyCodes.H -> if (hasShift) 'H' else 'h'
            KeyCodes.I -> if (hasShift) 'I' else 'i'
            KeyCodes.J -> if (hasShift) 'J' else 'j'
            KeyCodes.K -> if (hasShift) 'K' else 'k'
            KeyCodes.L -> if (hasShift) 'L' else 'l'
            KeyCodes.M -> if (hasShift) 'M' else 'm'
            KeyCodes.N -> if (hasShift) 'N' else 'n'
            KeyCodes.O -> if (hasShift) 'O' else 'o'
            KeyCodes.P -> if (hasShift) 'P' else 'p'
            KeyCodes.Q -> if (hasShift) 'Q' else 'q'
            KeyCodes.R -> if (hasShift) 'R' else 'r'
            KeyCodes.S -> if (hasShift) 'S' else 's'
            KeyCodes.T -> if (hasShift) 'T' else 't'
            KeyCodes.U -> if (hasShift) 'U' else 'u'
            KeyCodes.V -> if (hasShift) 'V' else 'v'
            KeyCodes.W -> if (hasShift) 'W' else 'w'
            KeyCodes.X -> if (hasShift) 'X' else 'x'
            KeyCodes.Y -> if (hasShift) 'Y' else 'y'
            KeyCodes.Z -> if (hasShift) 'Z' else 'z'

            // Numbers (JIS layout: Shift produces symbols)
            KeyCodes.NUM_1 -> if (hasShift) '!' else '1'
            KeyCodes.NUM_2 -> if (hasShift) '"' else '2'  // JIS: Shift+2 = "
            KeyCodes.NUM_3 -> if (hasShift) '#' else '3'
            KeyCodes.NUM_4 -> if (hasShift) '$' else '4'
            KeyCodes.NUM_5 -> if (hasShift) '%' else '5'
            KeyCodes.NUM_6 -> if (hasShift) '&' else '6'  // JIS: Shift+6 = &
            KeyCodes.NUM_7 -> if (hasShift) '\'' else '7' // JIS: Shift+7 = '
            KeyCodes.NUM_8 -> if (hasShift) '(' else '8'  // JIS: Shift+8 = (
            KeyCodes.NUM_9 -> if (hasShift) ')' else '9'  // JIS: Shift+9 = )
            KeyCodes.NUM_0 -> if (hasShift) '~' else '0'  // JIS: Shift+0 = ~ (varies)

            // Symbols (JIS layout)
            KeyCodes.MINUS -> if (hasShift) '=' else '-'
            KeyCodes.EQUALS -> if (hasShift) '+' else '^'  // JIS: ^ key
            KeyCodes.LEFT_BRACKET -> if (hasShift) '{' else '['
            KeyCodes.RIGHT_BRACKET -> if (hasShift) '}' else ']'
            KeyCodes.BACKSLASH -> if (hasShift) '|' else '\\'
            KeyCodes.SEMICOLON -> if (hasShift) '+' else ';'  // JIS layout
            KeyCodes.APOSTROPHE -> if (hasShift) '*' else ':'  // JIS: : and *
            KeyCodes.COMMA -> if (hasShift) '<' else ','
            KeyCodes.PERIOD -> if (hasShift) '>' else '.'
            KeyCodes.SLASH -> if (hasShift) '?' else '/'
            KeyCodes.GRAVE -> if (hasShift) '~' else '`'

            // JIS-specific keys
            KeyCodes.YEN -> if (hasShift) '|' else '\\'  // ¥ key on JIS
            KeyCodes.RO -> if (hasShift) '_' else '\\'   // Backslash/underscore on JIS

            else -> null
        }
    }
}
