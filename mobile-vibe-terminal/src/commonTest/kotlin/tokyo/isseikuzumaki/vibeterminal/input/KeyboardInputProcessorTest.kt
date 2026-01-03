package tokyo.isseikuzumaki.vibeterminal.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Unit tests for [KeyboardInputProcessor].
 *
 * This test suite verifies the conversion of keyboard input to terminal escape sequences,
 * following the ANSI X3.64 / ECMA-48 standards and xterm extensions.
 *
 * ## Background: Terminal Escape Sequences
 *
 * Terminal emulators communicate special keys (arrows, function keys, etc.) using
 * "escape sequences" - strings that begin with the ESC character (0x1B). These sequences
 * were standardized in the 1970s-80s for hardware terminals like VT100/VT220.
 *
 * Common sequence formats:
 * - **CSI sequences**: `ESC [` followed by parameters (e.g., `ESC[A` for Up arrow)
 * - **SS3 sequences**: `ESC O` followed by a character (e.g., `ESC OP` for F1)
 *
 * ## Control Characters
 *
 * Ctrl+letter combinations produce ASCII control characters (0x00-0x1F).
 * This mapping dates back to the original ASCII standard (1963) where:
 * - Ctrl+A = 0x01, Ctrl+B = 0x02, ..., Ctrl+Z = 0x1A
 * - The control character is computed as: (letter ASCII code) - 0x40
 *
 * ## JIS Keyboard Layout
 *
 * This implementation is optimized for Apple Magic Keyboard with JIS (Japanese Industrial Standard)
 * layout, which differs from US layout in symbol key positions. Key differences include:
 * - Shift+2 produces `"` (US: `@`)
 * - Shift+6 produces `&` (US: `^`)
 * - Dedicated YEN (¥) and RO (ろ) keys
 *
 * @see KeyboardInputProcessor
 * @see <a href="https://invisible-island.net/xterm/ctlseqs/ctlseqs.html">XTerm Control Sequences</a>
 * @see <a href="https://ecma-international.org/publications-and-standards/standards/ecma-48/">ECMA-48 Standard</a>
 */
class KeyboardInputProcessorTest {

    /**
     * Helper function to create a key-down event for testing.
     *
     * @param keyCode The key code from [KeyCodes]
     * @param ctrl Whether Ctrl modifier is pressed
     * @param alt Whether Alt modifier is pressed
     * @param shift Whether Shift modifier is pressed
     * @param meta Whether Meta (Command on Mac) modifier is pressed
     */
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

    /**
     * Helper function to create a key-up event for testing.
     */
    private fun keyUp(keyCode: Int) = KeyEventData(
        keyCode = keyCode,
        action = KeyAction.UP
    )

    // =========================================================================
    // Key Event Filtering Tests
    // =========================================================================

    /**
     * Verifies that KEY_UP events are ignored.
     *
     * ## Rationale
     * Terminal input is character-based, not event-based. We only process KEY_DOWN
     * events to send characters/sequences to the terminal. Processing KEY_UP would
     * result in duplicate input.
     *
     * This follows the standard behavior of terminal emulators where key release
     * events are not transmitted to the remote host.
     */
    @Test
    fun testKeyUpIsIgnored() {
        val result = KeyboardInputProcessor.processKeyEvent(keyUp(KeyCodes.A))
        assertIs<KeyboardInputProcessor.KeyResult.Ignored>(result)
    }

    /**
     * Verifies that pure modifier key presses are ignored.
     *
     * ## Rationale
     * Modifier keys (Shift, Ctrl, Alt, Meta) by themselves don't produce terminal input.
     * They only modify the behavior of other keys when pressed in combination.
     *
     * If we didn't ignore these, pressing Shift alone would send something to the
     * terminal, which is incorrect behavior.
     *
     * ## Covered Keys
     * - Shift (left/right)
     * - Ctrl (left/right)
     * - Alt (left/right)
     * - Meta/Command (left/right)
     * - Caps Lock, Num Lock, Scroll Lock
     * - Function key (Fn)
     */
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

    /**
     * Verifies that regular letter keys without modifiers return PassThrough.
     *
     * ## Rationale
     * Regular character input (letters, numbers without Ctrl/Alt) should be handled
     * by the system's InputConnection/IME, not by this processor. This is critical
     * for avoiding double input when using hardware keyboards with Android's
     * text input system.
     *
     * The PassThrough result tells the caller to let the system handle this key
     * through the normal text input pipeline.
     */
    @Test
    fun testRegularLetterKeysPassThrough() {
        val letters = listOf(KeyCodes.A, KeyCodes.B, KeyCodes.Z)
        letters.forEach { keyCode ->
            val result = KeyboardInputProcessor.processKeyEvent(keyDown(keyCode))
            assertIs<KeyboardInputProcessor.KeyResult.PassThrough>(result)
        }
    }

    // =========================================================================
    // Arrow Key Tests - ANSI Cursor Key Sequences
    // =========================================================================

    /**
     * Verifies Up arrow produces `ESC[A`.
     *
     * ## Terminal Standard
     * Arrow keys use ANSI cursor movement sequences (CSI sequences):
     * - Up:    `ESC[A` (CSI A)
     * - Down:  `ESC[B` (CSI B)
     * - Right: `ESC[C` (CSI C)
     * - Left:  `ESC[D` (CSI D)
     *
     * These sequences are defined in ECMA-48 and universally supported by
     * terminal emulators. Applications like vim, less, and shells use these
     * to implement cursor movement and command history navigation.
     */
    @Test
    fun testArrowKeyUp() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DPAD_UP))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[A", result.sequence)
    }

    /**
     * Verifies Down arrow produces `ESC[B`.
     * @see testArrowKeyUp for sequence format explanation
     */
    @Test
    fun testArrowKeyDown() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DPAD_DOWN))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[B", result.sequence)
    }

    /**
     * Verifies Right arrow produces `ESC[C`.
     * @see testArrowKeyUp for sequence format explanation
     */
    @Test
    fun testArrowKeyRight() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DPAD_RIGHT))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[C", result.sequence)
    }

    /**
     * Verifies Left arrow produces `ESC[D`.
     * @see testArrowKeyUp for sequence format explanation
     */
    @Test
    fun testArrowKeyLeft() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DPAD_LEFT))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[D", result.sequence)
    }

    // =========================================================================
    // Navigation Key Tests - PC-Style Keys
    // =========================================================================

    /**
     * Verifies Home key produces `ESC[H`.
     *
     * ## Terminal Standard
     * Navigation keys use extended CSI sequences. The Home and End keys have
     * shorter sequences (`ESC[H` and `ESC[F`) in xterm's default mode.
     *
     * Note: Some terminals use `ESC[1~` for Home, but `ESC[H` is more common
     * in modern terminal emulators following xterm conventions.
     */
    @Test
    fun testHomeKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.MOVE_HOME))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[H", result.sequence)
    }

    /**
     * Verifies End key produces `ESC[F`.
     * @see testHomeKey for sequence format explanation
     */
    @Test
    fun testEndKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.MOVE_END))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[F", result.sequence)
    }

    /**
     * Verifies Page Up produces `ESC[5~`.
     *
     * ## Terminal Standard
     * Page Up/Down, Insert, and Delete use the "tilde" format: `ESC[n~`
     * where n is a number identifying the key:
     * - Insert:    `ESC[2~`
     * - Delete:    `ESC[3~`
     * - Page Up:   `ESC[5~`
     * - Page Down: `ESC[6~`
     *
     * These originated from the VT220 terminal.
     */
    @Test
    fun testPageUp() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.PAGE_UP))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[5~", result.sequence)
    }

    /**
     * Verifies Page Down produces `ESC[6~`.
     * @see testPageUp for sequence format explanation
     */
    @Test
    fun testPageDown() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.PAGE_DOWN))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[6~", result.sequence)
    }

    /**
     * Verifies Insert key produces `ESC[2~`.
     * @see testPageUp for sequence format explanation
     */
    @Test
    fun testInsertKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.INSERT))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[2~", result.sequence)
    }

    /**
     * Verifies Delete key produces `ESC[3~`.
     *
     * ## Important Distinction
     * The Delete key (forward delete) is different from Backspace:
     * - Delete (FORWARD_DEL): `ESC[3~` - deletes character under/after cursor
     * - Backspace (DEL): `0x7F` - deletes character before cursor
     *
     * @see testBackspaceKey
     */
    @Test
    fun testDeleteKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.FORWARD_DEL))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[3~", result.sequence)
    }

    // =========================================================================
    // Special Key Tests
    // =========================================================================

    /**
     * Verifies Enter key produces CR (0x0D).
     *
     * ## Terminal Standard
     * Enter/Return sends Carriage Return (CR, 0x0D, '\r').
     * The terminal or shell typically converts this to the appropriate
     * line ending (CR, LF, or CRLF) based on terminal settings.
     *
     * Note: Some systems expect LF (0x0A), but CR is the standard for
     * keyboard Enter key input.
     */
    @Test
    fun testEnterKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.ENTER))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\r", result.sequence)
    }

    /**
     * Verifies Numpad Enter behaves identically to main Enter key.
     *
     * ## Rationale
     * Both Enter keys should produce the same result for consistency.
     * Unlike some applications that distinguish between them, terminal
     * input treats them equivalently.
     */
    @Test
    fun testNumpadEnterKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.NUMPAD_ENTER))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\r", result.sequence)
    }

    /**
     * Verifies Tab produces HT (0x09).
     *
     * ## Terminal Standard
     * Tab sends the Horizontal Tab character (HT, 0x09, '\t').
     * This is one of the original ASCII control characters.
     */
    @Test
    fun testTabKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.TAB))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\t", result.sequence)
    }

    /**
     * Verifies Shift+Tab produces the back-tab sequence `ESC[Z`.
     *
     * ## Terminal Standard
     * Shift+Tab (back-tab or reverse-tab) sends `ESC[Z` (CSI Z),
     * also known as CBT (Cursor Backward Tabulation).
     *
     * This is used in applications to navigate backwards through
     * form fields or UI elements.
     */
    @Test
    fun testShiftTabKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.TAB, shift = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B[Z", result.sequence)
    }

    /**
     * Verifies Escape key produces ESC (0x1B).
     *
     * ## Terminal Standard
     * The Escape key sends the ESC character (0x1B), which is also
     * the start of all escape sequences. Applications like vim use
     * this to switch modes.
     */
    @Test
    fun testEscapeKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.ESCAPE))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B", result.sequence)
    }

    /**
     * Verifies Backspace produces DEL (0x7F).
     *
     * ## Terminal Standard
     * Modern terminals send DEL (0x7F) for Backspace. This is the
     * xterm default and most compatible choice.
     *
     * ## Historical Note
     * Older systems sometimes sent BS (0x08) for Backspace. The choice
     * of DEL vs BS has been a source of configuration issues. DEL is
     * now the de facto standard for terminal emulators.
     *
     * @see testDeleteKey for forward delete
     */
    @Test
    fun testBackspaceKey() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.DEL))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u007F", result.sequence)
    }

    // =========================================================================
    // Function Key Tests - VT220/xterm Sequences
    // =========================================================================

    /**
     * Verifies all function keys (F1-F12) produce correct escape sequences.
     *
     * ## Terminal Standard
     * Function keys use two different sequence formats:
     *
     * ### F1-F4: SS3 Sequences (VT100 style)
     * - F1: `ESC O P`
     * - F2: `ESC O Q`
     * - F3: `ESC O R`
     * - F4: `ESC O S`
     *
     * These use SS3 (Single Shift 3, ESC O) prefix, originating from VT100.
     *
     * ### F5-F12: CSI Tilde Sequences (VT220 style)
     * - F5:  `ESC[15~`
     * - F6:  `ESC[17~`
     * - F7:  `ESC[18~`
     * - F8:  `ESC[19~`
     * - F9:  `ESC[20~`
     * - F10: `ESC[21~`
     * - F11: `ESC[23~`
     * - F12: `ESC[24~`
     *
     * Note the gaps in numbering (no 16, 22) - this is historical from VT220
     * where those codes were used for other keys.
     */
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

    // =========================================================================
    // Ctrl+Key Combination Tests - ASCII Control Characters
    // =========================================================================

    /**
     * Verifies Ctrl+C produces ASCII 3 (ETX - End of Text).
     *
     * ## Unix Signal
     * Ctrl+C sends SIGINT to the foreground process, typically used to
     * interrupt/cancel a running command. This is one of the most important
     * terminal control sequences.
     */
    @Test
    fun testCtrlC() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.C, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u0003", result.sequence)
    }

    /**
     * Verifies Ctrl+D produces ASCII 4 (EOT - End of Transmission).
     *
     * ## Unix Behavior
     * Ctrl+D signals EOF (End of File) to the terminal. When entered on an
     * empty line in a shell, it typically logs out or closes the shell.
     * In programs reading stdin, it signals end of input.
     */
    @Test
    fun testCtrlD() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.D, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u0004", result.sequence)
    }

    /**
     * Verifies Ctrl+Z produces ASCII 26 (SUB - Substitute).
     *
     * ## Unix Signal
     * Ctrl+Z sends SIGTSTP (Terminal Stop), which suspends the foreground
     * process. The process can be resumed with `fg` (foreground) or `bg`
     * (background) commands.
     */
    @Test
    fun testCtrlZ() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.Z, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001A", result.sequence)
    }

    /**
     * Verifies Ctrl+L produces ASCII 12 (FF - Form Feed).
     *
     * ## Terminal Behavior
     * Ctrl+L is conventionally used to clear/refresh the terminal screen.
     * Most shells and terminal applications respond to this by redrawing
     * the display.
     */
    @Test
    fun testCtrlL() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.L, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u000C", result.sequence)
    }

    /**
     * Verifies Ctrl+[ produces ASCII 27 (ESC).
     *
     * ## Vim Users
     * This is crucial for vim users - Ctrl+[ is equivalent to pressing
     * Escape. This works because '[' is ASCII 0x5B, and 0x5B - 0x40 = 0x1B (ESC).
     *
     * This allows Escape functionality without moving hands from home row.
     */
    @Test
    fun testCtrlLeftBracket() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.LEFT_BRACKET, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B", result.sequence)
    }

    /**
     * Verifies Ctrl+Space produces ASCII 0 (NUL).
     *
     * ## Emacs Users
     * Ctrl+Space is commonly used in Emacs to set a mark for text selection.
     * The NUL character (0x00) is sent to the application.
     */
    @Test
    fun testCtrlSpace() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.SPACE, ctrl = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u0000", result.sequence)
    }

    /**
     * Verifies all Ctrl+letter combinations produce correct ASCII control characters.
     *
     * ## ASCII Control Character Mapping
     * The mapping follows the original ASCII standard:
     * - Ctrl+A = 0x01 (SOH - Start of Heading)
     * - Ctrl+B = 0x02 (STX - Start of Text)
     * - ...
     * - Ctrl+Z = 0x1A (SUB - Substitute)
     *
     * The formula is: Ctrl+X = ASCII(X) - 0x40
     * For example: Ctrl+A = 0x41 - 0x40 = 0x01
     *
     * ## Common Uses
     * - Ctrl+A (1): Select all, tmux prefix, readline beginning of line
     * - Ctrl+C (3): SIGINT - interrupt process
     * - Ctrl+D (4): EOF - end of file/input
     * - Ctrl+E (5): Readline end of line
     * - Ctrl+K (11): Readline kill to end of line
     * - Ctrl+U (21): Readline kill to beginning of line
     * - Ctrl+W (23): Readline kill previous word
     * - Ctrl+Z (26): SIGTSTP - suspend process
     */
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

    // =========================================================================
    // Alt+Key Combination Tests - Meta/Escape Prefix
    // =========================================================================

    /**
     * Verifies Alt+A produces `ESC a` (0x1B 0x61).
     *
     * ## Terminal Standard
     * Alt (Meta) key combinations are sent as ESC followed by the character.
     * This is known as "Meta sends Escape" mode, which is the most compatible
     * approach for terminal applications.
     *
     * ## Alternative Encoding
     * Some systems use "8-bit Meta" where Alt+A would send 0xE1 (0x61 | 0x80),
     * but this is less common and can conflict with UTF-8 encoding.
     */
    @Test
    fun testAltA() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.A, alt = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001Ba", result.sequence)
    }

    /**
     * Verifies Alt+Shift+A produces `ESC A` (uppercase).
     *
     * ## Shift Interaction
     * When Shift is also pressed, the character after ESC is uppercase.
     * This allows applications to distinguish Alt+a from Alt+A.
     */
    @Test
    fun testAltShiftA() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.A, alt = true, shift = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001BA", result.sequence)
    }

    /**
     * Verifies Alt+number produces `ESC` followed by the digit.
     *
     * ## Use Cases
     * Alt+number combinations are used in various applications:
     * - Window managers: Switch desktops (Alt+1, Alt+2, etc.)
     * - Terminals: Switch tabs
     * - Editors: Quick navigation
     */
    @Test
    fun testAltNumber() {
        val result = KeyboardInputProcessor.processKeyEvent(keyDown(KeyCodes.NUM_1, alt = true))
        assertIs<KeyboardInputProcessor.KeyResult.Handled>(result)
        assertEquals("\u001B1", result.sequence)
    }

    // =========================================================================
    // isModifierKey Tests
    // =========================================================================

    /**
     * Verifies correct identification of modifier keys.
     *
     * ## Purpose
     * This function is used to filter out pure modifier key presses that
     * should not produce terminal input.
     */
    @Test
    fun testIsModifierKeyTrue() {
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.SHIFT_LEFT))
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.CTRL_LEFT))
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.ALT_LEFT))
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.META_LEFT))
        assertTrue(KeyboardInputProcessor.isModifierKey(KeyCodes.CAPS_LOCK))
    }

    /**
     * Verifies that non-modifier keys are correctly identified.
     */
    @Test
    fun testIsModifierKeyFalse() {
        assertFalse(KeyboardInputProcessor.isModifierKey(KeyCodes.A))
        assertFalse(KeyboardInputProcessor.isModifierKey(KeyCodes.ENTER))
        assertFalse(KeyboardInputProcessor.isModifierKey(KeyCodes.DPAD_UP))
        assertFalse(KeyboardInputProcessor.isModifierKey(KeyCodes.F1))
    }

    // =========================================================================
    // getSpecialKeySequence Tests
    // =========================================================================

    /**
     * Verifies that regular keys don't have special sequences.
     *
     * Regular character keys (letters, numbers) should return null from
     * getSpecialKeySequence, indicating they should be handled differently
     * (either as Ctrl/Alt combinations or passed through to the system).
     */
    @Test
    fun testGetSpecialKeySequenceReturnsNullForRegularKeys() {
        assertNull(KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.A, hasShift = false))
        assertNull(KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.NUM_1, hasShift = false))
    }

    /**
     * Verifies arrow key sequence generation.
     * @see testArrowKeyUp for detailed explanation
     */
    @Test
    fun testGetSpecialKeySequenceArrows() {
        assertEquals("\u001B[A", KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.DPAD_UP, hasShift = false))
        assertEquals("\u001B[B", KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.DPAD_DOWN, hasShift = false))
        assertEquals("\u001B[C", KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.DPAD_RIGHT, hasShift = false))
        assertEquals("\u001B[D", KeyboardInputProcessor.getSpecialKeySequence(KeyCodes.DPAD_LEFT, hasShift = false))
    }

    // =========================================================================
    // getCtrlKeySequence Tests
    // =========================================================================

    /**
     * Verifies that non-control keys return null.
     *
     * Only keys with defined Ctrl mappings should return sequences.
     * Arrow keys and function keys don't have Ctrl character equivalents.
     */
    @Test
    fun testGetCtrlKeySequenceReturnsNullForUnmappedKeys() {
        assertNull(KeyboardInputProcessor.getCtrlKeySequence(KeyCodes.DPAD_UP))
        assertNull(KeyboardInputProcessor.getCtrlKeySequence(KeyCodes.F1))
    }

    /**
     * Verifies special Ctrl key combinations.
     *
     * ## Ctrl+[ = ESC
     * As explained in [testCtrlLeftBracket], this is the calculated result
     * of the control character formula: '[' (0x5B) - 0x40 = 0x1B (ESC)
     *
     * ## Ctrl+Space = NUL
     * Space (0x20) with Ctrl modifier typically sends NUL (0x00).
     * This deviates from the formula but is the established convention.
     */
    @Test
    fun testGetCtrlKeySequenceSpecialCases() {
        assertEquals("\u001B", KeyboardInputProcessor.getCtrlKeySequence(KeyCodes.LEFT_BRACKET))
        assertEquals("\u0000", KeyboardInputProcessor.getCtrlKeySequence(KeyCodes.SPACE))
    }

    // =========================================================================
    // getCharFromKeyCode Tests
    // =========================================================================

    /**
     * Verifies letter key character generation with shift state.
     *
     * This tests the basic letter mapping that's used for Alt+key combinations.
     */
    @Test
    fun testGetCharFromKeyCodeLetters() {
        assertEquals('a', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.A, hasShift = false))
        assertEquals('A', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.A, hasShift = true))
        assertEquals('z', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.Z, hasShift = false))
        assertEquals('Z', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.Z, hasShift = true))
    }

    /**
     * Verifies number key character generation.
     *
     * Note: The shift symbols follow JIS layout, not US layout.
     * @see testJisLayoutShiftNumbers for JIS-specific symbols
     */
    @Test
    fun testGetCharFromKeyCodeNumbers() {
        assertEquals('1', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_1, hasShift = false))
        assertEquals('!', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_1, hasShift = true))
        assertEquals('0', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_0, hasShift = false))
    }

    /**
     * Verifies symbol key character generation.
     */
    @Test
    fun testGetCharFromKeyCodeSymbols() {
        assertEquals('-', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.MINUS, hasShift = false))
        assertEquals('=', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.MINUS, hasShift = true))
        assertEquals('[', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.LEFT_BRACKET, hasShift = false))
        assertEquals('{', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.LEFT_BRACKET, hasShift = true))
    }

    /**
     * Verifies that special keys don't map to characters.
     *
     * Arrow keys, Enter, function keys, etc. don't produce printable characters
     * and should return null from this function.
     */
    @Test
    fun testGetCharFromKeyCodeReturnsNullForSpecialKeys() {
        assertNull(KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.DPAD_UP, hasShift = false))
        assertNull(KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.ENTER, hasShift = false))
        assertNull(KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.F1, hasShift = false))
    }

    // =========================================================================
    // JIS Keyboard Layout Tests
    // =========================================================================

    /**
     * Verifies JIS layout Shift+number symbol mappings.
     *
     * ## JIS vs US Layout Differences
     * The JIS (Japanese Industrial Standard) keyboard layout has different
     * Shift+number symbols compared to US layout:
     *
     * | Key | JIS (Shift) | US (Shift) |
     * |-----|-------------|------------|
     * | 2   | "           | @          |
     * | 6   | &           | ^          |
     * | 7   | '           | &          |
     * | 8   | (           | *          |
     * | 9   | )           | (          |
     *
     * This implementation targets Apple Magic Keyboard with JIS layout,
     * which is commonly used in Japan.
     */
    @Test
    fun testJisLayoutShiftNumbers() {
        assertEquals('"', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_2, hasShift = true))
        assertEquals('&', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_6, hasShift = true))
        assertEquals('\'', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_7, hasShift = true))
        assertEquals('(', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_8, hasShift = true))
        assertEquals(')', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.NUM_9, hasShift = true))
    }

    /**
     * Verifies JIS-specific key handling (YEN and RO keys).
     *
     * ## JIS-Specific Keys
     * JIS keyboards have additional keys not found on US keyboards:
     *
     * ### YEN key (¥)
     * - Located where backslash is on US keyboard
     * - Unshifted: produces backslash '\' (for path separators)
     * - Shifted: produces pipe '|'
     *
     * ### RO key (ろ)
     * - Located near right Shift
     * - Used for Japanese input
     * - Unshifted: produces backslash '\'
     * - Shifted: produces underscore '_'
     *
     * Note: Android reports these as separate key codes (KEYCODE_YEN, KEYCODE_RO)
     * distinct from KEYCODE_BACKSLASH.
     */
    @Test
    fun testJisSpecificKeys() {
        // YEN key (¥)
        assertEquals('\\', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.YEN, hasShift = false))
        assertEquals('|', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.YEN, hasShift = true))
        // RO key (ろ)
        assertEquals('\\', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.RO, hasShift = false))
        assertEquals('_', KeyboardInputProcessor.getCharFromKeyCode(KeyCodes.RO, hasShift = true))
    }
}
