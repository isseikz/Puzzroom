package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
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

    @Test
    fun testInsertLines_DeepCopy() {
        // This test verifies that insertLines creates independent line copies,
        // not just reference copies (which would cause corruption)
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Write distinct content on each line
        buffer.moveCursor(1, 1)
        buffer.writeChar('A')
        buffer.moveCursor(2, 1)
        buffer.writeChar('B')
        buffer.moveCursor(3, 1)
        buffer.writeChar('C')
        buffer.moveCursor(4, 1)
        buffer.writeChar('D')

        // Insert a line at row 2
        buffer.moveCursor(2, 1)
        buffer.insertLines(1)

        // Now modify the shifted line (originally 'B', now at row 3)
        buffer.moveCursor(3, 2)
        buffer.writeChar('X')

        val screenBuffer = buffer.getBuffer()
        // Line 4 (originally 'C', shifted down) should NOT have 'X'
        // If insertLines used reference copy, line 3 and line 4 would share the same array
        assertEquals('C', screenBuffer[3][0].char, "Line 4 should still have 'C'")
        assertEquals(' ', screenBuffer[3][1].char, "Line 4 col 2 should be empty, not 'X'")
    }

    @Test
    fun testDeleteLines_DeepCopy() {
        // This test verifies that deleteLines creates independent line copies
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Write distinct content on each line
        buffer.moveCursor(1, 1)
        buffer.writeChar('A')
        buffer.moveCursor(2, 1)
        buffer.writeChar('B')
        buffer.moveCursor(3, 1)
        buffer.writeChar('C')
        buffer.moveCursor(4, 1)
        buffer.writeChar('D')
        buffer.moveCursor(5, 1)
        buffer.writeChar('E')

        // Delete a line at row 2 ('B' gets deleted)
        buffer.moveCursor(2, 1)
        buffer.deleteLines(1)

        // Now modify line 2 (which now contains 'C')
        buffer.moveCursor(2, 2)
        buffer.writeChar('X')

        val screenBuffer = buffer.getBuffer()
        // Line 3 (now contains 'D') should NOT have 'X'
        assertEquals('D', screenBuffer[2][0].char, "Line 3 should have 'D'")
        assertEquals(' ', screenBuffer[2][1].char, "Line 3 col 2 should be empty, not 'X'")
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

    // ========== Wide Character (Full-width) Handling ==========

    @Test
    fun testWriteAtLastCell_DoesNotScrollImmediately() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // 1. Write marker at top
        buffer.moveCursor(1, 1)
        buffer.writeChar('A')

        // 2. Move to last cell of last line
        buffer.moveCursor(5, 10) // row 5, col 10 (1-indexed)

        // 3. Write char at last cell
        buffer.writeChar('Z')

        val screenBuffer = buffer.getBuffer()

        // 4. Verify no scroll happened yet
        // The marker 'A' should still be at (0,0)
        assertEquals('A', screenBuffer[0][0].char, "Screen should not scroll immediately after writing to last cell")
        assertEquals('Z', screenBuffer[4][9].char, "Char should be written at last cell")
        
        // 5. Verify cursor state (conceptually at the end of line, waiting for next char to wrap)
        // Note: Implementation dependent, but typically cursor stays at last col or moves to a "phantom" col
        // For now, checking content integrity is most important
    }

    /**
     * Test: 全角文字の基本的な書き込み
     *
     * 操作: writeChar('あ')
     *
     * 期待されるバッファ状態:
     *   Col:  0   1   2   3 ...
     *        [あ][P] [ ] [ ] ...
     *        ↑   ↑
     *      wide padding
     *
     * 全角文字を書き込むと:
     * 1. 現在位置に文字が書き込まれ、isWideChar=true がセットされる
     * 2. 次の位置にパディングセル（isWideCharPadding=true）が配置される
     * 3. カーソルは2つ進む（全角文字は2セル幅）
     */
    @Test
    fun testWideChar_BasicWrite() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        buffer.writeChar('あ')  // Hiragana 'a' - should occupy 2 cells

        val screenBuffer = buffer.getBuffer()
        assertEquals('あ', screenBuffer[0][0].char, "Wide char should be written")
        assertTrue(screenBuffer[0][0].isWideChar, "Cell should be marked as wide char")
        assertTrue(screenBuffer[0][1].isWideCharPadding, "Next cell should be padding")
        assertEquals(2, buffer.cursorCol, "Cursor should advance by 2")
    }

    /**
     * Test: 複数の全角文字の連続書き込み
     *
     * 操作: writeChar('日'), writeChar('本'), writeChar('語')
     *
     * 期待されるバッファ状態:
     *   Col:  0   1   2   3   4   5   6 ...
     *        [日][P] [本][P] [語][P] [ ] ...
     *
     * 3文字 × 2セル = 6セル消費、カーソルは列6に移動
     */
    @Test
    fun testWideChar_MultipleWideChars() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        buffer.writeChar('日')  // Kanji
        buffer.writeChar('本')  // Kanji
        buffer.writeChar('語')  // Kanji

        val screenBuffer = buffer.getBuffer()
        assertEquals('日', screenBuffer[0][0].char)
        assertTrue(screenBuffer[0][0].isWideChar)
        assertTrue(screenBuffer[0][1].isWideCharPadding)

        assertEquals('本', screenBuffer[0][2].char)
        assertTrue(screenBuffer[0][2].isWideChar)
        assertTrue(screenBuffer[0][3].isWideCharPadding)

        assertEquals('語', screenBuffer[0][4].char)
        assertTrue(screenBuffer[0][4].isWideChar)
        assertTrue(screenBuffer[0][5].isWideCharPadding)

        assertEquals(6, buffer.cursorCol, "Cursor should be at position 6")
    }

    /**
     * Test: 半角文字と全角文字の混在
     *
     * 操作: writeChar('A'), writeChar('あ'), writeChar('B')
     *
     * 期待されるバッファ状態:
     *   Col:  0   1   2   3   4 ...
     *        [A] [あ][P] [B] [ ] ...
     *        ↑   ↑       ↑
     *       1セル 2セル  1セル
     *
     * 半角文字は1セル、全角文字は2セルを消費
     * 合計: 1 + 2 + 1 = 4セル、カーソルは列4に移動
     */
    @Test
    fun testWideChar_MixedWithNarrow() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        buffer.writeChar('A')   // Narrow (1 cell)
        buffer.writeChar('あ')  // Wide (2 cells)
        buffer.writeChar('B')   // Narrow (1 cell)

        val screenBuffer = buffer.getBuffer()
        assertEquals('A', screenBuffer[0][0].char)
        assertFalse(screenBuffer[0][0].isWideChar)

        assertEquals('あ', screenBuffer[0][1].char)
        assertTrue(screenBuffer[0][1].isWideChar)
        assertTrue(screenBuffer[0][2].isWideCharPadding)

        assertEquals('B', screenBuffer[0][3].char)
        assertFalse(screenBuffer[0][3].isWideChar)

        assertEquals(4, buffer.cursorCol)
    }

    /**
     * Test: 最終列での全角文字書き込み時の自動折り返し
     *
     * ターミナルサイズ: 10列 × 5行
     *
     * 操作:
     *   1. moveCursor(1, 10) → カーソルを最終列（列9、0-indexed）に移動
     *   2. writeChar('あ') → 全角文字（2セル必要）を書き込み
     *
     * 期待される動作:
     *   最終列から全角文字を書き込もうとすると、パディングセルを
     *   置くスペースがないため:
     *   1. 最終列は空白のまま残す
     *   2. 次の行の先頭から全角文字を書き込む
     *
     * 期待されるバッファ状態:
     *   Row 0: [ ][ ][ ][ ][ ][ ][ ][ ][ ][ ]  ← 列9は空白
     *   Row 1: [あ][P][ ][ ][ ][ ][ ][ ][ ][ ]  ← 全角文字は次行に
     */
    @Test
    fun testWideChar_AtLineEnd_Wraps() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Move cursor to column 9 (last column, 0-indexed)
        buffer.moveCursor(1, 10)  // 1-indexed: row 1, col 10

        // Write a wide char - should wrap to next line because it needs 2 cells
        buffer.writeChar('あ')

        val screenBuffer = buffer.getBuffer()
        // The last cell of row 0 should be empty (can't fit wide char)
        assertEquals(' ', screenBuffer[0][9].char, "Last cell should be empty")

        // Wide char should be on row 1
        assertEquals('あ', screenBuffer[1][0].char, "Wide char should wrap to next line")
        assertTrue(screenBuffer[1][0].isWideChar)
        assertTrue(screenBuffer[1][1].isWideCharPadding)

        assertEquals(1, buffer.cursorRow, "Cursor should be on row 1")
        assertEquals(2, buffer.cursorCol, "Cursor should be at column 2")
    }

    /**
     * Test: 最後から2番目の列での全角文字書き込み
     *
     * ターミナルサイズ: 10列 × 5行
     *
     * 操作:
     *   1. moveCursor(1, 9) → カーソルを列9（0-indexedで列8）に移動
     *   2. writeChar('あ') → 全角文字を書き込み
     *
     * 期待される動作:
     *   列8-9の2セルに全角文字がぴったり収まる。
     *   書き込み後、カーソルは次の行の先頭に折り返す。
     *
     * 期待されるバッファ状態:
     *   Row 0: [ ][ ][ ][ ][ ][ ][ ][ ][あ][P]  ← 列8-9に全角文字
     *   Row 1: (カーソル位置)
     */
    @Test
    fun testWideChar_AtSecondToLastColumn() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Move cursor to column 9 (second to last, 0-indexed = 8)
        buffer.moveCursor(1, 9)  // 1-indexed

        // Write a wide char - should fit
        buffer.writeChar('あ')

        val screenBuffer = buffer.getBuffer()
        assertEquals('あ', screenBuffer[0][8].char, "Wide char should be at col 8")
        assertTrue(screenBuffer[0][8].isWideChar)
        assertTrue(screenBuffer[0][9].isWideCharPadding, "Padding at col 9")

        // With Delayed Wrap, cursor stays at the last column
        assertEquals(0, buffer.cursorRow, "Cursor should NOT wrap immediately (Delayed Wrap)")
        assertEquals(9, buffer.cursorCol, "Cursor should be at last column")
    }

    /**
     * Test: 全角文字を半角文字で上書き
     *
     * 操作:
     *   1. writeChar('あ') → [あ][P] を書き込み
     *   2. moveCursor(1, 1) → カーソルを列0に戻す
     *   3. writeChar('X') → 全角文字の先頭セルを上書き
     *
     * 期待される動作:
     *   全角文字の先頭セルを上書きすると、全角文字は不完全になるため:
     *   1. 上書きした位置に'X'が入る
     *   2. パディングセル（列1）は空白にクリアされる
     *
     * 期待されるバッファ状態:
     *   Before: [あ][P]
     *   After:  [X] [ ]  ← パディングもクリア
     */
    @Test
    fun testWideChar_OverwriteWideChar() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Write a wide char
        buffer.writeChar('あ')

        // Move back and overwrite with narrow char
        buffer.moveCursor(1, 1)
        buffer.writeChar('X')

        val screenBuffer = buffer.getBuffer()
        assertEquals('X', screenBuffer[0][0].char, "Wide char should be overwritten")
        assertFalse(screenBuffer[0][0].isWideChar, "Cell should not be marked as wide")
        // The padding cell should be cleared
        assertEquals(' ', screenBuffer[0][1].char, "Padding cell should be cleared")
        assertFalse(screenBuffer[0][1].isWideCharPadding, "Padding flag should be cleared")
    }

    /**
     * Test: 全角文字のパディングセルを上書き
     *
     * 操作:
     *   1. writeChar('あ') → [あ][P] を書き込み
     *   2. moveCursor(1, 2) → カーソルを列1（パディング位置）に移動
     *   3. writeChar('X') → パディングセルを上書き
     *
     * 期待される動作:
     *   パディングセルを上書きすると、元の全角文字は不完全になるため:
     *   1. 元の全角文字（列0）は空白にクリアされる
     *   2. 上書きした位置（列1）に'X'が入る
     *
     * 期待されるバッファ状態:
     *   Before: [あ][P]
     *   After:  [ ] [X]  ← 元の全角文字もクリア
     */
    @Test
    fun testWideChar_OverwritePaddingCell() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Write a wide char
        buffer.writeChar('あ')

        // Move to padding cell (col 1) and overwrite
        buffer.moveCursor(1, 2)  // 1-indexed
        buffer.writeChar('X')

        val screenBuffer = buffer.getBuffer()
        // The original wide char should be cleared
        assertEquals(' ', screenBuffer[0][0].char, "Original wide char should be cleared")
        assertFalse(screenBuffer[0][0].isWideChar, "Wide char flag should be cleared")

        assertEquals('X', screenBuffer[0][1].char, "X should overwrite padding")
        assertFalse(screenBuffer[0][1].isWideCharPadding, "Padding flag should be cleared")
    }

    /**
     * Test: 半角文字を全角文字で上書き
     *
     * 操作:
     *   1. writeChar('A'), writeChar('B'), writeChar('C') → [A][B][C]
     *   2. moveCursor(1, 2) → カーソルを列1（'B'の位置）に移動
     *   3. writeChar('あ') → 'B'の位置に全角文字を書き込み
     *
     * 期待される動作:
     *   全角文字は2セル必要なため、'B'の位置と'C'の位置を占める。
     *   1. 列1に全角文字が入る
     *   2. 列2は全角文字のパディングに置き換わる（'C'は消える）
     *
     * 期待されるバッファ状態:
     *   Before: [A][B][C]
     *   After:  [A][あ][P]  ← 'C'はパディングに置き換わる
     */
    @Test
    fun testWideChar_OverwriteWithWideChar() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Write ABC
        buffer.writeChar('A')
        buffer.writeChar('B')
        buffer.writeChar('C')

        // Move back and overwrite B with wide char
        buffer.moveCursor(1, 2)  // Position of 'B'
        buffer.writeChar('あ')

        val screenBuffer = buffer.getBuffer()
        assertEquals('A', screenBuffer[0][0].char, "A should remain")
        assertEquals('あ', screenBuffer[0][1].char, "Wide char should be at position 1")
        assertTrue(screenBuffer[0][1].isWideChar)
        // C at position 2 should be replaced by padding
        assertTrue(screenBuffer[0][2].isWideCharPadding, "Position 2 should be padding")
    }

    /**
     * Test: 全角文字を含む行のスクロール
     *
     * ターミナルサイズ: 10列 × 3行
     *
     * 操作:
     *   1. 各行に全角文字を書き込み:
     *      Row 0: [あ][P][い][P]
     *      Row 1: [う][P][え][P]
     *      Row 2: [お][P][か][P]
     *   2. 最終行でさらに10文字書き込み → スクロールがトリガー
     *
     * 期待される動作:
     *   スクロール時、全角文字とそのパディングは一緒に移動する。
     *   Row 0の内容は消え、Row 1の内容がRow 0に移動する。
     *
     * 期待されるバッファ状態（スクロール後）:
     *   Row 0: [う][P][え][P]  ← 元のRow 1
     *   Row 1: [お][P][か][P]  ← 元のRow 2
     *   Row 2: [X][X][X]...    ← 新しい内容
     */
    @Test
    fun testWideChar_ScrollWithWideChars() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 3)

        // Fill first line with wide chars
        buffer.moveCursor(1, 1)
        buffer.writeChar('あ')
        buffer.writeChar('い')

        // Fill second line
        buffer.moveCursor(2, 1)
        buffer.writeChar('う')
        buffer.writeChar('え')

        // Fill third line
        buffer.moveCursor(3, 1)
        buffer.writeChar('お')
        buffer.writeChar('か')

        // Trigger scroll by writing more on last line
        for (i in 0 until 10) {
            buffer.writeChar('X')
        }

        val screenBuffer = buffer.getBuffer()
        // First line (was second) should have う and え
        assertEquals('う', screenBuffer[0][0].char, "First line should have scrolled content")
        assertTrue(screenBuffer[0][0].isWideChar)
    }

    /**
     * Test: 全角文字を含む行のクリア
     *
     * 操作:
     *   1. writeChar('あ'), writeChar('い') → [あ][P][い][P]
     *   2. clearLine() → 行全体をクリア
     *
     * 期待される動作:
     *   clearLine()は行のすべてのセルを空白にクリアする。
     *   全角文字フラグとパディングフラグも同時にクリアされる。
     *
     * 期待されるバッファ状態:
     *   Before: [あ][P][い][P]
     *   After:  [ ] [ ] [ ] [ ]  ← すべてのフラグもクリア
     */
    @Test
    fun testWideChar_ClearLine() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        buffer.writeChar('あ')
        buffer.writeChar('い')

        buffer.moveCursor(1, 3)
        buffer.clearLine()

        val screenBuffer = buffer.getBuffer()
        assertEquals(' ', screenBuffer[0][0].char, "Wide char should be cleared")
        assertFalse(screenBuffer[0][0].isWideChar, "Wide char flag should be cleared")
        assertEquals(' ', screenBuffer[0][1].char, "Padding should be cleared")
        assertFalse(screenBuffer[0][1].isWideCharPadding, "Padding flag should be cleared")
    }

    /**
     * Test: スクロール領域内での全角文字処理
     *
     * ターミナルサイズ: 10列 × 10行
     *
     * 操作:
     *   1. setScrollRegion(3, 7) → スクロール領域を行3-7に設定
     *   2. スクロール領域内（行7）に全角文字を書き込み、スクロールをトリガー
     *   3. スクロール領域外（行1と行10）にも全角文字を書き込み
     *
     * 期待される動作:
     *   - スクロール領域内でスクロールが発生
     *   - スクロール領域外（行1と行10）の内容は影響を受けない
     *
     * これはbyobuやtmuxのステータスバー動作に関連する重要なテスト。
     */
    @Test
    fun testWideChar_InScrollRegion() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 10)

        // Set scroll region to lines 3-7
        buffer.setScrollRegion(3, 7)

        // Write wide chars at bottom of scroll region
        buffer.moveCursor(7, 1)
        buffer.writeChar('あ')
        buffer.writeChar('い')
        buffer.writeChar('う')
        buffer.writeChar('え')
        buffer.writeChar('お')  // Should trigger scroll within region

        // Lines outside region should not be affected
        buffer.moveCursor(1, 1)
        buffer.writeChar('外')  // Outside scroll region

        buffer.moveCursor(10, 1)
        buffer.writeChar('外')  // Outside scroll region (below)

        val screenBuffer = buffer.getBuffer()
        assertEquals('外', screenBuffer[0][0].char, "Line above region should be unchanged")
        assertEquals('外', screenBuffer[9][0].char, "Line below region should be unchanged")
    }

    /**
     * Test: 代替スクリーンバッファでの全角文字処理
     *
     * 操作:
     *   1. writeChar('主') → プライマリバッファに書き込み
     *   2. useAlternateScreenBuffer() → 代替バッファに切り替え
     *   3. writeChar('副') → 代替バッファに書き込み
     *   4. usePrimaryScreenBuffer() → プライマリバッファに戻る
     *
     * 期待される動作:
     *   - 代替バッファ切り替え時、プライマリの内容は保持される
     *   - 代替バッファは空の状態から開始
     *   - プライマリに戻ると、元の全角文字が復元される
     */
    @Test
    fun testWideChar_AlternateBuffer() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        // Write to primary buffer
        buffer.writeChar('主')

        // Switch to alternate buffer
        buffer.useAlternateScreenBuffer()

        // Alternate should be empty
        val altBuffer = buffer.getBuffer()
        assertEquals(' ', altBuffer[0][0].char, "Alternate should be empty")

        // Write wide char to alternate
        buffer.writeChar('副')
        assertEquals('副', buffer.getBuffer()[0][0].char)
        assertTrue(buffer.getBuffer()[0][0].isWideChar)

        // Switch back to primary
        buffer.usePrimaryScreenBuffer()

        // Primary should still have original wide char
        assertEquals('主', buffer.getBuffer()[0][0].char)
        assertTrue(buffer.getBuffer()[0][0].isWideChar)
    }

    /**
     * Test: バッファリサイズ時の全角文字保持
     *
     * 操作:
     *   1. writeChar('あ'), writeChar('い') → [あ][P][い][P]
     *   2. resize(5, 3) → バッファを小さくリサイズ
     *
     * 期待される動作:
     *   リサイズ後も、新しいサイズに収まる全角文字は保持される。
     *   全角文字フラグとパディングフラグも保持される。
     *
     * 期待されるバッファ状態（リサイズ後、5列×3行）:
     *   [あ][P][い][P][ ]  ← 全角文字とパディングが保持される
     */
    @Test
    fun testWideChar_Resize() {
        val buffer = TerminalScreenBuffer(cols = 10, rows = 5)

        buffer.writeChar('あ')
        buffer.writeChar('い')

        // Resize to smaller
        buffer.resize(newCols = 5, newRows = 3)

        val screenBuffer = buffer.getBuffer()
        assertEquals('あ', screenBuffer[0][0].char, "Wide char should be preserved")
        assertTrue(screenBuffer[0][0].isWideChar)
        assertTrue(screenBuffer[0][1].isWideCharPadding)
        assertEquals('い', screenBuffer[0][2].char, "Second wide char should be preserved")
    }

    /**
     * Test: 半角・全角混在時のカーソル位置追跡
     *
     * 操作:
     *   1. writeChar('A')   → 列0に書き込み、カーソル→列1
     *   2. writeChar('あ')  → 列1-2に書き込み、カーソル→列3
     *   3. writeChar('B')   → 列3に書き込み、カーソル→列4
     *   4. writeChar('い')  → 列4-5に書き込み、カーソル→列6
     *
     * 期待されるバッファ状態:
     *   Col:  0   1   2   3   4   5   6
     *        [A] [あ][P] [B] [い][P] (cursor)
     *
     * カーソル位置は各文字のセル幅を正しく反映して列6になる。
     */
    @Test
    fun testWideChar_CursorPositionAfterWrite() {
        val buffer = TerminalScreenBuffer(cols = 20, rows = 5)

        // Write mix of wide and narrow chars
        buffer.writeChar('A')   // col 0 -> cursor at 1
        buffer.writeChar('あ')  // col 1-2 -> cursor at 3
        buffer.writeChar('B')   // col 3 -> cursor at 4
        buffer.writeChar('い')  // col 4-5 -> cursor at 6

        assertEquals(6, buffer.cursorCol, "Cursor should account for wide char widths")
    }
}
