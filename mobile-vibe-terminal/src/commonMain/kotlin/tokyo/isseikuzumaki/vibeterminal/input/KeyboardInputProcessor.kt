package tokyo.isseikuzumaki.vibeterminal.input

// ModifierBitmask is in the same package — no explicit import needed

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
    fun processKeyEvent(event: KeyEventData, uiModifierBitmask: Int = 0): KeyResult {
        // Only handle key down events
        if (event.action != KeyAction.DOWN) {
            return KeyResult.Ignored
        }

        // Skip pure modifier key presses
        if (isModifierKey(event.keyCode)) {
            return KeyResult.Ignored
        }

        // Combine hardware modifier flags with UI toggle bitmask
        val combined = uiModifierBitmask or
                (if (event.isShiftPressed) 1 else 0) or
                (if (event.isAltPressed)   2 else 0) or
                (if (event.isCtrlPressed)  4 else 0) or
                (if (event.isMetaPressed)  8 else 0)

        // Handle special keys (modifier-aware)
        val specialSequence = getSpecialKeySequence(event.keyCode, combined)
        if (specialSequence != null) {
            return KeyResult.Handled(specialSequence)
        }

        val hasCtrl = (combined and 4) != 0
        val hasAlt  = (combined and 2) != 0
        val hasShift = (combined and 1) != 0

        // Handle Alt+Ctrl+key (ESC + control byte) — must check before Ctrl-only branch
        if (hasAlt && hasCtrl) {
            val ctrlSequence = getCtrlKeySequence(event.keyCode)
            if (ctrlSequence != null) {
                return KeyResult.Handled("\u001B$ctrlSequence")
            }
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
     * Get terminal escape sequence for special keys, taking the combined modifier bitmask
     * into account. Returns modifier-parameterized CSI sequences when modifiers are active.
     *
     * @param keyCode       Platform-independent key code from [KeyCodes]
     * @param combinedBitmask Bitwise OR of hardware and UI modifier bits (Shift=1, Alt=2, Ctrl=4, Meta=8)
     */
    fun getSpecialKeySequence(keyCode: Int, combinedBitmask: Int): String? {
        val hasShift = (combinedBitmask and 1) != 0
        val hasCtrl  = (combinedBitmask and 4) != 0
        val modNum   = ModifierBitmask.toModifierNumber(combinedBitmask)

        return when (keyCode) {
            // Arrow keys — modifier-aware CSI parameterized sequences
            KeyCodes.DPAD_UP    -> if (modNum > 0) "\u001B[1;${modNum}A" else "\u001B[A"
            KeyCodes.DPAD_DOWN  -> if (modNum > 0) "\u001B[1;${modNum}B" else "\u001B[B"
            KeyCodes.DPAD_RIGHT -> if (modNum > 0) "\u001B[1;${modNum}C" else "\u001B[C"
            KeyCodes.DPAD_LEFT  -> if (modNum > 0) "\u001B[1;${modNum}D" else "\u001B[D"

            // Home / End — modifier-aware
            KeyCodes.MOVE_HOME -> if (modNum > 0) "\u001B[1;${modNum}H" else "\u001B[H"
            KeyCodes.MOVE_END  -> if (modNum > 0) "\u001B[1;${modNum}F" else "\u001B[F"

            // Page Up / Page Down — modifier-aware
            KeyCodes.PAGE_UP   -> if (modNum > 0) "\u001B[5;${modNum}~" else "\u001B[5~"
            KeyCodes.PAGE_DOWN -> if (modNum > 0) "\u001B[6;${modNum}~" else "\u001B[6~"

            // Insert / Delete — modifier-aware
            KeyCodes.INSERT      -> if (modNum > 0) "\u001B[2;${modNum}~" else "\u001B[2~"
            KeyCodes.FORWARD_DEL -> if (modNum > 0) "\u001B[3;${modNum}~" else "\u001B[3~"

            // Enter/Return
            KeyCodes.ENTER,
            KeyCodes.NUMPAD_ENTER -> "\r"

            // Tab — Shift+Tab = backtab
            KeyCodes.TAB -> if (hasShift) "\u001B[Z" else "\t"

            // Escape
            KeyCodes.ESCAPE -> "\u001B"

            // Backspace — Ctrl+Backspace = 0x08; plain = DEL (0x7F)
            KeyCodes.DEL -> if (hasCtrl) "\u0008" else "\u007F"

            // Function keys — modifier-aware
            // F1–F4: unmodified use SS3 (ESC O); modified use CSI with key numbers 11–14
            KeyCodes.F1  -> if (modNum > 0) "\u001B[11;${modNum}~" else "\u001BOP"
            KeyCodes.F2  -> if (modNum > 0) "\u001B[12;${modNum}~" else "\u001BOQ"
            KeyCodes.F3  -> if (modNum > 0) "\u001B[13;${modNum}~" else "\u001BOR"
            KeyCodes.F4  -> if (modNum > 0) "\u001B[14;${modNum}~" else "\u001BOS"
            // F5–F12: always CSI tilde
            KeyCodes.F5  -> if (modNum > 0) "\u001B[15;${modNum}~" else "\u001B[15~"
            KeyCodes.F6  -> if (modNum > 0) "\u001B[17;${modNum}~" else "\u001B[17~"
            KeyCodes.F7  -> if (modNum > 0) "\u001B[18;${modNum}~" else "\u001B[18~"
            KeyCodes.F8  -> if (modNum > 0) "\u001B[19;${modNum}~" else "\u001B[19~"
            KeyCodes.F9  -> if (modNum > 0) "\u001B[20;${modNum}~" else "\u001B[20~"
            KeyCodes.F10 -> if (modNum > 0) "\u001B[21;${modNum}~" else "\u001B[21~"
            KeyCodes.F11 -> if (modNum > 0) "\u001B[23;${modNum}~" else "\u001B[23~"
            KeyCodes.F12 -> if (modNum > 0) "\u001B[24;${modNum}~" else "\u001B[24~"

            else -> null
        }
    }

    /**
     * Maps a printable character to its Ctrl control byte (char-based IME path).
     *
     * Covers:
     * - Letters a–z / A–Z → \u0001–\u001A
     * - Symbols: [ → \u001B, \ → \u001C, ] → \u001D, 6 → \u001E, - → \u001F, space → \u0000
     *
     * Returns null for characters with no defined Ctrl mapping.
     * Produces identical output to [getCtrlKeySequence] for overlapping inputs.
     */
    fun getCtrlSequenceForChar(char: Char): String? {
        return when (char.lowercaseChar()) {
            in 'a'..'z' -> ((char.uppercaseChar().code - 'A'.code + 1).toChar()).toString()
            '[' -> "\u001B"
            '\\' -> "\u001C"
            ']' -> "\u001D"
            '6' -> "\u001E"
            '-' -> "\u001F"
            ' ' -> "\u0000"
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
