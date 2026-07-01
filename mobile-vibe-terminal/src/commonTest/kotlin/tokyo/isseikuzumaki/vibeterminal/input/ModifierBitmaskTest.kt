package tokyo.isseikuzumaki.vibeterminal.input

import kotlin.test.Test
import kotlin.test.assertEquals

class ModifierBitmaskTest {

    private fun noModEvent() = KeyEventData(
        keyCode = KeyCodes.A,
        action = KeyAction.DOWN
    )

    private fun eventWith(shift: Boolean = false, alt: Boolean = false, ctrl: Boolean = false, meta: Boolean = false) =
        KeyEventData(
            keyCode = KeyCodes.A,
            action = KeyAction.DOWN,
            isShiftPressed = shift,
            isAltPressed = alt,
            isCtrlPressed = ctrl,
            isMetaPressed = meta
        )

    // =========================================================================
    // combine() — single source
    // =========================================================================

    @Test
    fun `combine - no modifiers returns 0`() {
        val result = ModifierBitmask.combine(
            noModEvent(),
            ModifierButtonState.INACTIVE,
            ModifierButtonState.INACTIVE,
            ModifierButtonState.INACTIVE
        )
        assertEquals(0, result)
    }

    @Test
    fun `combine - UI Ctrl ONE_SHOT returns 4`() {
        val result = ModifierBitmask.combine(
            noModEvent(),
            shiftState = ModifierButtonState.INACTIVE,
            ctrlState = ModifierButtonState.ONE_SHOT,
            altState = ModifierButtonState.INACTIVE
        )
        assertEquals(4, result)
    }

    @Test
    fun `combine - UI Shift LOCKED returns 1`() {
        val result = ModifierBitmask.combine(
            noModEvent(),
            shiftState = ModifierButtonState.LOCKED,
            ctrlState = ModifierButtonState.INACTIVE,
            altState = ModifierButtonState.INACTIVE
        )
        assertEquals(1, result)
    }

    @Test
    fun `combine - hardware Shift returns 1`() {
        val result = ModifierBitmask.combine(
            eventWith(shift = true),
            ModifierButtonState.INACTIVE,
            ModifierButtonState.INACTIVE,
            ModifierButtonState.INACTIVE
        )
        assertEquals(1, result)
    }

    @Test
    fun `combine - Meta bit from hardware returns 8`() {
        val result = ModifierBitmask.combine(
            eventWith(meta = true),
            ModifierButtonState.INACTIVE,
            ModifierButtonState.INACTIVE,
            ModifierButtonState.INACTIVE
        )
        assertEquals(8, result)
    }

    // =========================================================================
    // combine() — dual source
    // =========================================================================

    @Test
    fun `combine - hardware Shift plus UI Ctrl returns 5`() {
        val result = ModifierBitmask.combine(
            eventWith(shift = true),
            shiftState = ModifierButtonState.INACTIVE,
            ctrlState = ModifierButtonState.ONE_SHOT,
            altState = ModifierButtonState.INACTIVE
        )
        assertEquals(5, result)
    }

    @Test
    fun `combine - hardware Ctrl plus UI Ctrl ONE_SHOT is idempotent returns 4`() {
        val result = ModifierBitmask.combine(
            eventWith(ctrl = true),
            shiftState = ModifierButtonState.INACTIVE,
            ctrlState = ModifierButtonState.ONE_SHOT,
            altState = ModifierButtonState.INACTIVE
        )
        assertEquals(4, result)
    }

    @Test
    fun `combine - hardware Shift plus UI Shift is idempotent returns 1`() {
        val result = ModifierBitmask.combine(
            eventWith(shift = true),
            shiftState = ModifierButtonState.ONE_SHOT,
            ctrlState = ModifierButtonState.INACTIVE,
            altState = ModifierButtonState.INACTIVE
        )
        assertEquals(1, result)
    }

    @Test
    fun `combine - hardware Shift plus UI Ctrl plus UI Alt returns 7`() {
        val result = ModifierBitmask.combine(
            eventWith(shift = true),
            shiftState = ModifierButtonState.INACTIVE,
            ctrlState = ModifierButtonState.LOCKED,
            altState = ModifierButtonState.ONE_SHOT
        )
        assertEquals(7, result)
    }

    // =========================================================================
    // toModifierNumber()
    // =========================================================================

    @Test
    fun `toModifierNumber - bitmask 0 returns 0 (sentinel)`() {
        assertEquals(0, ModifierBitmask.toModifierNumber(0))
    }

    @Test
    fun `toModifierNumber - Shift bitmask 1 returns 2`() {
        assertEquals(2, ModifierBitmask.toModifierNumber(1))
    }

    @Test
    fun `toModifierNumber - Alt bitmask 2 returns 3`() {
        assertEquals(3, ModifierBitmask.toModifierNumber(2))
    }

    @Test
    fun `toModifierNumber - Ctrl bitmask 4 returns 5`() {
        assertEquals(5, ModifierBitmask.toModifierNumber(4))
    }

    @Test
    fun `toModifierNumber - Shift+Ctrl bitmask 5 returns 6`() {
        assertEquals(6, ModifierBitmask.toModifierNumber(5))
    }

    @Test
    fun `toModifierNumber - Meta bitmask 8 returns 9`() {
        assertEquals(9, ModifierBitmask.toModifierNumber(8))
    }

    @Test
    fun `toModifierNumber - all modifiers bitmask 15 returns 16`() {
        assertEquals(16, ModifierBitmask.toModifierNumber(15))
    }
}
