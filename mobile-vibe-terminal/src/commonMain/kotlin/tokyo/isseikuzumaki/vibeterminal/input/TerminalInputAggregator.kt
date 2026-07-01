package tokyo.isseikuzumaki.vibeterminal.input

/**
 * Aggregates terminal input from all UI sources into a single resolved sequence
 * before dispatching to the SSH channel.
 *
 * Responsibilities:
 *  - Apply the current modifier state (Shift/Ctrl/Alt) to raw input
 *  - Consume ONE_SHOT modifiers after each key event
 *  - Call [onDispatch] with the final resolved sequence
 *
 * Two entry points:
 *  - [send] — for UI sources (IME, FixedKeyRow, macro buttons): applies modifiers
 *  - [dispatch] — for pre-resolved sources (hardware keyboard): skips modifier
 *                 application but still consumes ONE_SHOT modifier state
 *
 * All modifier transformation logic is pure (no side-effects) and fully unit-testable.
 *
 * @param getModifierBitmask  Returns the current combined UI modifier bitmask (Shift=1, Alt=2, Ctrl=4)
 * @param consumeOneShotModifiers  Resets all ONE_SHOT modifiers to INACTIVE; leaves LOCKED untouched
 * @param onDispatch  Sends the resolved byte sequence to the SSH channel (suspend)
 */
class TerminalInputAggregator(
    private val getModifierBitmask: () -> Int,
    private val consumeOneShotModifiers: () -> Unit,
    private val onDispatch: suspend (String) -> Unit
) {
    /**
     * Apply active modifier state to [rawSequence], consume ONE_SHOT modifiers,
     * then dispatch. Use for all UI-sourced input (IME text, FixedKeyRow, macro buttons).
     */
    suspend fun send(rawSequence: String) {
        if (rawSequence.isEmpty()) return
        val bitmask = getModifierBitmask()
        val resolved = applyModifiers(rawSequence, bitmask)
        if (bitmask != 0) consumeOneShotModifiers()
        onDispatch(resolved)
    }

    /**
     * Skip modifier application and dispatch [resolvedSequence] directly.
     * Use when the caller has already resolved modifiers (e.g. hardware keyboard
     * via [KeyboardInputProcessor]). Still consumes ONE_SHOT UI modifier state
     * so on-screen toggle buttons reset after a hardware key press.
     */
    suspend fun dispatch(resolvedSequence: String) {
        if (resolvedSequence.isEmpty()) return
        val bitmask = getModifierBitmask()
        if (bitmask != 0) consumeOneShotModifiers()
        onDispatch(resolvedSequence)
    }

    // ── Modifier application ──────────────────────────────────────────────────

    private fun applyModifiers(raw: String, bitmask: Int): String {
        if (bitmask == 0) return raw
        // Tab is a single char but has escape-sequence modifier mappings (Shift+Tab = backtab)
        return if (raw.length == 1 && raw[0] != '\t') applyModifierToChar(raw[0], bitmask)
               else applyModifierToSequence(raw, bitmask)
    }

    /**
     * Apply modifier bitmask to a single character from IME input.
     *
     * Rules (checked in priority order):
     *  - Ctrl + Alt + char → ESC + control-byte
     *  - Ctrl + char       → control-byte (0x01–0x1A for a–z)
     *  - Alt  + char       → ESC + char
     */
    internal fun applyModifierToChar(char: Char, bitmask: Int): String {
        val hasCtrl = (bitmask and 4) != 0
        val hasAlt  = (bitmask and 2) != 0
        return when {
            hasCtrl && hasAlt -> {
                val ctrl = KeyboardInputProcessor.getCtrlSequenceForChar(char)
                if (ctrl != null) "\u001B$ctrl" else "\u001B$char"
            }
            hasCtrl -> KeyboardInputProcessor.getCtrlSequenceForChar(char) ?: char.toString()
            hasAlt  -> "\u001B$char"
            else    -> char.toString()
        }
    }

    /**
     * Apply modifier bitmask to a known escape sequence (from FixedKeyRow / macro buttons).
     * Only transforms sequences that have a defined xterm modifier encoding.
     * Unknown sequences are returned unchanged.
     */
    internal fun applyModifierToSequence(raw: String, bitmask: Int): String {
        val modNum   = ModifierBitmask.toModifierNumber(bitmask)
        val hasShift = (bitmask and 1) != 0
        return when (raw) {
            "\t"         -> if (hasShift) "\u001B[Z" else raw   // Shift+Tab → backtab
            "\u001B[A"   -> "\u001B[1;${modNum}A"               // Up
            "\u001B[B"   -> "\u001B[1;${modNum}B"               // Down
            "\u001B[C"   -> "\u001B[1;${modNum}C"               // Right
            "\u001B[D"   -> "\u001B[1;${modNum}D"               // Left
            "\u001B[H"   -> "\u001B[1;${modNum}H"               // Home
            "\u001B[F"   -> "\u001B[1;${modNum}F"               // End
            else         -> raw
        }
    }
}
