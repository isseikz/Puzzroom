package tokyo.isseikuzumaki.vibeterminal.terminal

import androidx.compose.ui.graphics.Color

/**
 * Manages the terminal screen buffer and cursor state.
 * This implements a simplified VT100-style terminal emulator.
 */
class TerminalScreenBuffer(
    private var cols: Int = 80,
    private var rows: Int = 24
) {
    private var buffer: Array<Array<TerminalCell>> = createBuffer(cols, rows)
    private var primaryBuffer: Array<Array<TerminalCell>> = buffer
    private var alternateBuffer: Array<Array<TerminalCell>>? = null

    // Cursor position (0-indexed)
    var cursorRow: Int = 0
        private set
    var cursorCol: Int = 0
        private set
    
    // Saved cursor states for primary buffer
    private var primaryCursorRow = 0
    private var primaryCursorCol = 0

    // Current text styling
    private var currentForeground = Color.White
    private var currentBackground = Color.Black
    private var isBold = false
    private var isUnderline = false
    private var isReverse = false

    // Saved cursor position (for save/restore cursor operations within a buffer)
    private var savedCursorRow = 0
    private var savedCursorCol = 0

    var isAlternateScreen: Boolean = false
        private set

    // Scroll region (DECSTBM) - 0-indexed, inclusive
    private var scrollTop = 0
    private var scrollBottom = rows - 1

    private fun createBuffer(cols: Int, rows: Int): Array<Array<TerminalCell>> {
        return Array(rows) { Array(cols) { TerminalCell.EMPTY } }
    }
    
    /**
     * Switch to the alternate screen buffer.
     * This saves the current (primary) buffer state and creates a new empty buffer.
     */
    fun useAlternateScreenBuffer() {
        if (!isAlternateScreen) {
            // Save primary cursor state
            primaryCursorRow = cursorRow
            primaryCursorCol = cursorCol
            primaryBuffer = buffer
            
            // Initialize alternate buffer if needed (or clear it)
            // xterm typically clears the alternate buffer on entry
            alternateBuffer = createBuffer(cols, rows)
            buffer = alternateBuffer!!
            
            // Reset cursor for alternate buffer
            cursorRow = 0
            cursorCol = 0
            
            isAlternateScreen = true
        }
    }

    /**
     * Switch back to the primary screen buffer.
     * This restores the primary buffer content and cursor state.
     */
    fun usePrimaryScreenBuffer() {
        if (isAlternateScreen) {
            buffer = primaryBuffer
            cursorRow = primaryCursorRow
            cursorCol = primaryCursorCol
            alternateBuffer = null
            isAlternateScreen = false
        }
    }

    /**
     * Get the current screen buffer as a 2D array
     */
    fun getBuffer(): Array<Array<TerminalCell>> = buffer

    /**
     * Write a character at the current cursor position
     */
    fun writeChar(char: Char) {
        if (cursorRow >= 0 && cursorRow < rows && cursorCol >= 0 && cursorCol < cols) {
            buffer[cursorRow][cursorCol] = TerminalCell(
                char = char,
                foregroundColor = if (isReverse) currentBackground else currentForeground,
                backgroundColor = if (isReverse) currentForeground else currentBackground,
                isBold = isBold,
                isUnderline = isUnderline,
                isReverse = isReverse
            )
            cursorCol++
            if (cursorCol >= cols) {
                cursorCol = 0
                cursorRow++
                // Check if we need to scroll within the scroll region
                if (cursorRow > scrollBottom) {
                    scrollUp()
                    cursorRow = scrollBottom
                }
            }
        }
    }

    /**
     * Move cursor to absolute position (1-indexed as per VT100 spec)
     */
    fun moveCursor(row: Int, col: Int) {
        cursorRow = (row - 1).coerceIn(0, rows - 1)
        cursorCol = (col - 1).coerceIn(0, cols - 1)
    }

    /**
     * Move cursor relatively
     */
    fun moveCursorRelative(rowDelta: Int, colDelta: Int) {
        cursorRow = (cursorRow + rowDelta).coerceIn(0, rows - 1)
        cursorCol = (cursorCol + colDelta).coerceIn(0, cols - 1)
    }

    /**
     * Move cursor to column (1-indexed)
     */
    fun moveCursorToColumn(col: Int) {
        cursorCol = (col - 1).coerceIn(0, cols - 1)
    }

    /**
     * Clear the entire screen
     */
    fun clearScreen() {
        // Instead of creating a new buffer, we should clear the existing one to preserve reference if it's primary or alternate locally
        // But for simplicity in this MVP, re-creating is fine as long as we assign it back to `buffer`
        // However, `buffer` is a reference. 
        // Correct approach:
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                buffer[r][c] = TerminalCell.EMPTY
            }
        }
        cursorRow = 0
        cursorCol = 0
    }

    /**
     * Clear from cursor to end of screen
     */
    fun clearToEndOfScreen() {
        // Clear rest of current line
        for (c in cursorCol until cols) {
            buffer[cursorRow][c] = TerminalCell.EMPTY
        }
        // Clear all lines below
        for (r in (cursorRow + 1) until rows) {
            for (c in 0 until cols) {
                buffer[r][c] = TerminalCell.EMPTY
            }
        }
    }

    /**
     * Clear from start of screen to cursor
     */
    fun clearToStartOfScreen() {
        // Clear all lines above
        for (r in 0 until cursorRow) {
            for (c in 0 until cols) {
                buffer[r][c] = TerminalCell.EMPTY
            }
        }
        // Clear start of current line to cursor
        for (c in 0..cursorCol) {
            buffer[cursorRow][c] = TerminalCell.EMPTY
        }
    }

    /**
     * Clear the current line
     */
    fun clearLine() {
        for (c in 0 until cols) {
            buffer[cursorRow][c] = TerminalCell.EMPTY
        }
    }

    /**
     * Clear from cursor to end of line
     */
    fun clearToEndOfLine() {
        for (c in cursorCol until cols) {
            buffer[cursorRow][c] = TerminalCell.EMPTY
        }
    }

    /**
     * Clear from start of line to cursor
     */
    fun clearToStartOfLine() {
        for (c in 0..cursorCol.coerceAtMost(cols - 1)) {
            buffer[cursorRow][c] = TerminalCell.EMPTY
        }
    }

    /**
     * Scroll the screen up by one line within the scroll region
     */
    private fun scrollUp() {
        // Move lines up within scroll region only
        for (r in scrollTop until scrollBottom) {
            buffer[r] = buffer[r + 1]
        }
        // Clear the last line in scroll region
        buffer[scrollBottom] = Array(cols) { TerminalCell.EMPTY }
    }

    /**
     * Scroll the screen down by one line within the scroll region
     */
    private fun scrollDown() {
        // Move lines down within scroll region only
        for (r in scrollBottom downTo scrollTop + 1) {
            buffer[r] = buffer[r - 1]
        }
        // Clear the first line in scroll region
        buffer[scrollTop] = Array(cols) { TerminalCell.EMPTY }
    }

    /**
     * Set scrolling region (DECSTBM)
     * @param top Top line (1-indexed, inclusive)
     * @param bottom Bottom line (1-indexed, inclusive)
     */
    fun setScrollRegion(top: Int, bottom: Int) {
        // Convert from 1-indexed to 0-indexed
        scrollTop = (top - 1).coerceIn(0, rows - 1)
        scrollBottom = (bottom - 1).coerceIn(scrollTop, rows - 1)
    }

    /**
     * Reset scrolling region to full screen
     */
    fun resetScrollRegion() {
        scrollTop = 0
        scrollBottom = rows - 1
    }

    /**
     * Set text styling based on SGR (Select Graphic Rendition) parameters
     */
    fun setGraphicsMode(params: List<Int>) {
        for (param in params) {
            when (param) {
                0 -> resetStyle()
                1 -> isBold = true
                4 -> isUnderline = true
                7 -> isReverse = true
                22 -> isBold = false
                24 -> isUnderline = false
                27 -> isReverse = false
                30 -> currentForeground = Color.Black
                31 -> currentForeground = Color(0xFFFF5555)  // Red
                32 -> currentForeground = Color(0xFF50FA7B)  // Green
                33 -> currentForeground = Color(0xFFF1FA8C)  // Yellow
                34 -> currentForeground = Color(0xFFBD93F9)  // Blue
                35 -> currentForeground = Color(0xFFFF79C6)  // Magenta
                36 -> currentForeground = Color(0xFF8BE9FD)  // Cyan
                37 -> currentForeground = Color.White
                39 -> currentForeground = Color.White  // Default
                40 -> currentBackground = Color.Black
                41 -> currentBackground = Color(0xFFFF5555)  // Red
                42 -> currentBackground = Color(0xFF50FA7B)  // Green
                43 -> currentBackground = Color(0xFFF1FA8C)  // Yellow
                44 -> currentBackground = Color(0xFFBD93F9)  // Blue
                45 -> currentBackground = Color(0xFFFF79C6)  // Magenta
                46 -> currentBackground = Color(0xFF8BE9FD)  // Cyan
                47 -> currentBackground = Color.White
                49 -> currentBackground = Color.Black  // Default
            }
        }
    }

    private fun resetStyle() {
        currentForeground = Color.White
        currentBackground = Color.Black
        isBold = false
        isUnderline = false
        isReverse = false
    }

    /**
     * Save current cursor position
     */
    fun saveCursor() {
        savedCursorRow = cursorRow
        savedCursorCol = cursorCol
    }

    /**
     * Restore saved cursor position
     */
    fun restoreCursor() {
        cursorRow = savedCursorRow
        cursorCol = savedCursorCol
    }

    /**
     * Resize the terminal buffer
     */
    fun resize(newCols: Int, newRows: Int) {
        if (newCols == cols && newRows == rows) return

        // Resize the current active buffer
        val newActiveBuffer = resizeBufferArray(buffer, cols, rows, newCols, newRows)
        buffer = newActiveBuffer
        
        // Update references
        if (isAlternateScreen) {
            alternateBuffer = newActiveBuffer
            // Resize primary buffer as well to stay in sync
            primaryBuffer = resizeBufferArray(primaryBuffer, cols, rows, newCols, newRows)
        } else {
            primaryBuffer = newActiveBuffer
            // If alternate buffer exists, resize it too
            alternateBuffer?.let { alt ->
                alternateBuffer = resizeBufferArray(alt, cols, rows, newCols, newRows)
            }
        }

        cols = newCols
        rows = newRows

        // Reset scroll region to full screen
        scrollTop = 0
        scrollBottom = rows - 1

        // Adjust cursor position if needed
        cursorRow = cursorRow.coerceIn(0, rows - 1)
        cursorCol = cursorCol.coerceIn(0, cols - 1)
    }

    private fun resizeBufferArray(
        source: Array<Array<TerminalCell>>,
        oldCols: Int,
        oldRows: Int,
        newCols: Int,
        newRows: Int
    ): Array<Array<TerminalCell>> {
        val newBuffer = createBuffer(newCols, newRows)
        val copyRows = minOf(oldRows, newRows)
        val copyCols = minOf(oldCols, newCols)

        for (r in 0 until copyRows) {
            for (c in 0 until copyCols) {
                newBuffer[r][c] = source[r][c]
            }
        }
        return newBuffer
    }

    /**
     * Insert blank lines at cursor position
     */
    fun insertLines(count: Int) {
        val linesToInsert = count.coerceAtMost(rows - cursorRow)
        for (i in 0 until linesToInsert) {
            // Shift lines down
            for (r in rows - 1 downTo cursorRow + 1) {
                buffer[r] = buffer[r - 1]
            }
            // Insert blank line at cursor
            buffer[cursorRow] = Array(cols) { TerminalCell.EMPTY }
        }
    }

    /**
     * Delete lines at cursor position
     */
    fun deleteLines(count: Int) {
        val linesToDelete = count.coerceAtMost(rows - cursorRow)
        for (i in 0 until linesToDelete) {
            // Shift lines up
            for (r in cursorRow until rows - 1) {
                buffer[r] = buffer[r + 1]
            }
            // Clear last line
            buffer[rows - 1] = Array(cols) { TerminalCell.EMPTY }
        }
    }
}
