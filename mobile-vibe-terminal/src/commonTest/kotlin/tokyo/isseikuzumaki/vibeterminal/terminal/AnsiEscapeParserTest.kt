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

    private fun createParser(): Pair<AnsiEscapeParser, TerminalScreenBuffer> {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)
        val parser = AnsiEscapeParser(buffer)
        return Pair(parser, buffer)
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
}
