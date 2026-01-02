package tokyo.isseikuzumaki.vibeterminal.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class KeyboardInputProcessorTest {

    // Helper function to create key event
    private fun keyDown(
        keyCode: Int,
        ctrl: Boolean = false,
        alt: Boolean = false,
        shift: Boolean = false,
        meta: Boolean = false
    ) = KeyEventData(
        keyCode = keyCode,
        action = KeyAction.DOWN,
        isCtrlPressed = ctrl,
        isAltPressed = alt,
        isShiftPressed = shift,
        isMetaPressed = meta
    )

    private fun keyUp(keyCode: Int) = KeyEventData(
        keyCode = keyCode,
        action = KeyAction.UP
    )

    // ===========================================
    // Tests for processKeyEvent
    // ===========================================

    @Test
    fun testKeyUpIsIgnored() {
        val result = KeyboardInputProcessor.processKeyEvent(keyUp(KeyCodes.A))
        assertIs<KeyboardInputProcessor.KeyResult.Ignored>(result)
    }

    @Test
    fun testModifierKeysAreIgnored() {
        val modifierKeys = listOf(
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
            KeyCodes.FUNCTION
        )

        modifierKeys.forEach { keyCode ->
            val result = KeyboardInputProcessor.processKeyEvent(keyDown(keyCode))
            assertIs<KeyboardInputProcessor.KeyResult.Ignored>(result, "Modifier key $keyCode should be ignored")
        }
    }

    @Test
    fun testRegularLetterKeysPassThrough() {
        // Without modifiers, regular letters should pass through
        val letters = listOf(KeyCodes.A, KeyCodes.B, KeyCodes.Z)
        letters.forEach { keyCode ->
            val result = KeyboardInputProcessor.processKeyEvent(keyDown(keyCode))
            assertIs<KeyboardInputProcessor.KeyResult.PassThrough>(result)
        }
    }

    // ===========================================
    // Tests for Arrow Keys
    // ===========================================

    @Test
    fun testArrowKeyUp() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DPAD_UP))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[A", result.sequence)
    }

    @Test
    fun testArrowKeyDown() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DPAD_DOWN))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[B", result.sequence)
    }

    @Test
    fun testArrowKeyRight() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DPAD_RIGHT))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[C", result.sequence)
    }

    @Test
    fun testArrowKeyLeft() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DPAD_LEFT))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[D", result.sequence)
    }

    // ===========================================
    // Tests for Navigation Keys
    // ===========================================

    @Test
    fun testHomeKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.MOVE_HOME))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[H", result.sequence)
    }

    @Test
    fun testEndKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.MOVE_END))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[F", result.sequence)
    }

    @Test
    fun testPageUp() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.PAGE_UP))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[5~", result.sequence)
    }

    @Test
    fun testPageDown() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.PAGE_DOWN))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[6~", result.sequence)
    }

    @Test
    fun testInsertKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.INSERT))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[2~", result.sequence)
    }

    @Test
    fun testDeleteKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.FORWARD_DEL))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[3~", result.sequence)
    }

    // ===========================================
    // Tests for Special Keys
    // ===========================================

    @Test
    fun testEnterKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.ENTER))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\r", result.sequence)
    }

    @Test
    fun testNumpadEnterKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.NUMPAD_ENTER))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\r", result.sequence)
    }

    @Test
    fun testTabKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.TAB))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\t", result.sequence)
    }

    @Test
    fun testShiftTabKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.TAB, shift = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[Z", result.sequence)
    }

    @Test
    fun testEscapeKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.ESCAPE))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B", result.sequence)
    }

    @Test
    fun testBackspaceKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DEL))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u007F", result.sequence)
    }

    // ===========================================
    // Tests for Function Keys
    // ===========================================

    @Test
    fun testFunctionKeys() {
        val expectedSequences = mapOf(
            KeyCodes.F1 to "\u001BOP",
            KeyCodes.F2 to "\u001BOQ",
            KeyCodes.F3 to "\u001BOR",
            KeyCodes.F4 to "\u001BOS",
            KeyCodes.F5 to "\u001B[15~",
            KeyCodes.F6 to "\u001B[17~",
            KeyCodes.F7 to "\u001B[18~",
            KeyCodes.F8 to "\u001B[19~",
            KeyCodes.F9 to "\u001B[20~",
            KeyCodes.F10 to "\u001B[21~",
            KeyCodes.F11 to "\u001B[23~",
            KeyCodes.F12 to "\u001B[24~"
        )

        expectedSequences.forEach { (keyCode, expected) ->
            val result = KeyboardInputProcessor.processKeyEvent(keyDown(keyCode))
            assertIs<KeyboardInputProcessor.KeyResult.Handled>(result, "F key $keyCode should be handled")
            assertEquals(expected, result.sequence, "F key $keyCode sequence mismatch")
        }
    }

    // ===========================================
    // Tests for Ctrl+Key Combinations
    // ===========================================

    @Test
    fun testCtrlC() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.C, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u0003", result.sequence) // ASCII 3 = Ctrl+C (SIGINT)
    }

    @Test
    fun testCtrlD() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.D, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u0004", result.sequence) // ASCII 4 = Ctrl+D (EOF)
    }

    @Test
    fun testCtrlZ() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.Z, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001A", result.sequence) // ASCII 26 = Ctrl+Z (SIGTSTP)
    }

    @Test
    fun testCtrlL() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.L, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u000C", result.sequence) // ASCII 12 = Ctrl+L (clear screen)
    }

    @Test
    fun testCtrlLeftBracket() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.LEFT_BRACKET, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B", result.sequence) // ASCII 27 = ESC
    }

    @Test
    fun testCtrlSpace() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.SPACE, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u0000", result.sequence) // ASCII 0 = NUL
    }

    @Test
    fun testAllCtrlLetters() {
        val letters = listOf(
            KeyCodes.A to 1, KeyCodes.B to 2, KeyCodes.C to 3, KeyCodes.D to 4,
            KeyCodes.E to 5, KeyCodes.F to 6, KeyCodes.G to 7, KeyCodes.H to 8,
            KeyCodes.I to 9, KeyCodes.J to 10, KeyCodes.K to 11, KeyCodes.L to 12,
            KeyCodes.M to 13, KeyCodes.N to 14, KeyCodes.O to 15, KeyCodes.P to 16,
            KeyCodes.Q to 17, KeyCodes.R to 18, KeyCodes.S to 19, KeyCodes.T to 20,
            KeyCodes.U to 21, KeyCodes.V to 22, KeyCodes.W to 23, KeyCodes.X to 24,
            KeyCodes.Y to 25, KeyCodes.Z to 26
        )

        letters.forEach { (keyCode, expectedAscii) ->
            val result = KeyboardInputProcessor.processKeyEvent(keyDown(keyCode, ctrl = true))
            assertIs<KeyboardInputProcessor.KeyResult.Handled>(result, "Ctrl+letter should be handled")
            assertEquals(expectedAscii.toChar().toString(), result.sequence, "Ctrl+$keyCode should produce ASCII $expectedAscii")
        }
    }

    // ===========================================
    // Tests for Alt+Key Combinations
    // ===========================================

    @Test
    fun testAltA() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.A, alt = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001Ba", result.sequence) // ESC + 'a'
    }

    @Test
    fun testAltShiftA() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.A, alt = true, shift = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001BA", result.sequence) // ESC + 'A'
    }

    @Test
    fun testAltNumber() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.NUM_1, alt = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B1", result.sequence) // ESC + '1'
    }

    // ===========================================
    // Tests for isModifierKey
    // ===========================================

    @Test
    fun testIsModifierKeyTrue() {
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.SHIFT_LEFT))
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.CTRL_LEFT))
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.ALT_LEFT))
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.META_LEFT))
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.CAPS_LOCK))
    }

    @Test
    fun testIsModifierKeyFalse() {
        assertFalse(KeyboardInputProcessor.isModifierKey(KeyCodes.A))
        assertFalse(KeyboardInputProcessor.isModifierKey(KeyCodes.ENTER))
        assertFalse(KeyboardInputProcessor.isModifierKey(KeyCodes.DPAD_UP))
        assertFalse(KeyboardInputProcessor.isModifierKey(KeyCodes.F1))
    }

    // ===========================================
    // Tests for getSpecialKeySequence
    // ===========================================

    @Test
    fun testGetSpecialKeySequenceReturnsNullForRegularKeys() {
        assertNull(KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.A, hasShift = false))
        assertNull(KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.NUM_1, hasShift = false))
    }

    @Test
    fun testGetSpecialKeySequenceArrows() {
        assertEquals("\u001B[A", KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.DPAD_UP, hasShift = false))
        assertEquals("\u001B[B", KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.DPAD_DOWN, hasShift = false))
        assertEquals("\u001B[C", KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.DPAD_RIGHT, hasShift = false))
        assertEquals("\u001B[D", KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.DPAD_LEFT, hasShift = false))
    }

    // ===========================================
    // Tests for getCtrlKeySequence
    // ===========================================

    @Test
    fun testGetCtrlKeySequenceReturnsNullForUnmappedKeys() {
        assertNull(KeyboardInputProcessor.getCtrlKeySequence(KeyCodes.DPAD_UP))
        assertNull(KeyboardInputProcessor.getCtrlKeySequence(KeyCodes.F1))
    }

    @Test
    fun testGetCtrlKeySequenceSpecialCases() {
        assertEquals("\u001B", KeyboardInputProcessor.getCtrlKeySequence(KeyCodes.LEFT_BRACKET)) // Ctrl+[ = ESC
        assertEquals("\u0000", KeyboardInputProcessor.getCtrlKeySequence(KeyCodes.SPACE)) // Ctrl+Space = NUL
    }

    // ===========================================
    // Tests for getCharFromKeyCode
    // ===========================================

    @Test
    fun testGetCharFromKeyCodeLetters() {
        assertEquals('a', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.A, hasShift = false))
        assertEquals('A', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.A, hasShift = true))
        assertEquals('z', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.Z, hasShift = false))
        assertEquals('Z', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.Z, hasShift = true))
    }

    @Test
    fun testGetCharFromKeyCodeNumbers() {
        assertEquals('1', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_1, hasShift = false))
        assertEquals('!', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_1, hasShift = true))
        assertEquals('0', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_0, hasShift = false))
    }

    @Test
    fun testGetCharFromKeyCodeSymbols() {
        assertEquals('-', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.MINUS, hasShift = false))
        assertEquals('=', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.MINUS, hasShift = true))
        assertEquals('[', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.LEFT_BRACKET, hasShift = false))
        assertEquals('{', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.LEFT_BRACKET, hasShift = true))
    }

    @Test
    fun testGetCharFromKeyCodeReturnsNullForSpecialKeys() {
        assertNull(KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.DPAD_UP, hasShift = false))
        assertNull(KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.ENTER, hasShift = false))
        assertNull(KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.F1, hasShift = false))
    }

    // ===========================================
    // Tests for JIS Layout Specifics
    // ===========================================

    @Test
    fun testJisLayoutShiftNumbers() {
        // JIS layout specific: Shift+2 = ", Shift+6 = &, etc.
        assertEquals('"', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_2, hasShift = true))
        assertEquals('&', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_6, hasShift = true))
        assertEquals('\'', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_7, hasShift = true))
        assertEquals('(', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_8, hasShift = true))
        assertEquals(')', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_9, hasShift = true))
    }

    @Test
    fun testJisSpecificKeys() {
        // YEN key
        assertEquals('\\', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.YEN, hasShift = false))
        assertEquals('|', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.YEN, hasShift = true))
        // RO key
        assertEquals('\\', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.RO, hasShift = false))
        assertEquals('_', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.RO, hasShift = true))
    }
}
