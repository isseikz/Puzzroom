package tokyo.isseikuzumaki.vibeterminal.input

/**
 * Combines modifier state from all input sources into a single xterm-convention bitmask.
 *
 * Bit assignments (xterm standard):
 * - Shift = 1
 * - Alt   = 2
 * - Ctrl  = 4
 * - Meta  = 8
 *
 * Combinations are additive (bitwise OR). Duplicate sources for the same modifier
 * do not double-count (idempotent OR).
 */
object ModifierBitmask {

    /**
     * Combines hardware modifier flags from a [KeyEventData] with the current
     * states of the three on-screen modifier buttons via bitwise OR.
     *
     * @param keyEventData The key event carrying hardware modifier flags
     * @param shiftState   On-screen Shift button state
     * @param ctrlState    On-screen Ctrl button state
     * @param altState     On-screen Alt button state
     * @return Combined bitmask in range [0, 15]
     */
    fun combine(
        keyEventData: KeyEventData,
        shiftState: ModifierButtonState,
        ctrlState: ModifierButtonState,
        altState: ModifierButtonState
    ): Int {
        var bitmask = 0
        // Hardware modifier flags
        if (keyEventData.isShiftPressed) bitmask = bitmask or 1
        if (keyEventData.isAltPressed)   bitmask = bitmask or 2
        if (keyEventData.isCtrlPressed)  bitmask = bitmask or 4
        if (keyEventData.isMetaPressed)  bitmask = bitmask or 8
        // UI button states (ONE_SHOT or LOCKED contribute their bit)
        if (shiftState.isActive) bitmask = bitmask or 1
        if (altState.isActive)   bitmask = bitmask or 2
        if (ctrlState.isActive)  bitmask = bitmask or 4
        return bitmask
    }

    /**
     * Converts a combined modifier bitmask to the xterm modifier parameter number.
     *
     * xterm convention: modifier_number = bitmask + 1 (for bitmask > 0).
     * Returns 0 for bitmask == 0 as a sentinel meaning "no modifier parameter".
     *
     * @param bitmask Combined modifier bitmask in [0, 15]
     * @return xterm modifier number (0 = no modifier; 2 = Shift; 5 = Ctrl; etc.)
     */
    fun toModifierNumber(bitmask: Int): Int {
        return if (bitmask > 0) bitmask + 1 else 0
    }
}
