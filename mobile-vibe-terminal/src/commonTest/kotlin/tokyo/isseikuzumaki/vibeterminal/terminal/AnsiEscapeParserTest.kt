package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.graphics.Color

/**
 * Unit tests for AnsiEscapeParser focusing on xterm control sequence compliance.
 * Based on Xterm Control Sequences specification.
 */
class AnsiEscapeParserTest {

    /**
     * Helper class that combines Parser + Executor for testing.
     * Provides the same interface as the old AnsiEscapeParser for backward compatibility.
     */
    private class TerminalProcessor(
        private val parser: AnsiEscapeParser,
        private val executor: TerminalCommandExecutor
    ) {
        fun processText(text: String) {
            val commands = parser.parse(text)
            executor.executeAll(commands)
        }
    }

    private fun createParser(): Pair<TerminalProcessor, TerminalScreenBuffer> {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)
        val parser = AnsiEscapeParser(defaultScrollBottom = 23)
        val executor = TerminalCommandExecutor(buffer)
        return Pair(TerminalProcessor(parser, executor), buffer)
    }

    // ========== CSI Cursor Movement Commands (VT100 Mode) ==========

    @Test
    fun testCursorUp_CUU() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(10, 10)

        parser.processText("\u001B[A")  // CSI A - Cursor Up 1 line (default)
        assertEquals(8, buffer.cursorRow, "Cursor should move up 1 line")
        assertEquals(9, buffer.cursorCol, "Cursor column should not change")

        parser.processText("\u001B[5A")  // CSI 5 A - Cursor Up 5 lines
        assertEquals(3, buffer.cursorRow, "Cursor should move up 5 lines")
    }

    @Test
    fun testCursorDown_CUD() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(5, 10)

        parser.processText("\u001B[B")  // CSI B - Cursor Down 1 line (default)
        assertEquals(5, buffer.cursorRow, "Cursor should move down 1 line")

        parser.processText("\u001B[3B")  // CSI 3 B - Cursor Down 3 lines
        assertEquals(8, buffer.cursorRow, "Cursor should move down 3 lines")
    }

    @Test
    fun testCursorForward_CUF() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(1, 1)

        parser.processText("\u001B[C")  // CSI C - Cursor Forward 1 column (default)
        assertEquals(1, buffer.cursorCol, "Cursor should move forward 1 column")

        parser.processText("\u001B[10C")  // CSI 10 C - Cursor Forward 10 columns
        assertEquals(11, buffer.cursorCol, "Cursor should move forward 10 columns")
    }

    @Test
    fun testCursorBackward_CUB() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(1, 20)

        parser.processText("\u001B[D")  // CSI D - Cursor Backward 1 column (default)
        assertEquals(18, buffer.cursorCol, "Cursor should move backward 1 column")

        parser.processText("\u001B[5D")  // CSI 5 D - Cursor Backward 5 columns
        assertEquals(13, buffer.cursorCol, "Cursor should move backward 5 columns")
    }

    @Test
    fun testCursorPosition_CUP() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[H")  // CSI H - Cursor to home (1,1)
        assertEquals(0, buffer.cursorRow, "Cursor should be at row 0 (1-indexed = 1)")
        assertEquals(0, buffer.cursorCol, "Cursor should be at col 0 (1-indexed = 1)")

        parser.processText("\u001B[10;20H")  // CSI 10;20 H - Cursor to row 10, col 20
        assertEquals(9, buffer.cursorRow, "Cursor should be at row 9 (1-indexed = 10)")
        assertEquals(19, buffer.cursorCol, "Cursor should be at col 19 (1-indexed = 20)")

        parser.processText("\u001B[5;5f")  // CSI 5;5 f - Alternative CUP command
        assertEquals(4, buffer.cursorRow, "f command should work like H")
        assertEquals(4, buffer.cursorCol, "f command should work like H")
    }

    @Test
    fun testCursorHorizontalAbsolute_CHA() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(5, 5)

        parser.processText("\u001B[10G")  // CSI 10 G - Move cursor to column 10
        assertEquals(4, buffer.cursorRow, "Row should not change")
        assertEquals(9, buffer.cursorCol, "Cursor should be at column 10 (0-indexed = 9)")

        parser.processText("\u001B[1`")  // CSI 1 ` - Alternative CHA (backtick)
        assertEquals(0, buffer.cursorCol, "Backtick command should move to column 1")
    }

    @Test
    fun testCursorVerticalAbsolute_VPA() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(5, 5)

        parser.processText("\u001B[10d")  // CSI 10 d - Move cursor to row 10
        assertEquals(9, buffer.cursorRow, "Cursor should be at row 10 (0-indexed = 9)")
        assertEquals(4, buffer.cursorCol, "Column should not change")
    }

    // ========== Erase Commands ==========

    @Test
    fun testEraseInDisplay_ED() {
        val (parser, buffer) = createParser()
        // Fill buffer with 'X'
        for (r in 0 until 24) {
            for (c in 0 until 80) {
                buffer.moveCursor(r + 1, c + 1)
                buffer.writeChar('X')
            }
        }

        buffer.moveCursor(10, 10)
        parser.processText("\u001B[J")  // CSI J or CSI 0 J - Erase from cursor to end of display

        // Check that cursor position and before are unchanged
        assertEquals('X', buffer.getBuffer()[9][8].char, "Before cursor should be unchanged")
        // Check that cursor and after are erased
        assertEquals(' ', buffer.getBuffer()[9][9].char, "Cursor position should be erased")
        assertEquals(' ', buffer.getBuffer()[23][79].char, "End of screen should be erased")
    }

    @Test
    fun testEraseInDisplay_ED_Mode1() {
        val (parser, buffer) = createParser()
        // Fill buffer with 'X'
        for (r in 0 until 24) {
            for (c in 0 until 80) {
                buffer.moveCursor(r + 1, c + 1)
                buffer.writeChar('X')
            }
        }

        buffer.moveCursor(10, 10)
        parser.processText("\u001B[1J")  // CSI 1 J - Erase from start to cursor

        assertEquals(' ', buffer.getBuffer()[0][0].char, "Start of screen should be erased")
        assertEquals(' ', buffer.getBuffer()[9][9].char, "Cursor position should be erased")
        assertEquals('X', buffer.getBuffer()[9][10].char, "After cursor should be unchanged")
    }

    @Test
    fun testEraseInDisplay_ED_Mode2() {
        val (parser, buffer) = createParser()
        // Fill buffer with 'X'
        for (r in 0 until 24) {
            for (c in 0 until 80) {
                buffer.moveCursor(r + 1, c + 1)
                buffer.writeChar('X')
            }
        }

        buffer.moveCursor(10, 10)
        parser.processText("\u001B[2J")  // CSI 2 J - Erase entire display

        assertEquals(' ', buffer.getBuffer()[0][0].char, "Entire screen should be erased")
        assertEquals(' ', buffer.getBuffer()[23][79].char, "Entire screen should be erased")
        assertEquals(0, buffer.cursorRow, "Cursor should be at home after clear")
        assertEquals(0, buffer.cursorCol, "Cursor should be at home after clear")
    }

    @Test
    fun testEraseInLine_EL() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(5, 1)
        parser.processText("ABCDEFGHIJKLMNOPQRSTUVWXYZ")

        buffer.moveCursor(5, 10)
        parser.processText("\u001B[K")  // CSI K or CSI 0 K - Erase from cursor to end of line

        assertEquals('I', buffer.getBuffer()[4][8].char, "Before cursor should be unchanged")
        assertEquals(' ', buffer.getBuffer()[4][9].char, "Cursor position should be erased")
        assertEquals(' ', buffer.getBuffer()[4][79].char, "End of line should be erased")
    }

    @Test
    fun testEraseInLine_EL_Mode1() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(5, 1)
        parser.processText("ABCDEFGHIJKLMNOPQRSTUVWXYZ")

        buffer.moveCursor(5, 10)
        parser.processText("\u001B[1K")  // CSI 1 K - Erase from start to cursor

        assertEquals(' ', buffer.getBuffer()[4][0].char, "Start of line should be erased")
        assertEquals(' ', buffer.getBuffer()[4][9].char, "Cursor position should be erased")
        assertEquals('K', buffer.getBuffer()[4][10].char, "After cursor should be unchanged")
    }

    @Test
    fun testEraseInLine_EL_Mode2() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(5, 1)
        parser.processText("ABCDEFGHIJKLMNOPQRSTUVWXYZ")

        buffer.moveCursor(5, 10)
        parser.processText("\u001B[2K")  // CSI 2 K - Erase entire line

        assertEquals(' ', buffer.getBuffer()[4][0].char, "Entire line should be erased")
        assertEquals(' ', buffer.getBuffer()[4][9].char, "Entire line should be erased")
        assertEquals(' ', buffer.getBuffer()[4][79].char, "Entire line should be erased")
    }

    // ========== Line Manipulation Commands ==========

    @Test
    fun testInsertLines_IL() {
        val (parser, buffer) = createParser()

        // Write text at specific positions
        parser.processText("\u001B[1;1HLine 1")
        parser.processText("\u001B[2;1HLine 2")
        parser.processText("\u001B[3;1HLine 3")

        // Move to line 2 and insert 1 line
        parser.processText("\u001B[2;1H")  // Move to line 2
        parser.processText("\u001B[L")  // CSI L - Insert 1 blank line

        val screenBuffer = buffer.getBuffer()
        assertEquals('L', screenBuffer[0][0].char, "Line 1 should remain")
        assertEquals(' ', screenBuffer[1][0].char, "Inserted line should be blank")
        assertEquals('L', screenBuffer[2][0].char, "Line 2 should shift down")
    }

    @Test
    fun testDeleteLines_DL() {
        val (parser, buffer) = createParser()

        // Write text at specific positions
        parser.processText("\u001B[1;1HLine 1")
        parser.processText("\u001B[2;1HLine 2")
        parser.processText("\u001B[3;1HLine 3")

        // Move to line 2 and delete 1 line
        parser.processText("\u001B[2;1H")  // Move to line 2
        parser.processText("\u001B[M")  // CSI M - Delete 1 line

        val screenBuffer = buffer.getBuffer()
        assertEquals('L', screenBuffer[0][0].char, "Line 1 should remain")
        assertEquals('L', screenBuffer[1][0].char, "Line 3 should move up")
        assertEquals(' ', screenBuffer[2][0].char, "Deleted line should be blank")
    }

    // ========== Character Manipulation Commands ==========

    @Test
    fun testDeleteCharacters_DCH() {
        val (parser, buffer) = createParser()

        parser.processText("ABCDEFGHIJ")
        parser.processText("\u001B[1;3H")  // Move to column 3 ('C')
        parser.processText("\u001B[2P")    // CSI 2 P - Delete 2 characters

        val screenBuffer = buffer.getBuffer()
        assertEquals('A', screenBuffer[0][0].char, "A should remain")
        assertEquals('B', screenBuffer[0][1].char, "B should remain")
        assertEquals('E', screenBuffer[0][2].char, "E should shift left to position of C")
        assertEquals('F', screenBuffer[0][3].char, "F should shift left")
        assertEquals(' ', screenBuffer[0][8].char, "Position 9 should be blank after shift")
        assertEquals(' ', screenBuffer[0][9].char, "Position 10 should be blank after shift")
    }

    @Test
    fun testInsertCharacters_ICH() {
        val (parser, buffer) = createParser()

        parser.processText("ABCDEFGHIJ")
        parser.processText("\u001B[1;3H")  // Move to column 3 ('C')
        parser.processText("\u001B[2@")    // CSI 2 @ - Insert 2 blank characters

        val screenBuffer = buffer.getBuffer()
        assertEquals('A', screenBuffer[0][0].char, "A should remain")
        assertEquals('B', screenBuffer[0][1].char, "B should remain")
        assertEquals(' ', screenBuffer[0][2].char, "Position 3 should be blank (inserted)")
        assertEquals(' ', screenBuffer[0][3].char, "Position 4 should be blank (inserted)")
        assertEquals('C', screenBuffer[0][4].char, "C should shift right")
        assertEquals('D', screenBuffer[0][5].char, "D should shift right")
    }

    @Test
    fun testEraseCharacters_ECH() {
        val (parser, buffer) = createParser()

        parser.processText("ABCDEFGHIJ")
        parser.processText("\u001B[1;3H")  // Move to column 3 ('C')
        parser.processText("\u001B[3X")    // CSI 3 X - Erase 3 characters (no shift)

        val screenBuffer = buffer.getBuffer()
        assertEquals('A', screenBuffer[0][0].char, "A should remain")
        assertEquals('B', screenBuffer[0][1].char, "B should remain")
        assertEquals(' ', screenBuffer[0][2].char, "C should be erased")
        assertEquals(' ', screenBuffer[0][3].char, "D should be erased")
        assertEquals(' ', screenBuffer[0][4].char, "E should be erased")
        assertEquals('F', screenBuffer[0][5].char, "F should remain (no shift)")
    }

    @Test
    fun testScrollUp_SU() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[1;1HLine 1")
        parser.processText("\u001B[2;1HLine 2")
        parser.processText("\u001B[3;1HLine 3")
        parser.processText("\u001B[S")  // CSI S - Scroll up 1 line

        val screenBuffer = buffer.getBuffer()
        assertEquals('L', screenBuffer[0][0].char, "Line 2 should scroll to top")
        assertEquals('i', screenBuffer[0][1].char)
        assertEquals('n', screenBuffer[0][2].char)
        assertEquals('e', screenBuffer[0][3].char)
        assertEquals(' ', screenBuffer[0][4].char)
        assertEquals('2', screenBuffer[0][5].char, "Should be Line 2")
    }

    @Test
    fun testScrollDown_SD() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[1;1HLine 1")
        parser.processText("\u001B[2;1HLine 2")
        parser.processText("\u001B[3;1HLine 3")
        parser.processText("\u001B[T")  // CSI T - Scroll down 1 line

        val screenBuffer = buffer.getBuffer()
        assertEquals(' ', screenBuffer[0][0].char, "First line should be blank after scroll down")
        assertEquals('L', screenBuffer[1][0].char, "Line 1 should move to second row")
    }

    // ========== SGR - Select Graphic Rendition ==========

    @Test
    fun testSGR_Reset() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[1;4;31m")  // Bold, underline, red
        parser.processText("X")
        val styledCell = buffer.getBuffer()[0][0]
        assertTrue(styledCell.isBold, "Should be bold")
        assertTrue(styledCell.isUnderline, "Should be underline")

        parser.processText("\u001B[0m")  // Reset
        parser.processText("Y")
        val resetCell = buffer.getBuffer()[0][1]
        assertTrue(!resetCell.isBold, "Should not be bold after reset")
        assertTrue(!resetCell.isUnderline, "Should not be underline after reset")
        assertEquals(Color.White, resetCell.foregroundColor, "Should be white after reset")
    }

    @Test
    fun testSGR_ForegroundColors() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[31mR")  // Red
        parser.processText("\u001B[32mG")  // Green
        parser.processText("\u001B[33mY")  // Yellow
        parser.processText("\u001B[34mB")  // Blue
        parser.processText("\u001B[35mM")  // Magenta
        parser.processText("\u001B[36mC")  // Cyan
        parser.processText("\u001B[37mW")  // White

        assertEquals(Color(0xFFFF5555), buffer.getBuffer()[0][0].foregroundColor, "Red")
        assertEquals(Color(0xFF50FA7B), buffer.getBuffer()[0][1].foregroundColor, "Green")
        assertEquals(Color(0xFFF1FA8C), buffer.getBuffer()[0][2].foregroundColor, "Yellow")
        assertEquals(Color(0xFFBD93F9), buffer.getBuffer()[0][3].foregroundColor, "Blue")
        assertEquals(Color(0xFFFF79C6), buffer.getBuffer()[0][4].foregroundColor, "Magenta")
        assertEquals(Color(0xFF8BE9FD), buffer.getBuffer()[0][5].foregroundColor, "Cyan")
        assertEquals(Color.White, buffer.getBuffer()[0][6].foregroundColor, "White")
    }

    @Test
    fun testSGR_BackgroundColors() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[41mR")  // Red background
        parser.processText("\u001B[42mG")  // Green background
        parser.processText("\u001B[43mY")  // Yellow background

        assertEquals(Color(0xFFFF5555), buffer.getBuffer()[0][0].backgroundColor, "Red bg")
        assertEquals(Color(0xFF50FA7B), buffer.getBuffer()[0][1].backgroundColor, "Green bg")
        assertEquals(Color(0xFFF1FA8C), buffer.getBuffer()[0][2].backgroundColor, "Yellow bg")
    }

    @Test
    fun testSGR_BrightForegroundColors() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[90mX")  // Bright Black (Gray)
        parser.processText("\u001B[91mX")  // Bright Red
        parser.processText("\u001B[92mX")  // Bright Green
        parser.processText("\u001B[93mX")  // Bright Yellow
        parser.processText("\u001B[94mX")  // Bright Blue
        parser.processText("\u001B[95mX")  // Bright Magenta
        parser.processText("\u001B[96mX")  // Bright Cyan
        parser.processText("\u001B[97mX")  // Bright White

        assertEquals(Color(0xFF6E6E6E), buffer.getBuffer()[0][0].foregroundColor, "Bright Black")
        assertEquals(Color(0xFFFF6E6E), buffer.getBuffer()[0][1].foregroundColor, "Bright Red")
        assertEquals(Color(0xFF69FF94), buffer.getBuffer()[0][2].foregroundColor, "Bright Green")
        assertEquals(Color(0xFFFFFFA5), buffer.getBuffer()[0][3].foregroundColor, "Bright Yellow")
        assertEquals(Color(0xFFD6ACFF), buffer.getBuffer()[0][4].foregroundColor, "Bright Blue")
        assertEquals(Color(0xFFFF92DF), buffer.getBuffer()[0][5].foregroundColor, "Bright Magenta")
        assertEquals(Color(0xFFA4FFFF), buffer.getBuffer()[0][6].foregroundColor, "Bright Cyan")
        assertEquals(Color(0xFFFFFFFF), buffer.getBuffer()[0][7].foregroundColor, "Bright White")
    }

    @Test
    fun testSGR_BrightBackgroundColors() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[100mX")  // Bright Black bg
        parser.processText("\u001B[101mX")  // Bright Red bg
        parser.processText("\u001B[102mX")  // Bright Green bg
        parser.processText("\u001B[103mX")  // Bright Yellow bg

        assertEquals(Color(0xFF6E6E6E), buffer.getBuffer()[0][0].backgroundColor, "Bright Black bg")
        assertEquals(Color(0xFFFF6E6E), buffer.getBuffer()[0][1].backgroundColor, "Bright Red bg")
        assertEquals(Color(0xFF69FF94), buffer.getBuffer()[0][2].backgroundColor, "Bright Green bg")
        assertEquals(Color(0xFFFFFFA5), buffer.getBuffer()[0][3].backgroundColor, "Bright Yellow bg")
    }

    @Test
    fun testSGR_256Colors_Foreground() {
        val (parser, buffer) = createParser()

        // Standard colors (0-15) - Index 1 = Red
        parser.processText("\u001B[38;5;1mX")
        assertEquals(Color(0xFFFF5555), buffer.getBuffer()[0][0].foregroundColor, "Index 1 should be Red")

        // Color cube (16-231) - Index 196 = Red (part of 6x6x6 cube)
        // 196 = 16 + 36*5 + 6*0 + 0 = Red
        parser.processText("\u001B[38;5;196mX")
        // Note: Exact RGB value depends on palette implementation, but for 196 it's typically FF0000
        // We'll verify it's parsed and not the default color
        assertTrue(buffer.getBuffer()[0][1].foregroundColor != Color.White, "Index 196 should not be default white")

        // Grayscale (232-255) - Index 255 = Nearly White
        parser.processText("\u001B[38;5;255mX")
        // Grayscale 255 is usually #EEEEEE
        assertTrue(buffer.getBuffer()[0][2].foregroundColor != Color.Black, "Index 255 should not be black")
    }

    @Test
    fun testSGR_256Colors_Background() {
        val (parser, buffer) = createParser()

        // Index 2 = Green
        parser.processText("\u001B[48;5;2mX")
        assertEquals(Color(0xFF50FA7B), buffer.getBuffer()[0][0].backgroundColor, "Index 2 should be Green")

        // Index 21 = Blue (0,0,5 in cube)
        parser.processText("\u001B[48;5;21mX")
        assertTrue(buffer.getBuffer()[0][1].backgroundColor != Color.Black, "Index 21 should not be default black")
    }

    @Test
    fun testSGR_TrueColor_Foreground() {
        val (parser, buffer) = createParser()

        // RGB: 123, 45, 67
        parser.processText("\u001B[38;2;123;45;67mX")
        
        val color = buffer.getBuffer()[0][0].foregroundColor
        assertEquals(Color(123, 45, 67), color, "Should match exact RGB value")
    }

    @Test
    fun testSGR_TrueColor_Background() {
        val (parser, buffer) = createParser()

        // RGB: 10, 200, 255
        parser.processText("\u001B[48;2;10;200;255mX")

        val color = buffer.getBuffer()[0][0].backgroundColor
        assertEquals(Color(10, 200, 255), color, "Should match exact RGB value")
    }

    @Test
    fun testSGR_TextAttributes() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[1mB")  // Bold
        assertTrue(buffer.getBuffer()[0][0].isBold, "Should be bold")

        parser.processText("\u001B[4mU")  // Underline
        assertTrue(buffer.getBuffer()[0][1].isUnderline, "Should be underline")

        parser.processText("\u001B[7mI")  // Inverse/Reverse
        assertTrue(buffer.getBuffer()[0][2].isReverse, "Should be reverse")
    }

    @Test
    fun testSGR_ReverseVideo() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[31;44m")  // Red on blue
        parser.processText("\u001B[7m")       // Reverse video
        parser.processText("X")

        val cell = buffer.getBuffer()[0][0]
        // In reverse mode, foreground and background should be swapped
        assertEquals(Color(0xFFBD93F9), cell.foregroundColor, "Foreground should be blue (swapped)")
        assertEquals(Color(0xFFFF5555), cell.backgroundColor, "Background should be red (swapped)")
    }

    // ========== Cursor Save/Restore ==========

    @Test
    fun testSaveCursor_DECSC() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(10, 20)

        parser.processText("\u001B7")  // ESC 7 - Save cursor
        buffer.moveCursor(5, 5)
        parser.processText("\u001B8")  // ESC 8 - Restore cursor

        assertEquals(9, buffer.cursorRow, "Cursor row should be restored")
        assertEquals(19, buffer.cursorCol, "Cursor col should be restored")
    }

    @Test
    fun testSaveCursor_CSI() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(15, 25)

        parser.processText("\u001B[s")  // CSI s - Save cursor
        buffer.moveCursor(1, 1)
        parser.processText("\u001B[u")  // CSI u - Restore cursor

        assertEquals(14, buffer.cursorRow, "Cursor row should be restored")
        assertEquals(24, buffer.cursorCol, "Cursor col should be restored")
    }

    // ========== Control Characters ==========

    @Test
    fun testCarriageReturn() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(5, 20)

        parser.processText("\r")  // CR - Carriage return

        assertEquals(4, buffer.cursorRow, "Row should not change")
        assertEquals(0, buffer.cursorCol, "Column should return to 0")
    }

    @Test
    fun testLineFeed() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(5, 20)

        parser.processText("\n")  // LF - Line feed

        assertEquals(5, buffer.cursorRow, "Row should increment")
        assertEquals(19, buffer.cursorCol, "Column should not change")
    }

    @Test
    fun testBackspace() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(5, 20)  // Move to 1-indexed (5, 20) = 0-indexed (4, 19)

        parser.processText("\b")  // BS - Backspace

        assertEquals(4, buffer.cursorRow, "Row should not change")
        assertEquals(18, buffer.cursorCol, "Column should decrement by 1")
    }

    @Test
    fun testTab() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(1, 1)

        parser.processText("\t")  // Tab - Horizontal tab (every 8 columns)
        assertEquals(8, buffer.cursorCol, "Should move to column 8 (0-indexed)")

        parser.processText("\t")
        assertEquals(16, buffer.cursorCol, "Should move to column 16 (0-indexed)")
    }

    // ========== Scroll Region (DECSTBM) ==========

    @Test
    fun testScrollRegion_DECSTBM() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[5;20r")  // CSI 5;20 r - Set scroll region lines 5-20

        // Scroll region is internal state, test by filling buffer and scrolling
        buffer.moveCursor(20, 1)
        parser.processText("Bottom of region\n")  // Should trigger scroll

        // The implementation should only scroll within region 5-20
        // This is tested more in TerminalScreenBufferTest
    }

    @Test
    fun testResetScrollRegion_DECSTBM() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[5;20r")  // Set scroll region
        parser.processText("\u001B[r")      // Reset scroll region to full screen

        // After reset, scrolling should affect entire screen
        // Verified through buffer operations
    }

    @Test
    fun testScrollRegion_DECSTBM_NonStandardRows() {
        // Test DECSTBM with a buffer that has different row count than 24
        // This tests that the default bottom value uses actual buffer rows, not hardcoded 24
        val buffer = TerminalScreenBuffer(cols = 80, rows = 40)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // Set only top margin, bottom should default to last row (40, not 24)
        parser.processText("\u001B[5r")  // CSI 5 r - Set top=5, bottom=default

        // Check that scroll region is (4, 39) not (4, 23)
        val (top, bottom) = buffer.scrollRegion
        assertEquals(4, top, "Top should be 4 (0-indexed from 5)")
        assertEquals(39, bottom, "Bottom should be 39 (last row of 40-row buffer)")
    }

    @Test
    fun testScrollRegion_DECSTBM_ResetWithNonStandardRows() {
        // Test that reset uses actual buffer rows
        val buffer = TerminalScreenBuffer(cols = 80, rows = 30)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        parser.processText("\u001B[5;20r")  // Set custom scroll region
        parser.processText("\u001B[r")      // Reset scroll region

        // After reset, scroll region should be full screen (0, 29)
        val (top, bottom) = buffer.scrollRegion
        assertEquals(0, top, "Top should be 0 after reset")
        assertEquals(29, bottom, "Bottom should be 29 (last row of 30-row buffer)")
    }

    // ========== Alternate Screen Buffer ==========

    @Test
    fun testAlternateScreenBuffer_DECSET() {
        val (parser, buffer) = createParser()

        parser.processText("Primary screen")
        assertTrue(!buffer.isAlternateScreen, "Should start on primary screen")

        parser.processText("\u001B[?1049h")  // CSI ? 1049 h - Use alternate screen buffer
        assertTrue(buffer.isAlternateScreen, "Should switch to alternate screen")

        // Alternate screen should be empty
        assertEquals(' ', buffer.getBuffer()[0][0].char, "Alternate screen should be empty")

        parser.processText("\u001B[?1049l")  // CSI ? 1049 l - Use primary screen buffer
        assertTrue(!buffer.isAlternateScreen, "Should switch back to primary screen")

        assertEquals('P', buffer.getBuffer()[0][0].char, "Primary screen content should be restored")
    }

    // ========== Character Set Selection (SCS) ==========

    @Test
    fun testCharacterSetSelection_G0() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B(B")  // ESC ( B - Select ASCII into G0
        parser.processText("A")
        assertEquals('A', buffer.getBuffer()[0][0].char, "Should write ASCII character")

        parser.processText("\u001B(0")  // ESC ( 0 - Select DEC Special Graphics into G0
        // Currently implementation acknowledges but doesn't map special chars
        // This prevents "B" artifacts from appearing
    }

    @Test
    fun testCharacterSetSelection_G1() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B)B")  // ESC ) B - Select ASCII into G1
        parser.processText("\u001B)0")  // ESC ) 0 - Select DEC Special Graphics into G1
        // Implementation should acknowledge without error
    }

    @Test
    fun testCharacterSet_DECSpecialGraphics_LineDrawing() {
        val (parser, buffer) = createParser()

        // Switch to DEC Special Graphics
        parser.processText("\u001B(0")

        // Test box drawing characters
        parser.processText("l")  // Upper-left corner
        assertEquals('┌', buffer.getBuffer()[0][0].char, "l should map to ┌")

        parser.processText("k")  // Upper-right corner
        assertEquals('┐', buffer.getBuffer()[0][1].char, "k should map to ┐")

        parser.processText("m")  // Lower-left corner
        assertEquals('└', buffer.getBuffer()[0][2].char, "m should map to └")

        parser.processText("j")  // Lower-right corner
        assertEquals('┘', buffer.getBuffer()[0][3].char, "j should map to ┘")

        parser.processText("q")  // Horizontal line
        assertEquals('─', buffer.getBuffer()[0][4].char, "q should map to ─")

        parser.processText("x")  // Vertical line
        assertEquals('│', buffer.getBuffer()[0][5].char, "x should map to │")

        // Switch back to ASCII
        parser.processText("\u001B(B")
        parser.processText("l")
        assertEquals('l', buffer.getBuffer()[0][6].char, "Should be normal 'l' in ASCII mode")
    }

    @Test
    fun testCursorVisibility_DECTCEM() {
        val (parser, buffer) = createParser()

        // Initial state should be visible
        assertTrue(buffer.isCursorVisible, "Cursor should be visible by default")

        // Hide cursor
        parser.processText("\u001B[?25l")
        assertTrue(!buffer.isCursorVisible, "Cursor should be hidden")

        // Show cursor
        parser.processText("\u001B[?25h")
        assertTrue(buffer.isCursorVisible, "Cursor should be visible")
    }

    // ========== Edge Cases and Boundary Conditions ==========

    @Test
    fun testCursorMovement_Boundaries() {
        val (parser, buffer) = createParser()

        // Try to move beyond screen boundaries
        parser.processText("\u001B[999A")  // Move up 999 lines
        assertEquals(0, buffer.cursorRow, "Should clamp to top")

        parser.processText("\u001B[999B")  // Move down 999 lines
        assertEquals(23, buffer.cursorRow, "Should clamp to bottom")

        buffer.moveCursor(1, 1)
        parser.processText("\u001B[999D")  // Move left 999 columns
        assertEquals(0, buffer.cursorCol, "Should clamp to left")

        parser.processText("\u001B[999C")  // Move right 999 columns
        assertEquals(79, buffer.cursorCol, "Should clamp to right")
    }

    @Test
    fun testEmptyCSIParameters() {
        val (parser, buffer) = createParser()

        // Empty parameters should use defaults
        parser.processText("\u001B[H")   // Should move to 1,1 (default)
        assertEquals(0, buffer.cursorRow, "Empty H should go to home")
        assertEquals(0, buffer.cursorCol, "Empty H should go to home")

        buffer.moveCursor(10, 10)
        parser.processText("\u001B[A")   // Should move up 1 (default)
        assertEquals(8, buffer.cursorRow, "Empty A should move up 1")
    }

    @Test
    fun testMultipleParameterSGR() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[1;4;31;44m")  // Bold, underline, red fg, blue bg
        parser.processText("X")

        val cell = buffer.getBuffer()[0][0]
        assertTrue(cell.isBold, "Should be bold")
        assertTrue(cell.isUnderline, "Should be underline")
        assertEquals(Color(0xFFFF5555), cell.foregroundColor, "Should be red")
        assertEquals(Color(0xFFBD93F9), cell.backgroundColor, "Should be blue")
    }

    @Test
    fun testIncompleteEscapeSequences() {
        val (parser, buffer) = createParser()

        // Incomplete sequences should not crash
        parser.processText("\u001B")      // Just ESC
        parser.processText("A")           // 'A' after ESC is treated as unknown escape, returns to NORMAL
        // Current implementation: unknown escape chars return to NORMAL but don't re-process the char
        // So 'A' is not written. This is a known limitation.
        assertEquals(' ', buffer.getBuffer()[0][0].char, "Current behavior: char after invalid ESC is dropped")
    }

    @Test
    fun testReverseIndex_RI() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(5, 5)

        parser.processText("\u001BM")  // ESC M - Reverse index (move up)

        assertEquals(3, buffer.cursorRow, "Should move up one line")
        assertEquals(4, buffer.cursorCol, "Column should not change")
    }

    // ========== DECSCUSR - Set Cursor Style ==========

    @Test
    fun testDECSCUSR_BlinkingBlock() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(1, 1)

        parser.processText("\u001B[1 q")  // CSI 1 SP q - Blinking block cursor
        parser.processText("X")

        // The 'q' should not appear on screen - it should be consumed by the parser
        assertEquals('X', buffer.getBuffer()[0][0].char, "Only 'X' should be written, not 'q'")
        assertEquals(' ', buffer.getBuffer()[0][1].char, "Position after 'X' should be empty")
    }

    @Test
    fun testDECSCUSR_SteadyBlock() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(1, 1)

        parser.processText("\u001B[2 q")  // CSI 2 SP q - Steady block cursor
        parser.processText("X")

        assertEquals('X', buffer.getBuffer()[0][0].char, "Only 'X' should be written, not 'q'")
        assertEquals(' ', buffer.getBuffer()[0][1].char, "Position after 'X' should be empty")
    }

    @Test
    fun testDECSCUSR_BlinkingUnderline() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(1, 1)

        parser.processText("\u001B[3 q")  // CSI 3 SP q - Blinking underline cursor
        parser.processText("X")

        assertEquals('X', buffer.getBuffer()[0][0].char, "Only 'X' should be written, not 'q'")
    }

    @Test
    fun testDECSCUSR_SteadyUnderline() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(1, 1)

        parser.processText("\u001B[4 q")  // CSI 4 SP q - Steady underline cursor
        parser.processText("X")

        assertEquals('X', buffer.getBuffer()[0][0].char, "Only 'X' should be written, not 'q'")
    }

    @Test
    fun testDECSCUSR_BlinkingBar() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(1, 1)

        parser.processText("\u001B[5 q")  // CSI 5 SP q - Blinking bar cursor
        parser.processText("X")

        assertEquals('X', buffer.getBuffer()[0][0].char, "Only 'X' should be written, not 'q'")
    }

    @Test
    fun testDECSCUSR_SteadyBar() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(1, 1)

        parser.processText("\u001B[6 q")  // CSI 6 SP q - Steady bar cursor
        parser.processText("X")

        assertEquals('X', buffer.getBuffer()[0][0].char, "Only 'X' should be written, not 'q'")
    }

    @Test
    fun testDECSCUSR_Default() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(1, 1)

        parser.processText("\u001B[0 q")  // CSI 0 SP q - Default cursor (blinking block)
        parser.processText("X")

        assertEquals('X', buffer.getBuffer()[0][0].char, "Only 'X' should be written, not 'q'")
    }

    @Test
    fun testDECSCUSR_NoParameter() {
        val (parser, buffer) = createParser()
        buffer.moveCursor(1, 1)

        parser.processText("\u001B[ q")  // CSI SP q - No parameter (default)
        parser.processText("X")

        assertEquals('X', buffer.getBuffer()[0][0].char, "Only 'X' should be written, not 'q'")
    }

    // ========== Wide Character (Full-width) Handling ==========

    /**
     * Test: ひらがな（全角文字）の基本的な書き込み
     *
     * 入力: "あいうえお" (5文字のひらがな)
     *
     * 期待されるバッファ状態:
     *   Col:  0   1   2   3   4   5   6   7   8   9
     *        [あ][P] [い][P] [う][P] [え][P] [お][P]
     *        ↑       ↑       ↑       ↑       ↑
     *        wide    wide    wide    wide    wide
     *   (P = padding cell, 全角文字の右半分を占めるダミーセル)
     *
     * 全角文字は2セル幅を占めるため、5文字で10セル消費。
     * カーソルは列10に移動する。
     */
    @Test
    fun testWideChar_Japanese_Hiragana() {
        val (parser, buffer) = createParser()

        parser.processText("あいうえお")  // Hiragana

        val screenBuffer = buffer.getBuffer()
        assertEquals('あ', screenBuffer[0][0].char, "First hiragana")
        assertTrue(screenBuffer[0][0].isWideChar)
        assertTrue(screenBuffer[0][1].isWideCharPadding)

        assertEquals('い', screenBuffer[0][2].char, "Second hiragana")
        assertTrue(screenBuffer[0][2].isWideChar)
        assertTrue(screenBuffer[0][3].isWideCharPadding)

        assertEquals(10, buffer.cursorCol, "Cursor at col 10 (5 wide chars * 2)")
    }

    /**
     * Test: カタカナ（全角文字）の書き込み
     *
     * 入力: "アイウエオ" (5文字のカタカナ)
     *
     * 期待されるバッファ状態:
     *   Col:  0   1   2   3   4   5   6   7   8   9
     *        [ア][P] [イ][P] [ウ][P] [エ][P] [オ][P]
     *
     * カタカナもひらがな同様、全角文字として2セル幅で表示。
     */
    @Test
    fun testWideChar_Japanese_Katakana() {
        val (parser, buffer) = createParser()

        parser.processText("アイウエオ")  // Katakana

        val screenBuffer = buffer.getBuffer()
        assertEquals('ア', screenBuffer[0][0].char, "First katakana")
        assertTrue(screenBuffer[0][0].isWideChar)
        assertEquals('イ', screenBuffer[0][2].char, "Second katakana")
        assertEquals('ウ', screenBuffer[0][4].char, "Third katakana")
    }

    /**
     * Test: 漢字とカタカナの混在
     *
     * 入力: "日本語テスト" (漢字3文字 + カタカナ3文字)
     *
     * 期待されるバッファ状態:
     *   Col:  0   1   2   3   4   5   6   7   8   9  10  11
     *        [日][P] [本][P] [語][P] [テ][P] [ス][P] [ト][P]
     *
     * 全6文字 × 2セル = 12セル消費
     */
    @Test
    fun testWideChar_Japanese_Kanji() {
        val (parser, buffer) = createParser()

        parser.processText("日本語テスト")  // Kanji + Katakana

        val screenBuffer = buffer.getBuffer()
        assertEquals('日', screenBuffer[0][0].char)
        assertEquals('本', screenBuffer[0][2].char)
        assertEquals('語', screenBuffer[0][4].char)
        assertEquals('テ', screenBuffer[0][6].char)
        assertEquals('ス', screenBuffer[0][8].char)
        assertEquals('ト', screenBuffer[0][10].char)
    }

    /**
     * Test: 半角ASCII文字と全角文字の混在
     *
     * 入力: "Hello日本語World"
     *
     * 期待されるバッファ状態:
     *   Col:  0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
     *        [H] [e] [l] [l] [o] [日][P] [本][P] [語][P] [W] [o] [r] [l] [d]
     *        ←── 半角5文字 ──→ ←── 全角3文字(6セル) ──→ ←── 半角5文字 ──→
     *
     * 半角文字は1セル、全角文字は2セルを消費。
     * 合計: 5 + 6 + 5 = 16セル
     */
    @Test
    fun testWideChar_MixedWithASCII() {
        val (parser, buffer) = createParser()

        parser.processText("Hello日本語World")

        val screenBuffer = buffer.getBuffer()
        // "Hello" = 5 narrow chars
        assertEquals('H', screenBuffer[0][0].char)
        assertEquals('e', screenBuffer[0][1].char)
        assertEquals('l', screenBuffer[0][2].char)
        assertEquals('l', screenBuffer[0][3].char)
        assertEquals('o', screenBuffer[0][4].char)
        // "日本語" = 3 wide chars (6 cells)
        assertEquals('日', screenBuffer[0][5].char)
        assertTrue(screenBuffer[0][5].isWideChar)
        assertEquals('本', screenBuffer[0][7].char)
        assertEquals('語', screenBuffer[0][9].char)
        // "World" = 5 narrow chars
        assertEquals('W', screenBuffer[0][11].char)
    }

    /**
     * Test: カーソル移動後に文字を上書き
     *
     * シーケンス:
     *   1. "あいう" を書き込み → バッファ: [あ][P][い][P][う][P]
     *   2. ESC[1;3H (CUP) → カーソルを行1、列3に移動（0-indexedで列2、'い'の開始位置）
     *   3. "X" を書き込み
     *
     * 期待される動作:
     *   'い' の位置に文字が書き込まれる。
     *   前の文字 'あ' は影響を受けない。
     *
     * 期待されるバッファ状態:
     *   Before: [あ][P][い][P][う][P]
     *   After:  [あ][P][X] [ ][う][P]
     *                 ↑ 'い'が上書きされ、そのパディングもクリアされるのが望ましい
     */
    @Test
    fun testWideChar_WithCursorMovement() {
        val (parser, buffer) = createParser()

        parser.processText("あいう")
        parser.processText("\u001B[1;3H")  // Move to column 3 (start of 'い')
        parser.processText("X")

        val screenBuffer = buffer.getBuffer()
        
        // 'あ' should remain intact
        assertEquals('あ', screenBuffer[0][0].char, "First wide char 'あ' should remain")
        assertTrue(screenBuffer[0][0].isWideChar)
        
        // 'い' should be overwritten by 'X'
        assertEquals('X', screenBuffer[0][2].char, "X should overwrite 'い'")
        // The padding of 'い' should be cleared (or at least handled safely)
        assertEquals(' ', screenBuffer[0][3].char, "Padding of 'い' should be cleared")
    }

    /**
     * Test: 全角文字列の途中から行末まで消去
     *
     * シーケンス:
     *   1. "あいうえお" を書き込み → [あ][P][い][P][う][P][え][P][お][P]
     *   2. ESC[1;5H → カーソルを列5に移動（0-indexedで列4、'う'の開始位置）
     *   3. ESC[K → カーソル位置から行末まで消去
     *
     * CSI K の意味 (EL - Erase in Line):
     *   - パラメータなし or 0: カーソル位置から行末まで消去
     *   - 1: 行頭からカーソル位置まで消去
     *   - 2: 行全体を消去
     *
     * 期待されるバッファ状態:
     *   Before: [あ][P][い][P][う][P][え][P][お][P]
     *   After:  [あ][P][い][P][ ] [ ] [ ] [ ] [ ] [ ]
     *                         ↑ 列4以降が消去される
     */
    @Test
    fun testWideChar_WithEraseToEndOfLine() {
        val (parser, buffer) = createParser()

        parser.processText("あいうえお")
        parser.processText("\u001B[1;5H")  // Move to column 5 (start of 'う')
        parser.processText("\u001B[K")     // Erase to end of line

        val screenBuffer = buffer.getBuffer()
        assertEquals('あ', screenBuffer[0][0].char, "First char should remain")
        assertEquals('い', screenBuffer[0][2].char, "Second char should remain")
        assertEquals(' ', screenBuffer[0][4].char, "Third char should be erased")
        assertEquals(' ', screenBuffer[0][6].char, "Fourth char should be erased")
    }

    /**
     * Test: 全角文字に色属性を適用
     *
     * シーケンス:
     *   1. ESC[31m → 前景色を赤に設定
     *   2. "日" を書き込み
     *   3. ESC[32m → 前景色を緑に設定
     *   4. "本" を書き込み
     *   5. ESC[0m → 属性をリセット（デフォルトの白に戻る）
     *   6. "語" を書き込み
     *
     * CSI m の意味 (SGR - Select Graphic Rendition):
     *   - 31 = 前景色: 赤
     *   - 32 = 前景色: 緑
     *   - 0 = すべての属性をリセット
     *
     * 期待されるバッファ状態:
     *   Col:  0   1   2   3   4   5
     *        [日][P] [本][P] [語][P]
     *         赤      緑      白
     */
    @Test
    fun testWideChar_WithColors() {
        val (parser, buffer) = createParser()

        parser.processText("\u001B[31m日\u001B[32m本\u001B[0m語")

        val screenBuffer = buffer.getBuffer()
        assertEquals(Color(0xFFFF5555), screenBuffer[0][0].foregroundColor, "Red")
        assertEquals(Color(0xFF50FA7B), screenBuffer[0][2].foregroundColor, "Green")
        assertEquals(Color.White, screenBuffer[0][4].foregroundColor, "Reset to white")

        // All should be wide chars
        assertTrue(screenBuffer[0][0].isWideChar)
        assertTrue(screenBuffer[0][2].isWideChar)
        assertTrue(screenBuffer[0][4].isWideChar)
    }

    /**
     * Test: 10列のターミナルで全角文字5つがぴったり収まり、次の行に折り返し
     *
     * ターミナルサイズ: 10列 × 5行
     *
     * 入力: "あいうえお" (5文字 × 2セル = 10セル)
     *
     * 期待される動作 (Delayed Wrap):
     *   - 10セルがぴったり1行に収まる
     *   - 書き込み完了直後、カーソルは **まだ行末に留まる** (Delayed Wrap)
     *   - 次の文字を書き込んだ瞬間に改行が発生する
     */
    @Test
    fun testWideChar_LineWrap() {
        // Create a narrow terminal
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // Write wide chars that should wrap exactly
        parser.processText("あいうえお")  // 10 cells, should fit exactly

        // With delayed wrap, cursor sits at the last column waiting for next char
        assertEquals(0, buffer.cursorRow, "Should NOT wrap immediately (Delayed Wrap)")
        assertEquals(9, buffer.cursorCol, "Cursor at end of line")

        // Write one more wide char - THIS triggers the wrap
        parser.processText("か")

        assertEquals(1, buffer.cursorRow, "Should wrap to next line after writing new char")
        assertEquals('か', buffer.getBuffer()[1][0].char, "Wide char on second line")
        assertEquals(2, buffer.cursorCol, "Cursor advanced after 'か'")
    }

    /**
     * Test: 最終列に全角文字を書き込もうとした時の折り返し
     *
     * ターミナルサイズ: 10列 × 5行
     *
     * シーケンス:
     *   1. "123456789" を書き込み → 列0-8を使用、カーソルは列9
     *   2. "あ" を書き込み → 全角文字は2セル必要だが、残り1セルしかない
     *
     * 期待される動作:
     *   全角文字が最終列（列9）から始まると、パディングセルを置く
     *   スペースがないため、以下の処理を行う:
     *   1. 最終列を空白のままにする
     *   2. 次の行の先頭から全角文字を書き込む
     *
     * 期待されるバッファ状態:
     *   Row 0: [1][2][3][4][5][6][7][8][9][ ]  ← 列9は空白
     *   Row 1: [あ][P]...                      ← 全角文字は次行に
     */
    @Test
    fun testWideChar_AtLastColumn_Wraps() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // Fill 9 cells with narrow chars
        parser.processText("123456789")

        // Try to write wide char at last column - should wrap
        parser.processText("あ")

        val screenBuffer = buffer.getBuffer()
        assertEquals(' ', screenBuffer[0][9].char, "Last cell should be empty")
        assertEquals('あ', screenBuffer[1][0].char, "Wide char on next line")
        assertTrue(screenBuffer[1][0].isWideChar)
    }

    /**
     * Test: byobuスタイルのステータスバー（スクロール領域外）の動作
     *
     * ターミナルサイズ: 20列 × 10行
     *
     * シーケンス:
     *   1. ESC[1;8r → スクロール領域を行1-8に設定（行9-10はステータスバー用）
     *   2. ESC[1;1H → カーソルを行1、列1に移動
     *   3. コンテンツを書き込み
     *   4. ESC[9;1H → カーソルをステータスバー行（行9）に移動
     *   5. ステータスバーの内容を書き込み
     *   6. スクロール領域内でスクロールをトリガー
     *
     * CSI r の意味 (DECSTBM - Set Top and Bottom Margins):
     *   - 1;8 = 上マージン行1、下マージン行8
     *   - スクロールはこの領域内でのみ発生
     *   - 領域外（行9-10）はスクロールの影響を受けない
     *
     * 期待される動作:
     *   スクロール領域（行1-8）内でスクロールが発生しても、
     *   ステータスバー領域（行9-10）の内容は変更されない。
     *   これはbyobuやtmuxのステータスバーの動作を再現する。
     */
    @Test
    fun testWideChar_ScrollRegion_StatusBar() {
        // Simulate byobu-like status bar behavior
        val buffer = TerminalScreenBuffer(cols = 20, rows = 10)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // Set scroll region to exclude last 2 lines (status bar area)
        parser.processText("\u001B[1;8r")  // Scroll region lines 1-8

        // Write content in scroll region
        parser.processText("\u001B[1;1H")
        parser.processText("Content area with 日本語")

        // Write to status bar area (outside scroll region)
        parser.processText("\u001B[9;1H")
        parser.processText("Status: 状態")
        parser.processText("\u001B[10;1H")
        parser.processText("Time: 時間")

        // Fill scroll region to trigger scroll
        for (i in 1..8) {
            parser.processText("\u001B[$i;1H")
            parser.processText("Line $i 行$i")
        }
        // Write more to trigger scroll
        parser.processText("\u001B[8;1H")
        for (j in 0 until 20) {
            parser.processText("X")
        }

        val screenBuffer = buffer.getBuffer()
        // Status bar should be unchanged
        assertEquals('S', screenBuffer[8][0].char, "Status line should be unchanged")
        assertEquals('T', screenBuffer[9][0].char, "Time line should be unchanged")
    }

    /**
     * Test: 代替スクリーンバッファでの全角文字処理
     *
     * シーケンス:
     *   1. "主画面" を書き込み（プライマリバッファに）
     *   2. ESC[?1049h → 代替スクリーンバッファに切り替え
     *   3. "副画面" を書き込み（代替バッファに）
     *   4. ESC[?1049l → プライマリバッファに戻る
     *
     * CSI ? 1049 h/l の意味 (DECSET/DECRST):
     *   - ?1049h = 代替スクリーンバッファを使用（カーソルを保存し、画面をクリア）
     *   - ?1049l = プライマリスクリーンバッファに戻る（カーソルを復元）
     *
     * 期待される動作:
     *   - 代替バッファへの切り替え時、プライマリの内容は保持される
     *   - 代替バッファは空の状態から開始
     *   - プライマリに戻ると、元の内容（"主画面"）が復元される
     */
    @Test
    fun testWideChar_AlternateScreen() {
        val (parser, buffer) = createParser()

        // Write to primary with wide chars
        parser.processText("主画面")

        // Switch to alternate
        parser.processText("\u001B[?1049h")
        assertTrue(buffer.isAlternateScreen)

        // Write to alternate
        parser.processText("副画面")
        assertEquals('副', buffer.getBuffer()[0][0].char)

        // Switch back
        parser.processText("\u001B[?1049l")

        // Primary content should be restored
        assertEquals('主', buffer.getBuffer()[0][0].char)
        assertTrue(buffer.getBuffer()[0][0].isWideChar)
    }

    /**
     * Test: 全角文字書き込み中のカーソル保存・復元
     *
     * シーケンス:
     *   1. "あいう" を書き込み → カーソルは列6に移動
     *   2. ESC 7 → カーソル位置を保存（列6）
     *   3. "えお" を書き込み → カーソルは列10に移動
     *   4. ESC 8 → カーソル位置を復元（列6に戻る）
     *
     * ESC 7 / ESC 8 の意味 (DECSC/DECRC):
     *   - ESC 7 = DECSC (DEC Save Cursor) - カーソル位置を保存
     *   - ESC 8 = DECRC (DEC Restore Cursor) - カーソル位置を復元
     *
     * 期待される動作:
     *   全角文字を書き込んでもカーソル位置の保存・復元は
     *   正しく動作する。カーソル位置は列単位（セル単位）で保存される。
     */
    @Test
    fun testWideChar_CursorSaveRestore() {
        val (parser, buffer) = createParser()

        parser.processText("あいう")
        parser.processText("\u001B7")  // Save cursor (at col 6)

        parser.processText("えお")
        assertEquals(10, buffer.cursorCol, "Cursor at col 10")

        parser.processText("\u001B8")  // Restore cursor
        assertEquals(6, buffer.cursorCol, "Cursor restored to col 6")
    }

    /**
     * Test: 全角文字列内での文字削除
     *
     * シーケンス:
     *   1. "あいうえお" を書き込み → [あ][P][い][P][う][P][え][P][お][P]
     *   2. ESC[1;3H → カーソルを列3に移動（0-indexedで列2、'い'の開始位置）
     *   3. ESC[2P → カーソル位置から2文字削除
     *
     * CSI Ps P の意味 (DCH - Delete Character):
     *   - Ps = 削除する文字数（デフォルト1）
     *   - カーソル位置から指定数の文字を削除
     *   - 右側の文字が左にシフトする
     *
     * 期待されるバッファ状態:
     *   Before: [あ][P][い][P][う][P][え][P][お][P]
     *   After:  [あ][P][う][P][え][P][お][P][ ] [ ]
     *                 ↑ 'い'とそのパディングが削除され、後続がシフト
     */
    @Test
    fun testWideChar_DeleteCharacters() {
        val (parser, buffer) = createParser()

        parser.processText("あいうえお")
        parser.processText("\u001B[1;3H")  // Move to column 3 (position of 'い')
        parser.processText("\u001B[2P")    // Delete 2 characters

        val screenBuffer = buffer.getBuffer()
        assertEquals('あ', screenBuffer[0][0].char, "First char should remain")
        // After deletion, 'う' should shift left
        assertEquals('う', screenBuffer[0][2].char, "'う' should shift left")
    }

    /**
     * Test: 全角文字列内での空白文字挿入
     *
     * シーケンス:
     *   1. "あいう" を書き込み → [あ][P][い][P][う][P]
     *   2. ESC[1;3H → カーソルを列3に移動（0-indexedで列2）
     *   3. ESC[2@ → カーソル位置に2つの空白文字を挿入
     *
     * CSI Ps @ の意味 (ICH - Insert Character):
     *   - Ps = 挿入する空白文字数（デフォルト1）
     *   - カーソル位置に空白を挿入
     *   - 既存の文字は右にシフトする
     *
     * 期待されるバッファ状態:
     *   Before: [あ][P][い][P][う][P]
     *   After:  [あ][P][ ] [ ][い][P][う][P]
     *                 ↑ ↑  2つの空白が挿入され、'い'以降が右シフト
     */
    @Test
    fun testWideChar_InsertCharacters() {
        val (parser, buffer) = createParser()

        parser.processText("あいう")
        parser.processText("\u001B[1;3H")  // Move to column 3
        parser.processText("\u001B[2@")    // Insert 2 blank characters

        val screenBuffer = buffer.getBuffer()
        assertEquals('あ', screenBuffer[0][0].char, "First char should remain")
        assertEquals(' ', screenBuffer[0][2].char, "Inserted blank")
        assertEquals(' ', screenBuffer[0][3].char, "Inserted blank")
        assertEquals('い', screenBuffer[0][4].char, "'い' should shift right")
    }

    /**
     * Test: 全角文字のパディング位置での文字削除
     *
     * シーケンス:
     *   1. "あいう" を書き込み → [あ][P][い][P][う][P]
     *   2. ESC[1;2H → カーソルを列2（'あ'のパディング）に移動
     *   3. ESC[1P → 1文字削除
     *
     * 期待される動作:
     *   パディング位置での操作は親文字を破壊する。
     *   'あ'はクリアされ、削除処理が実行される。
     *
     * 期待されるバッファ状態:
     *   Before: [あ][P][い][P][う][P]
     *   After:  [ ][い][P][う][P][ ]
     *            ↑ 'あ'が壊れて空白になり、'い'以降が左にシフト
     */
    @Test
    fun testWideChar_DeleteAtPadding() {
        val (parser, buffer) = createParser()

        parser.processText("あいう")
        parser.processText("\u001B[1;2H")  // Move to column 2 (padding of 'あ')
        parser.processText("\u001B[1P")    // Delete 1 character

        val screenBuffer = buffer.getBuffer()
        
        // 'あ' should be broken (cleared)
        assertEquals(' ', screenBuffer[0][0].char, "'あ' should be cleared because padding was targeted")
        
        // Remaining chars should shift left into position 1
        assertEquals('い', screenBuffer[0][1].char, "'い' should shift left to column 1")
    }

    /**
     * Test: 全角文字のパディング位置での空白挿入
     *
     * シーケンス:
     *   1. "あいう" を書き込み → [あ][P][い][P][う][P]
     *   2. ESC[1;2H → カーソルを列2（'あ'のパディング）に移動
     *   3. ESC[1@ → 1文字挿入
     *
     * 期待される動作:
     *   パディング位置での操作は親文字を破壊する。
     *   'あ'はクリアされ、空白が挿入される。
     *
     * 期待されるバッファ状態:
     *   Before: [あ][P][い][P][う][P]
     *   After:  [ ][ ][い][P][う][P]
     *            ↑  ↑ 挿入された空白
     *            'あ'が壊れて空白になる
     */
    @Test
    fun testWideChar_InsertAtPadding() {
        val (parser, buffer) = createParser()

        parser.processText("あいう")
        parser.processText("\u001B[1;2H")  // Move to column 2 (padding of 'あ')
        parser.processText("\u001B[1@")    // Insert 1 blank character

        val screenBuffer = buffer.getBuffer()

        // 'あ' should be broken (cleared)
        assertEquals(' ', screenBuffer[0][0].char, "'あ' should be cleared")
        
        // Inserted blank at col 2
        assertEquals(' ', screenBuffer[0][2].char, "Inserted blank at col 2")
        
        // 'い' should shift right
        assertEquals('い', screenBuffer[0][3].char, "'い' should shift right")
    }

    /**
     * Test: 韓国語ハングル（全角文字）の書き込み
     *
     * 入力: "한글테스트" (5文字のハングル)
     *
     * ハングル文字はUnicodeのU+AC00-U+D7AF範囲にあり、
     * 全角文字として2セル幅で表示される。
     *
     * 期待されるバッファ状態:
     *   Col:  0   1   2   3   4   5   6   7   8   9
     *        [한][P] [글][P] [테][P] [스][P] [트][P]
     */
    @Test
    fun testWideChar_Korean_Hangul() {
        val (parser, buffer) = createParser()

        parser.processText("한글테스트")  // Korean Hangul

        val screenBuffer = buffer.getBuffer()
        assertEquals('한', screenBuffer[0][0].char)
        assertTrue(screenBuffer[0][0].isWideChar)
        assertEquals('글', screenBuffer[0][2].char)
        assertEquals('테', screenBuffer[0][4].char)
        assertEquals('스', screenBuffer[0][6].char)
        assertEquals('트', screenBuffer[0][8].char)
    }

    /**
     * Test: 中国語（簡体字）の書き込み
     *
     * 入力: "中文测试" (4文字の中国語)
     *
     * CJK統合漢字はUnicodeのU+4E00-U+9FFF範囲にあり、
     * 全角文字として2セル幅で表示される。
     *
     * 期待されるバッファ状態:
     *   Col:  0   1   2   3   4   5   6   7
     *        [中][P] [文][P] [测][P] [试][P]
     */
    @Test
    fun testWideChar_Chinese() {
        val (parser, buffer) = createParser()

        parser.processText("中文测试")  // Chinese

        val screenBuffer = buffer.getBuffer()
        assertEquals('中', screenBuffer[0][0].char)
        assertTrue(screenBuffer[0][0].isWideChar)
        assertEquals('文', screenBuffer[0][2].char)
        assertEquals('测', screenBuffer[0][4].char)
        assertEquals('试', screenBuffer[0][6].char)
    }

    /**
     * Test: 全角数字の書き込み
     *
     * 入力: "１２３" (全角数字3文字)
     *
     * 全角数字（U+FF10-U+FF19）は半角数字とは異なり、
     * 2セル幅で表示される。
     *
     * 期待されるバッファ状態:
     *   Col:  0   1   2   3   4   5
     *        [１][P] [２][P] [３][P]
     */
    @Test
    fun testWideChar_FullWidthNumbers() {
        val (parser, buffer) = createParser()

        parser.processText("１２３")  // Full-width numbers

        val screenBuffer = buffer.getBuffer()
        assertEquals('１', screenBuffer[0][0].char)
        assertTrue(screenBuffer[0][0].isWideChar)
        assertEquals('２', screenBuffer[0][2].char)
        assertEquals('３', screenBuffer[0][4].char)
    }

    // ========== スクロール領域対応テスト (ECMA-48) ==========
    // LF, IND, NEL, RI がスクロール領域を正しく処理することを検証する。
    // byobu, tmux, screen などのステータスバーを使用するアプリケーションに必須。

    /**
     * テスト: Line Feed (\n) が scrollBottom でスクロールを発生させる
     *
     * 操作:
     *   1. スクロール領域を行3-7に設定 (ESC[3;7r)
     *   2. 行3-7にコンテンツを書き込み
     *   3. 行8-9にステータスバーを書き込み
     *   4. カーソルを行7に移動して \n を送信
     *
     * 期待されるバッファ状態 (スクロール後):
     *   Row 1: [         ] ← スクロール領域外（変化なし）
     *   Row 2: [         ] ← スクロール領域外（変化なし）
     *   Row 3: [Line 4   ] ← 元のLine 3が消え、Line 4が上に移動
     *   Row 4: [Line 5   ]
     *   Row 5: [Line 6   ]
     *   Row 6: [Line 7   ]
     *   Row 7: [         ] ← 新しい空行、カーソルはここ
     *   Row 8: [Status A ] ← ステータスバー（変化なし）
     *   Row 9: [Status B ] ← ステータスバー（変化なし）
     *
     * スクロール領域内のみがスクロールし、ステータスバーは固定される。
     * これはbyobuやtmuxのステータスバー動作に必須の機能。
     */
    @Test
    fun testLineFeed_AtScrollBottom_ShouldScroll() {
        val buffer = TerminalScreenBuffer(cols = 20, rows = 10)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // Set scroll region to rows 3-7 (1-indexed)
        parser.processText("\u001B[3;7r")

        // Write content in scroll region
        parser.processText("\u001B[3;1HLine 3")
        parser.processText("\u001B[4;1HLine 4")
        parser.processText("\u001B[5;1HLine 5")
        parser.processText("\u001B[6;1HLine 6")
        parser.processText("\u001B[7;1HLine 7")

        // Write status bar content (outside scroll region)
        parser.processText("\u001B[8;1HStatus A")
        parser.processText("\u001B[9;1HStatus B")

        // Position cursor at scrollBottom (row 7) and send LF
        parser.processText("\u001B[7;1H")
        parser.processText("\n")

        val screenBuffer = buffer.getBuffer()

        // Cursor should stay at scrollBottom (row 6, 0-indexed)
        assertEquals(6, buffer.cursorRow, "Cursor should stay at scrollBottom after LF")

        // Scroll region should have scrolled - "Line 3" should be gone
        assertEquals('L', screenBuffer[2][0].char, "Row 3 should now have Line 4")
        assertEquals('4', screenBuffer[2][5].char, "Row 3 should have '4' from Line 4")

        // Status bar should be unchanged
        assertEquals('S', screenBuffer[7][0].char, "Status bar row 8 should be unchanged")
        assertEquals('S', screenBuffer[8][0].char, "Status bar row 9 should be unchanged")
    }

    /**
     * テスト: Line Feed (\n) が scrollBottom 以外ではスクロールしない
     *
     * 操作:
     *   1. スクロール領域を行3-7に設定 (ESC[3;7r)
     *   2. 行3-4にコンテンツを書き込み
     *   3. カーソルを行5（スクロール領域の中間）に移動
     *   4. \n を送信
     *
     * 期待される動作:
     *   Row 3: [Line 3   ] ← 変化なし
     *   Row 4: [Line 4   ] ← 変化なし
     *   Row 5: [         ] ← 元のカーソル位置
     *   Row 6: [         ] ← LF後のカーソル位置（単に下に移動）
     *
     * scrollBottom に達していない場合、スクロールは発生せず
     * カーソルが1行下に移動するのみ。
     */
    @Test
    fun testLineFeed_NotAtScrollBottom_ShouldOnlyMoveCursor() {
        val buffer = TerminalScreenBuffer(cols = 20, rows = 10)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // Set scroll region to rows 3-7
        parser.processText("\u001B[3;7r")

        // Write content
        parser.processText("\u001B[3;1HLine 3")
        parser.processText("\u001B[4;1HLine 4")

        // Position cursor at row 5 (middle of scroll region) and send LF
        parser.processText("\u001B[5;1H")
        parser.processText("\n")

        // Cursor should move down to row 6
        assertEquals(5, buffer.cursorRow, "Cursor should move down within scroll region")

        // Content should NOT scroll
        val screenBuffer = buffer.getBuffer()
        assertEquals('L', screenBuffer[2][0].char, "Line 3 should still be at row 3")
        assertEquals('3', screenBuffer[2][5].char)
    }

    /**
     * テスト: ESC D (Index/IND) が scrollBottom でスクロールを発生させる
     *
     * ESC D は ECMA-48 で定義された Index コマンド。
     * Line Feed と同様の動作を行う。
     *
     * 操作:
     *   1. スクロール領域を行3-7に設定 (ESC[3;7r)
     *   2. 行3, 7にコンテンツを書き込み
     *   3. 行8にステータスバーを書き込み
     *   4. カーソルを行7に移動して ESC D を送信
     *
     * 期待される動作:
     *   - スクロール領域がスクロール
     *   - カーソルは scrollBottom に留まる
     *   - ステータスバーは影響を受けない
     *
     * ESC D は \n と同等だが、一部のアプリケーションは明示的に
     * ESC D を使用するため、個別にテストする。
     */
    @Test
    fun testIndex_IND_AtScrollBottom_ShouldScroll() {
        val buffer = TerminalScreenBuffer(cols = 20, rows = 10)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // Set scroll region to rows 3-7
        parser.processText("\u001B[3;7r")

        // Write content
        parser.processText("\u001B[3;1HLine 3")
        parser.processText("\u001B[7;1HLine 7")

        // Write status bar
        parser.processText("\u001B[8;1HStatus")

        // Position cursor at scrollBottom and send IND (ESC D)
        parser.processText("\u001B[7;1H")
        parser.processText("\u001BD")  // ESC D = Index

        val screenBuffer = buffer.getBuffer()

        // Cursor should stay at scrollBottom
        assertEquals(6, buffer.cursorRow, "Cursor should stay at scrollBottom after IND")

        // Status bar should be unchanged
        assertEquals('S', screenBuffer[7][0].char, "Status bar should be unchanged")
    }

    /**
     * テスト: ESC D (Index/IND) が scrollBottom 以外で移動のみ行う
     *
     * 操作:
     *   1. スクロール領域を行3-7に設定
     *   2. カーソルを行5に移動して ESC D を送信
     *
     * 期待される動作:
     *   - カーソルが行5から行6に移動
     *   - スクロールは発生しない
     */
    @Test
    fun testIndex_IND_NotAtScrollBottom_ShouldOnlyMoveCursor() {
        val buffer = TerminalScreenBuffer(cols = 20, rows = 10)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // Set scroll region to rows 3-7
        parser.processText("\u001B[3;7r")

        // Position cursor at row 5 and send IND
        parser.processText("\u001B[5;1H")
        parser.processText("\u001BD")

        // Cursor should move down
        assertEquals(5, buffer.cursorRow, "Cursor should move down after IND")
    }

    /**
     * テスト: ESC E (Next Line/NEL) が scrollBottom で列0に移動しスクロール
     *
     * ESC E は ECMA-48 で定義された Next Line コマンド。
     * CR + LF と同等の動作を行う。
     *
     * 操作:
     *   1. スクロール領域を行3-7に設定
     *   2. 行7の列10にカーソルを移動
     *   3. ESC E を送信
     *
     * 期待される動作:
     *   - カーソルが列0に移動（CR相当）
     *   - スクロール領域がスクロール（LF相当）
     *   - カーソルは行7（scrollBottom）に留まる
     *   - ステータスバーは影響を受けない
     *
     * NEL は一部のアプリケーションで改行処理に使用される。
     * CR と LF を別々に送る代わりに NEL を使用することで
     * 1バイト節約できる。
     */
    @Test
    fun testNextLine_NEL_AtScrollBottom_ShouldScrollAndReturnToColumn0() {
        val buffer = TerminalScreenBuffer(cols = 20, rows = 10)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // Set scroll region to rows 3-7
        parser.processText("\u001B[3;7r")

        // Write content
        parser.processText("\u001B[3;1HLine 3")
        parser.processText("\u001B[7;10HMiddle")  // Cursor at column 10

        // Write status bar
        parser.processText("\u001B[8;1HStatus")

        // Position cursor at scrollBottom, column 10, and send NEL
        parser.processText("\u001B[7;10H")
        parser.processText("\u001BE")  // ESC E = Next Line

        val screenBuffer = buffer.getBuffer()

        // Cursor should be at column 0
        assertEquals(0, buffer.cursorCol, "Cursor should be at column 0 after NEL")

        // Cursor should stay at scrollBottom
        assertEquals(6, buffer.cursorRow, "Cursor should stay at scrollBottom after NEL")

        // Status bar should be unchanged
        assertEquals('S', screenBuffer[7][0].char, "Status bar should be unchanged")
    }

    /**
     * テスト: ESC E (Next Line/NEL) が scrollBottom 以外で次の行の列0に移動
     *
     * 操作:
     *   1. スクロール領域を行3-7に設定
     *   2. カーソルを行5の列10に移動
     *   3. ESC E を送信
     *
     * 期待される動作:
     *   - カーソルが列0に移動（CR相当）
     *   - カーソルが行6に移動（LF相当）
     *   - スクロールは発生しない
     */
    @Test
    fun testNextLine_NEL_NotAtScrollBottom_ShouldMoveToNextLineColumn0() {
        val buffer = TerminalScreenBuffer(cols = 20, rows = 10)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // Set scroll region to rows 3-7
        parser.processText("\u001B[3;7r")

        // Position cursor at row 5, column 10
        parser.processText("\u001B[5;10H")
        parser.processText("\u001BE")  // ESC E = Next Line

        // Cursor should be at next row, column 0
        assertEquals(5, buffer.cursorRow, "Cursor should move to next row")
        assertEquals(0, buffer.cursorCol, "Cursor should be at column 0")
    }

    /**
     * テスト: ESC M (Reverse Index/RI) が scrollTop で逆スクロールを発生させる
     *
     * ESC M は ECMA-48 で定義された Reverse Index コマンド。
     * カーソルを上に移動し、scrollTop にいる場合は逆スクロールを発生させる。
     *
     * 操作:
     *   1. スクロール領域を行3-7に設定
     *   2. 行3-4, 7にコンテンツを書き込み
     *   3. 行1にヘッダー、行8にステータスバーを書き込み
     *   4. カーソルを行3（scrollTop）に移動して ESC M を送信
     *
     * 期待されるバッファ状態 (逆スクロール後):
     *   Row 1: [Top      ] ← スクロール領域外（変化なし）
     *   Row 2: [         ] ← スクロール領域外（変化なし）
     *   Row 3: [         ] ← 新しい空行が挿入、カーソルはここ
     *   Row 4: [Line 3   ] ← 元のLine 3が下に移動
     *   Row 5: [Line 4   ] ← 元のLine 4が下に移動
     *   Row 6: [         ]
     *   Row 7: [         ] ← 元のLine 7は押し出されて消える
     *   Row 8: [Status   ] ← ステータスバー（変化なし）
     *
     * 逆スクロールはvimなどで上方向にスクロールする際に使用される。
     */
    @Test
    fun testReverseIndex_RI_AtScrollTop_ShouldReverseScroll() {
        val buffer = TerminalScreenBuffer(cols = 20, rows = 10)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // Set scroll region to rows 3-7
        parser.processText("\u001B[3;7r")

        // Write content
        parser.processText("\u001B[3;1HLine 3")
        parser.processText("\u001B[4;1HLine 4")
        parser.processText("\u001B[7;1HLine 7")

        // Write content outside scroll region
        parser.processText("\u001B[1;1HTop")
        parser.processText("\u001B[8;1HStatus")

        // Position cursor at scrollTop (row 3) and send RI
        parser.processText("\u001B[3;1H")
        parser.processText("\u001BM")  // ESC M = Reverse Index

        val screenBuffer = buffer.getBuffer()

        // Cursor should stay at scrollTop
        assertEquals(2, buffer.cursorRow, "Cursor should stay at scrollTop after RI")

        // Row 3 should now be blank (new line inserted)
        assertEquals(' ', screenBuffer[2][0].char, "Row 3 should be blank after reverse scroll")

        // Old Line 3 should now be at row 4
        assertEquals('L', screenBuffer[3][0].char, "Old Line 3 should shift to row 4")
        assertEquals('3', screenBuffer[3][5].char)

        // Content outside scroll region should be unchanged
        assertEquals('T', screenBuffer[0][0].char, "Top row should be unchanged")
        assertEquals('S', screenBuffer[7][0].char, "Status bar should be unchanged")
    }

    /**
     * テスト: ESC M (Reverse Index/RI) が scrollTop 以外で上移動のみ行う
     *
     * 操作:
     *   1. スクロール領域を行3-7に設定
     *   2. 行3にコンテンツを書き込み
     *   3. カーソルを行5に移動して ESC M を送信
     *
     * 期待される動作:
     *   - カーソルが行5から行4に移動
     *   - スクロールは発生しない
     *   - コンテンツは変化なし
     */
    @Test
    fun testReverseIndex_RI_NotAtScrollTop_ShouldOnlyMoveCursorUp() {
        val buffer = TerminalScreenBuffer(cols = 20, rows = 10)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // Set scroll region to rows 3-7
        parser.processText("\u001B[3;7r")

        // Write content
        parser.processText("\u001B[3;1HLine 3")

        // Position cursor at row 5 and send RI
        parser.processText("\u001B[5;1H")
        parser.processText("\u001BM")

        // Cursor should move up
        assertEquals(3, buffer.cursorRow, "Cursor should move up after RI")

        // Content should NOT scroll
        val screenBuffer = buffer.getBuffer()
        assertEquals('L', screenBuffer[2][0].char, "Line 3 should still be at row 3")
    }

    /**
     * テスト: byobu + logcat の実際のシナリオ
     *
     * 報告された実際の問題を再現するテスト:
     * - byobu がステータスバー（行9-10）を除外するスクロール領域（行1-8）を設定
     * - logcat が継続的に \n で出力
     * - ステータスバーは下部に固定されたまま維持されるべき
     *
     * 操作:
     *   1. スクロール領域を行1-8に設定 (ESC[1;8r)
     *   2. 行9-10にbyobuステータスバーを書き込み
     *   3. 行1-8にログ行を書き込み（スクロール領域を埋める）
     *   4. 新しいログ3行を \n 付きで書き込み（スクロールを発生させる）
     *
     * 期待されるバッファ状態:
     *   Row 1-5: [古いログ行がスクロールして上に移動]
     *   Row 6:   [New log A]
     *   Row 7:   [New log B]
     *   Row 8:   [New log C] ← カーソルはここ
     *   Row 9:   [[byobu] Session A | 12:34] ← 変化なし
     *   Row 10:  [[status] CPU: 50%        ] ← 変化なし
     *
     * このテストが失敗する場合:
     * - ステータスバーが上書きされる（カーソルがscrollBottomを超える）
     * - スクロールが発生せずログが停滞する
     */
    @Test
    fun testByobuScenario_LogcatWithStatusBar() {
        val buffer = TerminalScreenBuffer(cols = 40, rows = 10)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // byobu sets scroll region (rows 1-8, status bar at rows 9-10)
        parser.processText("\u001B[1;8r")

        // Write initial status bar
        parser.processText("\u001B[9;1H[byobu] Session A | 12:34")
        parser.processText("\u001B[10;1H[status] CPU: 50%")

        // Simulate logcat output filling the scroll region
        for (i in 1..8) {
            parser.processText("\u001B[$i;1HLog line $i")
        }

        // Simulate new logcat lines (should trigger scroll)
        parser.processText("\u001B[8;1H")  // Move to bottom of scroll region
        parser.processText("New log A\n")
        parser.processText("New log B\n")
        parser.processText("New log C\n")

        val screenBuffer = buffer.getBuffer()

        // Status bar should be completely unchanged
        assertEquals('[', screenBuffer[8][0].char, "Status bar row 9 should start with '['")
        assertEquals('b', screenBuffer[8][1].char, "Status bar should have 'byobu'")
        assertEquals('[', screenBuffer[9][0].char, "Status bar row 10 should start with '['")
        assertEquals('s', screenBuffer[9][1].char, "Status bar should have 'status'")

        // Cursor should be at scrollBottom (row 8, 0-indexed: 7)
        assertEquals(7, buffer.cursorRow, "Cursor should stay within scroll region")
    }

    /**
     * テスト: スクロール領域なし（全画面）で LF が画面全体をスクロール
     *
     * スクロール領域が設定されていない場合、デフォルトで画面全体が
     * スクロール領域となる。
     *
     * 操作:
     *   1. スクロール領域は設定しない（デフォルト: 全画面）
     *   2. 5行すべてにコンテンツを書き込み
     *   3. カーソルを最終行に移動して \n を送信
     *
     * 期待されるバッファ状態 (スクロール後):
     *   Row 1: [Line 2   ] ← 元のLine 1が消え、Line 2が上に移動
     *   Row 2: [Line 3   ]
     *   Row 3: [Line 4   ]
     *   Row 4: [Line 5   ]
     *   Row 5: [         ] ← 新しい空行、カーソルはここ
     *
     * これは通常のターミナル動作（スクロール領域なし）の基本テスト。
     */
    @Test
    fun testLineFeed_FullScreen_ShouldScrollEntireScreen() {
        val buffer = TerminalScreenBuffer(cols = 20, rows = 5)
        val parser = TerminalProcessor(AnsiEscapeParser(defaultScrollBottom = buffer.rows - 1), TerminalCommandExecutor(buffer))

        // No scroll region set (full screen)

        // Fill all lines
        parser.processText("\u001B[1;1HLine 1")
        parser.processText("\u001B[2;1HLine 2")
        parser.processText("\u001B[3;1HLine 3")
        parser.processText("\u001B[4;1HLine 4")
        parser.processText("\u001B[5;1HLine 5")

        // Move to last row and send LF
        parser.processText("\u001B[5;1H")
        parser.processText("\n")

        val screenBuffer = buffer.getBuffer()

        // Cursor should stay at last row
        assertEquals(4, buffer.cursorRow, "Cursor should stay at last row")

        // Line 1 should be gone, Line 2 should now be at row 1
        assertEquals('L', screenBuffer[0][0].char, "Row 1 should have Line 2")
        assertEquals('2', screenBuffer[0][5].char, "Row 1 should have '2' from Line 2")
    }
}
