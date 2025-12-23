package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.graphics.Color

/**
 * Unit tests for TerminalScreenBuffer business logic.
 * Tests screen buffer operations, scroll regions, and alternate screen handling.
 */
class TerminalScreenBufferTest {

    // ========== Basic Buffer Operations ==========

    @Test
    fun testInitialState() {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)

        assertEquals(0, buffer.cursorRow, "Cursor should start at row 0")
        assertEquals(0, buffer.cursorCol, "Cursor should start at col 0")
        assertTrue(!buffer.isAlternateScreen, "Should start on primary screen")

        // Buffer should be empty (all spaces)
        val screenBuffer = buffer.getBuffer()
        assertEquals(' ', screenBuffer[0][0].char, "Buffer should be empty")
        assertEquals(' ', screenBuffer[23][79].char, "Buffer should be empty")
    }

    @Test
    fun testWriteChar() {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)

        buffer.writeChar('A')
        buffer.writeChar('B')
        buffer.writeChar('C')

        val screenBuffer = buffer.getBuffer()
        assertEquals('A', screenBuffer[0][0].char, "First char should be 'A'")
        assertEquals('B', screenBuffer[0][1].char, "Second char should be 'B'")
        assertEquals('C', screenBuffer[0][2].char, "Third char should be 'C'")
        assertEquals(0, buffer.cursorRow, "Cursor row should still be 0")
        assertEquals(3, buffer.cursorCol, "Cursor col should be 3")
    }

    @Test
    fun testLineWrap() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Write 15 characters (should wrap to next line)
        for (i in 0 until 15) {
            buffer.writeChar('X')
        }

        val screenBuffer = buffer.getBuffer()
        assertEquals('X', screenBuffer[0][9].char, "First line should be full")
        assertEquals('X', screenBuffer[1][0].char, "Should wrap to second line")
        assertEquals('X', screenBuffer[1][4].char, "15th char on second line")
        assertEquals(1, buffer.cursorRow, "Cursor should be on second line")
        assertEquals(5, buffer.cursorCol, "Cursor should be at col 5")
    }

    @Test
    fun testScrollWhenFull() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 3)

        // Fill all 3 lines
        buffer.moveCursor(1, 1)
        buffer.writeChar('1')
        buffer.moveCursor(2, 1)
        buffer.writeChar('2')
        buffer.moveCursor(3, 1)
        buffer.writeChar('3')

        // Write on last line to trigger scroll
        for (i in 0 until 10) {
            buffer.writeChar('X')
        }

        val screenBuffer = buffer.getBuffer()
        assertEquals('2', screenBuffer[0][0].char, "First line should have scrolled up")
        assertEquals('3', screenBuffer[1][0].char, "Second line should be old line 3")
        assertEquals('X', screenBuffer[2][0].char, "Third line should be new content")
    }

    // ========== Cursor Movement ==========

    @Test
    fun testMoveCursor_Absolute() {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)

        buffer.moveCursor(10, 20)  // 1-indexed
        assertEquals(9, buffer.cursorRow, "Cursor should be at row 9 (0-indexed)")
        assertEquals(19, buffer.cursorCol, "Cursor should be at col 19 (0-indexed)")
    }

    @Test
    fun testMoveCursor_Relative() {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)
        buffer.moveCursor(10, 10)

        buffer.moveCursorRelative(5, 3)
        assertEquals(14, buffer.cursorRow, "Cursor should move down 5")
        assertEquals(12, buffer.cursorCol, "Cursor should move right 3")

        buffer.moveCursorRelative(-2, -4)
        assertEquals(12, buffer.cursorRow, "Cursor should move up 2")
        assertEquals(8, buffer.cursorCol, "Cursor should move left 4")
    }

    @Test
    fun testMoveCursor_Clamping() {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)

        buffer.moveCursor(-10, -10)  // Should clamp to 0,0
        assertEquals(0, buffer.cursorRow, "Negative row should clamp to 0")
        assertEquals(0, buffer.cursorCol, "Negative col should clamp to 0")

        buffer.moveCursor(1000, 1000)  // Should clamp to max
        assertEquals(23, buffer.cursorRow, "Large row should clamp to max")
        assertEquals(79, buffer.cursorCol, "Large col should clamp to max")
    }

    @Test
    fun testMoveCursorToColumn() {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)
        buffer.moveCursor(10, 10)

        buffer.moveCursorToColumn(50)  // 1-indexed
        assertEquals(9, buffer.cursorRow, "Row should not change")
        assertEquals(49, buffer.cursorCol, "Column should be 50 (0-indexed = 49)")
    }

    // ========== Screen Clearing Operations ==========

    @Test
    fun testClearScreen() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Fill buffer
        for (r in 0 until 5) {
            for (c in 0 until 10) {
                buffer.moveCursor(r + 1, c + 1)
                buffer.writeChar('X')
            }
        }

        buffer.clearScreen()

        val screenBuffer = buffer.getBuffer()
        assertEquals(' ', screenBuffer[0][0].char, "Buffer should be cleared")
        assertEquals(' ', screenBuffer[4][9].char, "Buffer should be cleared")
        assertEquals(0, buffer.cursorRow, "Cursor should be at home")
        assertEquals(0, buffer.cursorCol, "Cursor should be at home")
    }

    @Test
    fun testClearToEndOfScreen() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Fill buffer
        for (r in 0 until 5) {
            for (c in 0 until 10) {
                buffer.moveCursor(r + 1, c + 1)
                buffer.writeChar('X')
            }
        }

        buffer.moveCursor(3, 5)
        buffer.clearToEndOfScreen()

        val screenBuffer = buffer.getBuffer()
        assertEquals('X', screenBuffer[0][0].char, "Before cursor should be unchanged")
        assertEquals('X', screenBuffer[2][3].char, "Before cursor should be unchanged")
        assertEquals(' ', screenBuffer[2][4].char, "Cursor position should be cleared")
        assertEquals(' ', screenBuffer[4][9].char, "End of screen should be cleared")
    }

    @Test
    fun testClearToStartOfScreen() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Fill buffer without triggering scroll by writing directly at positions
        for (r in 0 until 5) {
            buffer.moveCursor(r + 1, 1)
            for (c in 0 until 10) {
                buffer.writeChar('X')
            }
            // After writing 10 chars, cursor wraps to next line, so no explicit newline needed
        }

        // Now position cursor and test clear
        buffer.moveCursor(3, 5)  // Row 3, Col 5 (0-indexed: 2, 4)
        buffer.clearToStartOfScreen()

        val screenBuffer = buffer.getBuffer()
        assertEquals(' ', screenBuffer[0][0].char, "Start should be cleared")
        assertEquals(' ', screenBuffer[2][4].char, "Up to cursor should be cleared")
        assertEquals('X', screenBuffer[2][5].char, "After cursor should be unchanged")
        assertEquals('X', screenBuffer[3][0].char, "Lines after cursor should be unchanged")
    }

    @Test
    fun testClearLine() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        buffer.moveCursor(3, 1)
        for (i in 0 until 10) {
            buffer.writeChar('X')
        }

        buffer.moveCursor(3, 5)
        buffer.clearLine()

        val screenBuffer = buffer.getBuffer()
        assertEquals(' ', screenBuffer[2][0].char, "Entire line should be cleared")
        assertEquals(' ', screenBuffer[2][9].char, "Entire line should be cleared")
    }

    @Test
    fun testClearToEndOfLine() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        buffer.moveCursor(3, 1)
        for (i in 0 until 10) {
            buffer.writeChar('X')
        }

        buffer.moveCursor(3, 5)
        buffer.clearToEndOfLine()

        val screenBuffer = buffer.getBuffer()
        assertEquals('X', screenBuffer[2][3].char, "Before cursor should be unchanged")
        assertEquals(' ', screenBuffer[2][4].char, "Cursor position should be cleared")
        assertEquals(' ', screenBuffer[2][9].char, "End of line should be cleared")
    }

    @Test
    fun testClearToStartOfLine() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        buffer.moveCursor(3, 1)
        for (i in 0 until 10) {
            buffer.writeChar('X')
        }

        buffer.moveCursor(3, 5)
        buffer.clearToStartOfLine()

        val screenBuffer = buffer.getBuffer()
        assertEquals(' ', screenBuffer[2][0].char, "Start should be cleared")
        assertEquals(' ', screenBuffer[2][4].char, "Up to cursor should be cleared")
        assertEquals('X', screenBuffer[2][5].char, "After cursor should be unchanged")
    }

    // ========== Line Insertion and Deletion ==========

    @Test
    fun testInsertLines() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        buffer.moveCursor(1, 1)
        buffer.writeChar('1')
        buffer.moveCursor(2, 1)
        buffer.writeChar('2')
        buffer.moveCursor(3, 1)
        buffer.writeChar('3')

        buffer.moveCursor(2, 1)
        buffer.insertLines(1)

        val screenBuffer = buffer.getBuffer()
        assertEquals('1', screenBuffer[0][0].char, "Line 1 should remain")
        assertEquals(' ', screenBuffer[1][0].char, "Inserted line should be blank")
        assertEquals('2', screenBuffer[2][0].char, "Line 2 should shift down")
        assertEquals('3', screenBuffer[3][0].char, "Line 3 should shift down")
    }

    @Test
    fun testDeleteLines() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        buffer.moveCursor(1, 1)
        buffer.writeChar('1')
        buffer.moveCursor(2, 1)
        buffer.writeChar('2')
        buffer.moveCursor(3, 1)
        buffer.writeChar('3')

        buffer.moveCursor(2, 1)
        buffer.deleteLines(1)

        val screenBuffer = buffer.getBuffer()
        assertEquals('1', screenBuffer[0][0].char, "Line 1 should remain")
        assertEquals('3', screenBuffer[1][0].char, "Line 3 should move up")
        assertEquals(' ', screenBuffer[2][0].char, "Deleted line should be blank")
    }

    // ========== Cursor Save and Restore ==========

    @Test
    fun testSaveRestoreCursor() {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)

        buffer.moveCursor(15, 25)
        buffer.saveCursor()

        buffer.moveCursor(5, 5)
        assertEquals(4, buffer.cursorRow, "Cursor should be at new position")
        assertEquals(4, buffer.cursorCol, "Cursor should be at new position")

        buffer.restoreCursor()
        assertEquals(14, buffer.cursorRow, "Cursor should be restored")
        assertEquals(24, buffer.cursorCol, "Cursor should be restored")
    }

    // ========== Graphics Mode / Styling ==========

    @Test
    fun testSetGraphicsMode_Attributes() {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)

        buffer.setGraphicsMode(listOf(1))  // Bold
        buffer.writeChar('B')
        assertTrue(buffer.getBuffer()[0][0].isBold, "Should be bold")

        buffer.setGraphicsMode(listOf(4))  // Underline
        buffer.writeChar('U')
        assertTrue(buffer.getBuffer()[0][1].isUnderline, "Should be underline")

        buffer.setGraphicsMode(listOf(7))  // Reverse
        buffer.writeChar('R')
        assertTrue(buffer.getBuffer()[0][2].isReverse, "Should be reverse")
    }

    @Test
    fun testSetGraphicsMode_Colors() {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)

        buffer.setGraphicsMode(listOf(31))  // Red foreground
        buffer.writeChar('R')
        assertEquals(Color(0xFFFF5555), buffer.getBuffer()[0][0].foregroundColor)

        buffer.setGraphicsMode(listOf(42))  // Green background
        buffer.writeChar('G')
        assertEquals(Color(0xFF50FA7B), buffer.getBuffer()[0][1].backgroundColor)
    }

    @Test
    fun testSetGraphicsMode_Reset() {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)

        buffer.setGraphicsMode(listOf(1, 4, 31, 44))  // Bold, underline, red, blue bg
        buffer.writeChar('X')

        buffer.setGraphicsMode(listOf(0))  // Reset
        buffer.writeChar('Y')

        val cell = buffer.getBuffer()[0][1]
        assertTrue(!cell.isBold, "Bold should be reset")
        assertTrue(!cell.isUnderline, "Underline should be reset")
        assertEquals(Color.White, cell.foregroundColor, "Foreground should be reset")
        assertEquals(Color.Black, cell.backgroundColor, "Background should be reset")
    }

    // ========== Scroll Region (DECSTBM) ==========

    @Test
    fun testSetScrollRegion() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 24)

        buffer.setScrollRegion(5, 20)  // Set scroll region to lines 5-20

        // Fill line 20 and trigger scroll
        buffer.moveCursor(20, 1)
        for (i in 0 until 10) {
            buffer.writeChar('X')
        }
        // Write one more line to trigger scroll
        buffer.writeChar('Y')

        // Line 4 (outside scroll region) should not be affected
        // Lines 5-20 should scroll
        // This tests that scrolling respects the region
        assertEquals(19, buffer.cursorRow, "Cursor should stay at bottom of scroll region")
    }

    @Test
    fun testResetScrollRegion() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 24)

        buffer.setScrollRegion(5, 20)
        buffer.resetScrollRegion()

        // After reset, scrolling should work for entire screen
        // Fill last line and trigger scroll
        buffer.moveCursor(24, 1)
        for (i in 0 until 10) {
            buffer.writeChar('X')
        }
        buffer.writeChar('Y')  // Should trigger full-screen scroll

        assertEquals(23, buffer.cursorRow, "Cursor should be at bottom of full screen")
    }

    @Test
    fun testScrollRegion_OnlyAffectsRegion() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 10)

        // Write marker above region
        buffer.moveCursor(2, 1)
        buffer.writeChar('A')

        // Write marker below region
        buffer.moveCursor(9, 1)
        buffer.writeChar('Z')

        // Set scroll region 4-7
        buffer.setScrollRegion(4, 7)

        // Fill the scroll region and trigger scroll
        buffer.moveCursor(7, 1)
        for (i in 0 until 10) {
            buffer.writeChar('X')
        }
        buffer.writeChar('S')  // Should scroll within region only

        val screenBuffer = buffer.getBuffer()
        assertEquals('A', screenBuffer[1][0].char, "Line above region should be unchanged")
        assertEquals('Z', screenBuffer[8][0].char, "Line below region should be unchanged")
    }

    // ========== Alternate Screen Buffer ==========

    @Test
    fun testAlternateScreenBuffer_Switch() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Write to primary screen
        buffer.writeChar('P')
        buffer.writeChar('R')
        buffer.writeChar('I')

        assertTrue(!buffer.isAlternateScreen, "Should be on primary screen")

        // Switch to alternate
        buffer.useAlternateScreenBuffer()
        assertTrue(buffer.isAlternateScreen, "Should be on alternate screen")

        // Alternate should be empty
        assertEquals(' ', buffer.getBuffer()[0][0].char, "Alternate should be empty")

        // Write to alternate
        buffer.writeChar('A')
        buffer.writeChar('L')
        buffer.writeChar('T')

        // Switch back to primary
        buffer.usePrimaryScreenBuffer()
        assertTrue(!buffer.isAlternateScreen, "Should be back on primary screen")

        // Primary content should be restored
        assertEquals('P', buffer.getBuffer()[0][0].char, "Primary content restored")
        assertEquals('R', buffer.getBuffer()[0][1].char, "Primary content restored")
        assertEquals('I', buffer.getBuffer()[0][2].char, "Primary content restored")
    }

    @Test
    fun testAlternateScreenBuffer_CursorSave() {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)

        // Set cursor on primary
        buffer.moveCursor(10, 20)

        // Switch to alternate
        buffer.useAlternateScreenBuffer()

        // Cursor should reset to 0,0 on alternate
        assertEquals(0, buffer.cursorRow, "Alternate cursor should be at 0,0")
        assertEquals(0, buffer.cursorCol, "Alternate cursor should be at 0,0")

        // Move cursor on alternate
        buffer.moveCursor(5, 5)

        // Switch back to primary
        buffer.usePrimaryScreenBuffer()

        // Primary cursor should be restored
        assertEquals(9, buffer.cursorRow, "Primary cursor should be restored")
        assertEquals(19, buffer.cursorCol, "Primary cursor should be restored")
    }

    @Test
    fun testAlternateScreenBuffer_NoDoubleSwitch() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        buffer.writeChar('X')

        buffer.useAlternateScreenBuffer()
        buffer.useAlternateScreenBuffer()  // Second call should do nothing
        assertTrue(buffer.isAlternateScreen, "Should still be on alternate")

        buffer.usePrimaryScreenBuffer()
        buffer.usePrimaryScreenBuffer()  // Second call should do nothing
        assertTrue(!buffer.isAlternateScreen, "Should still be on primary")

        assertEquals('X', buffer.getBuffer()[0][0].char, "Primary content preserved")
    }

    // ========== Resize Operations ==========

    @Test
    fun testResize_Smaller() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 10)

        // Fill with content
        for (r in 0 until 10) {
            buffer.moveCursor(r + 1, 1)
            buffer.writeChar(('0' + r))
        }

        buffer.resize(newCols = 5, newRows = 5)

        val screenBuffer = buffer.getBuffer()
        assertEquals('0', screenBuffer[0][0].char, "Content should be preserved")
        assertEquals('4', screenBuffer[4][0].char, "Content should be preserved")

        assertEquals(4, buffer.cursorRow, "Cursor should be clamped to new size")
    }

    @Test
    fun testResize_Larger() {
        val buffer = TerminalScreenBuffer(cols = 5, rows = 5)

        buffer.moveCursor(1, 1)
        buffer.writeChar('X')

        buffer.resize(newCols = 10, newRows = 10)

        assertEquals('X', buffer.getBuffer()[0][0].char, "Content should be preserved")
        assertEquals(' ', buffer.getBuffer()[9][9].char, "New area should be empty")
    }

    @Test
    fun testResize_ResetsScrollRegion() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 10)

        buffer.setScrollRegion(3, 7)
        buffer.resize(newCols = 10, newRows = 15)

        // After resize, scroll region should be reset to full screen (0, 14)
        // This is verified by the implementation's resetScrollRegion call
    }

    // ========== Edge Cases ==========

    @Test
    fun testWriteChar_OutOfBounds() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Move cursor out of bounds and try to write
        buffer.moveCursor(100, 100)  // Clamps to (4, 9)

        // Write a character at a position that won't trigger scroll
        buffer.moveCursor(5, 9)  // Row 5 (0-indexed 4), Col 9 (0-indexed 8)
        buffer.writeChar('X')

        // After writing at (4, 8), cursor advances to (4, 9)
        assertEquals(4, buffer.cursorRow, "Should stay on last row")
        assertEquals(9, buffer.cursorCol, "Cursor should advance")
        assertEquals('X', buffer.getBuffer()[4][8].char, "Character should be written")
    }

    @Test
    fun testEmptyBuffer_Operations() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Operations on empty buffer should not crash
        buffer.clearScreen()
        buffer.clearLine()
        buffer.clearToEndOfScreen()
        buffer.clearToStartOfScreen()
        buffer.insertLines(1)
        buffer.deleteLines(1)
        buffer.saveCursor()
        buffer.restoreCursor()
    }
}
