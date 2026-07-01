package tokyo.isseikuzumaki.vibeterminal.input

/**
 * Three-state model for on-screen modifier toggle buttons.
 *
 * State transitions:
 * - INACTIVE  --single tap-->  ONE_SHOT
 * - INACTIVE  --double tap-->  LOCKED
 * - ONE_SHOT  --key press-->   INACTIVE  (auto-deactivate after use)
 * - ONE_SHOT  --single tap-->  INACTIVE  (cancel)
 * - LOCKED    --single tap-->  INACTIVE  (unlock)
 */
enum class ModifierButtonState {
    /** Button is off; modifier not applied. */
    INACTIVE,
    /** Button active for next key only; auto-deactivates after use. */
    ONE_SHOT,
    /** Button active until user taps again to deactivate. */
    LOCKED;

    /** True when the modifier should be applied (ONE_SHOT or LOCKED). */
    val isActive: Boolean get() = this != INACTIVE

    /** Next state after a single tap. */
    fun nextOnTap(): ModifierButtonState = when (this) {
        INACTIVE -> ONE_SHOT
        ONE_SHOT -> INACTIVE
        LOCKED   -> INACTIVE
    }

    /**
     * Next state after a double-tap gesture.
     *
     * Both INACTIVE and ONE_SHOT map to LOCKED because some platforms fire
     * onTap before onDoubleTap, advancing the state to ONE_SHOT before this
     * function is called.
     */
    fun nextOnDoubleTap(): ModifierButtonState = when (this) {
        INACTIVE,
        ONE_SHOT -> LOCKED
        LOCKED   -> INACTIVE
    }
}

/**
 * The three on-screen modifier keys supported by the toggle button row.
 */
enum class ModifierKey { SHIFT, CTRL, ALT }
