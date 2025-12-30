package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for xterm specification compliance.
 * Tests real-world terminal sequences from applications like vim, tmux, and byobu.
 */
class XtermComplianceTest {

    private fun createParser(): Pair<AnsiEscapeParser, TerminalScreenBuffer> {
        val buffer = TerminalScreenBuffer(cols = 80, rows = 24)
        val parser = AnsiEscapeParser(buffer)
        return Pair(parser, buffer)
    }

    // ========== VT100 Compliance ==========

    @Test
    fun testVT100_BasicTextOutput() {
        val (parser, buffer) = createParser()

        parser.processText("Hello, World!")

        assertEquals('H', buffer.getBuffer()[0][0].char)
        assertEquals('e', buffer.getBuffer()[0][1].char)
        assertEquals('l', buffer.getBuffer()[0][2].char)
        assertEquals('!', buffer.getBuffer()[0][12].char)
    }

    @Test
    fun testVT100_CursorAddressing() {
        val (parser, buffer) = createParser()

        // VT100 sequence: Move to row 5, col 10 and write 'X'
        parser.processText("\u001B[5;10HX")

        assertEquals('X', buffer.getBuffer()[4][9].char)
        assertEquals(4, buffer.cursorRow)
        assertEquals(10, buffer.cursorCol)
    }

    @Test
    fun testVT100_ScreenErase() {
        val (parser, buffer) = createParser()

        parser.processText("AAAAA\nBBBBB\nCCCCC")
        parser.processText("\u001B[2J")  // Erase entire screen

        assertEquals(' ', buffer.getBuffer()[0][0].char)
        assertEquals(' ', buffer.getBuffer()[1][0].char)
        assertEquals(' ', buffer.getBuffer()[2][0].char)
    }

    @Test
    fun testVT100_LineErase() {
        val (parser, buffer) = createParser()

        parser.processText("XXXXXXXXXX")
        parser.processText("\u001B[1;5H")  // Move to col 5
        parser.processText("\u001B[K")     // Erase to end of line

        assertEquals('X', buffer.getBuffer()[0][0].char)
        assertEquals(' ', buffer.getBuffer()[0][4].char)
        assertEquals(' ', buffer.getBuffer()[0][9].char)
    }

    // ========== VT220 Compliance ==========

    @Test
    fun testVT220_ScrollRegion() {
        val (parser, buffer) = createParser()

        // VT220 DECSTBM: Set scrolling region to lines 5-15
        parser.processText("\u001B[5;15r")

        // Fill line 15 and overflow to trigger scroll
        parser.processText("\u001B[15;1H")  // Move to line 15
        for (i in 0 until 80) {
            parser.processText("X")
        }
        // Write more to trigger scroll
        parser.processText("Y")

        // Scroll should only affect region 5-15
        assertEquals(14, buffer.cursorRow, "Cursor should stay within scroll region")
    }

    @Test
    fun testVT220_InsertDeleteLines() {
        val (parser, buffer) = createParser()

        // Write lines at specific positions to avoid newline issues
        parser.processText("\u001B[1;1HLine 1")
        parser.processText("\u001B[2;1HLine 2")
        parser.processText("\u001B[3;1HLine 3")

        // Move to line 2 and insert a line
        parser.processText("\u001B[2;1H")
        parser.processText("\u001B[L")

        assertEquals('L', buffer.getBuffer()[0][0].char, "Line 1 should remain")
        assertEquals(' ', buffer.getBuffer()[1][0].char, "Inserted line should be blank")
        assertEquals('L', buffer.getBuffer()[2][0].char, "Line 2 should shift down")
    }

    @Test
    fun testVT220_DeleteCharacters() {
        val (parser, buffer) = createParser()

        parser.processText("ABCDEFGHIJ")
        parser.processText("\u001B[1;5H")  // Move to 'E'
        parser.processText("\u001B[3P")    // Delete 3 characters (DCH)

        // Note: DCH is not implemented in current AnsiEscapeParser
        // This test documents expected behavior for future implementation
    }

    // ========== Xterm-specific Features ==========

    @Test
    fun testXterm_AlternateScreenBuffer() {
        val (parser, buffer) = createParser()

        // This is commonly used by vim and less
        parser.processText("Primary screen content")

        // Switch to alternate screen buffer (xterm mode 1049)
        parser.processText("\u001B[?1049h")
        assertTrue(buffer.isAlternateScreen, "Should be on alternate screen")
        assertEquals(' ', buffer.getBuffer()[0][0].char, "Alternate screen should be empty")

        parser.processText("Alternate content")
        assertEquals('A', buffer.getBuffer()[0][0].char)

        // Switch back to primary
        parser.processText("\u001B[?1049l")
        assertTrue(!buffer.isAlternateScreen, "Should be back on primary")
        assertEquals('P', buffer.getBuffer()[0][0].char, "Primary content restored")
    }

    @Test
    fun testXterm_256ColorSupport() {
        val (parser, buffer) = createParser()

        // Xterm 256-color mode is documented but not yet implemented
        // CSI 38;5;N m - Set foreground to color N (0-255)
        // CSI 48;5;N m - Set background to color N (0-255)

        // This test documents expected behavior for future implementation
        // parser.processText("\u001B[38;5;196mRed text")
        // parser.processText("\u001B[48;5;21mBlue background")
    }

    @Test
    fun testXterm_TitleSetting() {
        val (parser, buffer) = createParser()

        // OSC sequences for window title (not yet implemented)
        // ESC ] 0 ; title BEL - Set window title
        parser.processText("\u001B]0;My Terminal\u0007")

        // Should not crash, even if not implemented
        // Title handling is typically done at UI level
    }

    // ========== Real-world Application Sequences ==========

    @Test
    fun testVim_StartupSequence() {
        val (parser, buffer) = createParser()

        // Typical vim startup: switch to alternate screen, clear, position cursor
        parser.processText("\u001B[?1049h")      // Alternate screen (cursor already at 0,0)
        parser.processText("\u001B[2J")          // Clear screen (cursor moves to 0,0)

        // After clear, write vim tilde lines with proper line positioning
        parser.processText("\u001B[1;1H~")       // Position at 1,1 and write first tilde
        parser.processText("\u001B[2;1H~")       // Position at 2,1 and write second tilde

        assertTrue(buffer.isAlternateScreen, "Vim should use alternate screen")
        assertEquals('~', buffer.getBuffer()[0][0].char, "First tilde line")
        assertEquals('~', buffer.getBuffer()[1][0].char, "Second tilde line")
    }

    @Test
    fun testVim_ExitSequence() {
        val (parser, buffer) = createParser()

        parser.processText("Original content")
        parser.processText("\u001B[?1049h")      // Enter vim
        parser.processText("Vim content")
        parser.processText("\u001B[?1049l")      // Exit vim

        assertTrue(!buffer.isAlternateScreen, "Should return to primary")
        assertEquals('O', buffer.getBuffer()[0][0].char, "Original content restored")
    }

    @Test
    fun testTmux_StatusLine() {
        val (parser, buffer) = createParser()

        // Tmux uses scroll regions to keep status bar fixed
        parser.processText("\u001B[1;23r")       // Scroll region: lines 1-23 (status on 24)

        // Write status bar on line 24
        parser.processText("\u001B[24;1H")
        parser.processText("\u001B[7m")          // Reverse video
        parser.processText(" tmux [0] ")

        // Write content in scrolling region
        parser.processText("\u001B[1;1H")
        parser.processText("Content line 1\n")
        parser.processText("Content line 2\n")

        // Status bar should remain when content scrolls
        assertTrue(buffer.getBuffer()[23][1].isReverse, "Status bar should be reverse video")
    }

    @Test
    fun testByobu_ComplexLayout() {
        val (parser, buffer) = createParser()

        // Byobu uses scroll regions similar to tmux
        parser.processText("\u001B[1;22r")       // Main content area
        parser.processText("\u001B[23r")         // Status line separator
        parser.processText("\u001B[24r")         // Bottom status bar

        // This tests that multiple scroll region changes don't break state
        parser.processText("\u001B[1;1H")
        parser.processText("Application output")

        // Should not crash or corrupt display
        assertEquals('A', buffer.getBuffer()[0][0].char)
    }

    @Test
    fun testLess_Pager() {
        val (parser, buffer) = createParser()

        // less uses alternate screen and colors
        parser.processText("\u001B[?1049h")      // Alternate screen
        parser.processText("\u001B[2J")          // Clear
        parser.processText("\u001B[H")           // Home
        parser.processText("\u001B[7m")          // Reverse video for highlight
        parser.processText("Search result")
        parser.processText("\u001B[27m")         // Normal video

        assertTrue(buffer.getBuffer()[0][0].isReverse, "Search highlight should be reverse")
        assertTrue(!buffer.getBuffer()[0][13].isReverse, "After highlight should be normal")
    }

    @Test
    fun testTop_DynamicUpdates() {
        val (parser, buffer) = createParser()

        // top clears screen and repositions cursor frequently
        parser.processText("\u001B[H\u001B[2J")  // Clear and home
        parser.processText("top - 12:34:56 up 1 day")

        // Simulate update
        parser.processText("\u001B[H")           // Return to home
        parser.processText("top - 12:34:57 up 1 day")

        assertEquals('t', buffer.getBuffer()[0][0].char, "Content should be updated")
    }

    @Test
    fun testHtop_ColorBars() {
        val (parser, buffer) = createParser()

        // htop uses colors extensively
        parser.processText("\u001B[32m")         // Green for CPU bar
        parser.processText("||||")
        parser.processText("\u001B[31m")         // Red for memory bar
        parser.processText("||||")
        parser.processText("\u001B[0m")          // Reset

        assertEquals(androidx.compose.ui.graphics.Color(0xFF50FA7B), buffer.getBuffer()[0][0].foregroundColor, "Green")
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFF5555), buffer.getBuffer()[0][4].foregroundColor, "Red")
    }

    // ========== Character Set Compliance ==========

    @Test
    fun testDECSpecialGraphics_LineDraw() {
        val (parser, buffer) = createParser()

        // DEC Special Graphics Character Set (line drawing)
        parser.processText("\u001B(0")           // Select DEC graphics into G0
        // In full implementation, 'q' would become horizontal line
        parser.processText("q")                  // Would be horizontal line
        parser.processText("\u001B(B")           // Back to ASCII
        parser.processText("A")

        // With implementation, q maps to horizontal line (─)
        assertEquals('─', buffer.getBuffer()[0][0].char, "Should map q to horizontal line")
        assertEquals('A', buffer.getBuffer()[0][1].char, "Should be A")
    }

    @Test
    fun testUTF8_CharacterSet() {
        val (parser, buffer) = createParser()

        // ESC % G - Select UTF-8 (not implemented, % is unknown escape)
        parser.processText("\u001B%G")
        parser.processText("UTF-8 text")

        // Current behavior: ESC % is unknown, returns to NORMAL, 'G' is written
        // Then "UTF-8 text" follows
        assertEquals('G', buffer.getBuffer()[0][0].char, "Unknown escape, 'G' written")
        assertEquals('U', buffer.getBuffer()[0][1].char, "UTF-8 text follows")
    }

    // ========== Performance and Stress Tests ==========

    @Test
    fun testLargeTextBlock() {
        val (parser, buffer) = createParser()

        // Process a large block of text
        val largeText = "X".repeat(1000)
        parser.processText(largeText)

        // Should handle without crash
        assertEquals('X', buffer.getBuffer()[0][0].char)
    }

    @Test
    fun testManyEscapeSequences() {
        val (parser, buffer) = createParser()

        // Rapid color changes
        for (i in 0 until 100) {
            parser.processText("\u001B[3${i % 8}m")
            parser.processText("X")
        }

        // Should handle without crash
        // Buffer is 80 cols, so after 80 chars, cursor wraps to next line
        // 100 chars = 1 full line (80 chars) + 20 chars on second line
        assertEquals(20, buffer.cursorCol, "Cursor wraps at 80 columns")
        assertEquals(1, buffer.cursorRow, "Should be on second line after 100 chars")
    }

    @Test
    fun testInterruptedSequences() {
        val (parser, buffer) = createParser()

        // Incomplete sequences should not break parser
        parser.processText("\u001B[")            // Incomplete CSI
        parser.processText("Normal text")        // Should recover

        // Exact behavior depends on implementation, but shouldn't crash
    }

    // ========== Mixed Content Tests ==========

    @Test
    fun testMixedTextAndControl() {
        val (parser, buffer) = createParser()

        parser.processText("Hello ")
        parser.processText("\u001B[31m")         // Red
        parser.processText("World")
        parser.processText("\u001B[0m")          // Reset
        parser.processText("!")

        assertEquals('H', buffer.getBuffer()[0][0].char)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFF5555), buffer.getBuffer()[0][6].foregroundColor, "World should be red")
        assertEquals(androidx.compose.ui.graphics.Color.White, buffer.getBuffer()[0][11].foregroundColor, "! should be white")
    }

    @Test
    fun testNewlineAndCarriageReturn() {
        val (parser, buffer) = createParser()

        parser.processText("Line 1\r\n")
        parser.processText("Line 2\r\n")
        parser.processText("Line 3")

        assertEquals('L', buffer.getBuffer()[0][0].char, "Line 1")
        assertEquals('L', buffer.getBuffer()[1][0].char, "Line 2")
        assertEquals('L', buffer.getBuffer()[2][0].char, "Line 3")
    }

    @Test
    fun testTabStops() {
        val (parser, buffer) = createParser()

        parser.processText("A\tB\tC\tD")

        assertEquals('A', buffer.getBuffer()[0][0].char)
        assertEquals('B', buffer.getBuffer()[0][8].char, "Should tab to column 8")
        assertEquals('C', buffer.getBuffer()[0][16].char, "Should tab to column 16")
        assertEquals('D', buffer.getBuffer()[0][24].char, "Should tab to column 24")
    }

    // ========== Regression Tests ==========

    @Test
    fun testByobuArtifactFix() {
        val (parser, buffer) = createParser()

        // This tests the fix for character set selection artifacts
        // Previously, sequences like ESC(B would show "B" as text
        parser.processText("\u001B(B")           // Select ASCII
        parser.processText("Normal text")

        assertEquals('N', buffer.getBuffer()[0][0].char, "Should not have 'B' artifact")
    }

    @Test
    fun testScrollRegionCursorBounds() {
        val (parser, buffer) = createParser()

        // Set scroll region
        parser.processText("\u001B[5;20r")

        // Fill to trigger scroll
        parser.processText("\u001B[20;1H")
        for (i in 0 until 80) {
            parser.processText("X")
        }
        parser.processText("Overflow")

        // Cursor should stay within scroll region bounds
        assertTrue(buffer.cursorRow <= 19, "Cursor should stay within scroll region")
    }
}
