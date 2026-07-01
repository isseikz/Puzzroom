package tokyo.isseikuzumaki.vibeterminal.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModifierButtonStateTest {

    // ── isActive ────────────────────────────────────────────────────────────

    @Test
    fun `INACTIVE isActive is false`() {
        assertFalse(ModifierButtonState.INACTIVE.isActive)
    }

    @Test
    fun `ONE_SHOT isActive is true`() {
        assertTrue(ModifierButtonState.ONE_SHOT.isActive)
    }

    @Test
    fun `LOCKED isActive is true`() {
        assertTrue(ModifierButtonState.LOCKED.isActive)
    }

    // ── nextOnTap ────────────────────────────────────────────────────────────

    @Test
    fun `tap INACTIVE activates ONE_SHOT`() {
        assertEquals(ModifierButtonState.ONE_SHOT, ModifierButtonState.INACTIVE.nextOnTap())
    }

    @Test
    fun `tap ONE_SHOT cancels back to INACTIVE`() {
        assertEquals(ModifierButtonState.INACTIVE, ModifierButtonState.ONE_SHOT.nextOnTap())
    }

    @Test
    fun `tap LOCKED unlocks to INACTIVE`() {
        assertEquals(ModifierButtonState.INACTIVE, ModifierButtonState.LOCKED.nextOnTap())
    }

    // ── nextOnDoubleTap ───────────────────────────────────────────────────────

    @Test
    fun `double-tap INACTIVE locks`() {
        assertEquals(ModifierButtonState.LOCKED, ModifierButtonState.INACTIVE.nextOnDoubleTap())
    }

    @Test
    fun `double-tap ONE_SHOT locks even if onTap fired first`() {
        // Some platforms fire onTap before onDoubleTap, advancing state to ONE_SHOT.
        // The result should still be LOCKED, not INACTIVE.
        assertEquals(ModifierButtonState.LOCKED, ModifierButtonState.ONE_SHOT.nextOnDoubleTap())
    }

    @Test
    fun `double-tap LOCKED unlocks to INACTIVE`() {
        assertEquals(ModifierButtonState.INACTIVE, ModifierButtonState.LOCKED.nextOnDoubleTap())
    }

    // ── full interaction sequences ────────────────────────────────────────────

    @Test
    fun `one-shot sequence - tap then auto-consume returns to INACTIVE`() {
        val afterTap = ModifierButtonState.INACTIVE.nextOnTap()
        assertEquals(ModifierButtonState.ONE_SHOT, afterTap)
        // simulating applyAndConsumeOneShotModifiers: ONE_SHOT → INACTIVE
        val afterUse = if (afterTap == ModifierButtonState.ONE_SHOT) ModifierButtonState.INACTIVE else afterTap
        assertEquals(ModifierButtonState.INACTIVE, afterUse)
    }

    @Test
    fun `lock sequence - double-tap then single-tap to unlock`() {
        val locked = ModifierButtonState.INACTIVE.nextOnDoubleTap()
        assertEquals(ModifierButtonState.LOCKED, locked)
        val unlocked = locked.nextOnTap()
        assertEquals(ModifierButtonState.INACTIVE, unlocked)
    }

    @Test
    fun `lock is not consumed by applyAndConsumeOneShotModifiers`() {
        // LOCKED should survive key presses (only ONE_SHOT is consumed)
        val state = ModifierButtonState.LOCKED
        val afterConsume = if (state == ModifierButtonState.ONE_SHOT) ModifierButtonState.INACTIVE else state
        assertEquals(ModifierButtonState.LOCKED, afterConsume)
    }

    @Test
    fun `platform fires onTap then onDoubleTap - ends in LOCKED not INACTIVE`() {
        // Simulate the problematic platform behavior:
        // 1. onTap fires first → INACTIVE → ONE_SHOT
        val afterOnTap = ModifierButtonState.INACTIVE.nextOnTap()
        assertEquals(ModifierButtonState.ONE_SHOT, afterOnTap)
        // 2. onDoubleTap fires → ONE_SHOT must → LOCKED (not INACTIVE)
        val afterOnDoubleTap = afterOnTap.nextOnDoubleTap()
        assertEquals(ModifierButtonState.LOCKED, afterOnDoubleTap)
    }
}
