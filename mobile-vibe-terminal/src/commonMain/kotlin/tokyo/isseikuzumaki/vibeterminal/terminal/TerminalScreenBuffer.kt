package tokyo.isseikuzumaki.vibeterminal.terminal

import androidx.compose.ui.graphics.Color

/**
 * Manages the terminal screen buffer and cursor state.
 * This implements a simplified VT100-style terminal emulator.
 */
class TerminalScreenBuffer(
    cols: Int = 80,
    rows: Int = 24
) {
    // Public read-only access to dimensions
    var cols: Int = cols
        private set
    var rows: Int = rows
        private set
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

    // Delayed wrap state (Xenon wrap)
    // When true, the cursor is technically past the last column, but wrapping is deferred
    // until the next character is written.
    private var delayedWrap = false

    /**
     * Get current scroll region (for testing and debugging)
     * Returns (top, bottom) as 0-indexed, inclusive bounds
     */
    val scrollRegion: Pair<Int, Int>
        get() = Pair(scrollTop, scrollBottom)

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
            delayedWrap = false
            
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
            delayedWrap = false
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
            val isWide = UnicodeWidth.isWideChar(char)

            // Resolve delayed wrap if pending
            if (delayedWrap) {
                delayedWrap = false
                cursorCol = 0
                cursorRow++
                if (cursorRow > scrollBottom) {
                    scrollUp()
                    cursorRow = scrollBottom
                }
            }

            // Handle wrapping for wide char at last column (Pre-wrap)
            // If we are at the last column and try to write a wide char, we wrap immediately
            // because a wide char cannot fit.
            if (isWide && cursorCol == cols - 1) {
                // Clear the last column
                buffer[cursorRow][cursorCol] = TerminalCell.EMPTY
                // Move to next line
                cursorCol = 0
                cursorRow++
                if (cursorRow > scrollBottom) {
                    scrollUp()
                    cursorRow = scrollBottom
                }
            }

            // Check boundary again after wrap
            if (cursorRow >= 0 && cursorRow < rows && cursorCol >= 0 && cursorCol < cols) {
                // Destructive overwrite logic
                // 1. Clear integrity at current position
                clearWideCharIntegrityAt(cursorRow, cursorCol)

                // 2. If writing a wide char, ensure the next cell is also cleared of any partial wide chars
                if (isWide && cursorCol + 1 < cols) {
                    clearWideCharIntegrityAt(cursorRow, cursorCol + 1)
                }

                val cell = TerminalCell(
                    char = char,
                    foregroundColor = if (isReverse) currentBackground else currentForeground,
                    backgroundColor = if (isReverse) currentForeground else currentBackground,
                    isBold = isBold,
                    isUnderline = isUnderline,
                    isReverse = isReverse,
                    isWideCharPadding = false
                )
                buffer[cursorRow][cursorCol] = cell

                if (isWide && cursorCol + 1 < cols) {
                    // Write padding cell
                    val paddingCell = cell.copy(char = ' ', isWideCharPadding = true)
                    buffer[cursorRow][cursorCol + 1] = paddingCell
                }

                cursorCol += if (isWide) 2 else 1
                
                if (cursorCol >= cols) {
                    delayedWrap = true
                    cursorCol = cols - 1
                }
            }
        }
    }

    /**
     * Clears wide character integrity at the given position.
     * If the cell at (row, col) is part of a wide character (either the main char or padding),
     * both cells are cleared to maintain buffer integrity.
     */
    private fun clearWideCharIntegrityAt(row: Int, col: Int) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return
        val target = buffer[row][col]

        if (target.isWideChar) {
            // This is the left half (main char), clear it and the right padding
            buffer[row][col] = TerminalCell.EMPTY
            if (col + 1 < cols) {
                // Ensure next one is actually padding before clearing (safe to clear anyway)
                if (buffer[row][col + 1].isWideCharPadding) {
                    buffer[row][col + 1] = TerminalCell.EMPTY
                }
            }
        } else if (target.isWideCharPadding) {
            // This is the right padding, clear it and the left half
            buffer[row][col] = TerminalCell.EMPTY
            if (col - 1 >= 0) {
                if (buffer[row][col - 1].isWideChar) {
                    buffer[row][col - 1] = TerminalCell.EMPTY
                }
            }
        }
    }

    /**
     * Move cursor to absolute position (1-indexed as per VT100 spec)
     */
    fun moveCursor(row: Int, col: Int) {
        delayedWrap = false
        cursorRow = (row - 1).coerceIn(0, rows - 1)
        cursorCol = (col - 1).coerceIn(0, cols - 1)
    }

    /**
     * Move cursor relatively
     */
    fun moveCursorRelative(rowDelta: Int, colDelta: Int) {
        delayedWrap = false
        cursorRow = (cursorRow + rowDelta).coerceIn(0, rows - 1)
        cursorCol = (cursorCol + colDelta).coerceIn(0, cols - 1)
    }

    /**
     * Move cursor to column (1-indexed)
     */
    fun moveCursorToColumn(col: Int) {
        delayedWrap = false
        cursorCol = (col - 1).coerceIn(0, cols - 1)
    }

    /**
     * Clear the entire screen
     */
    fun clearScreen() {
        delayedWrap = false
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
        delayedWrap = false
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
        delayedWrap = false
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
        delayedWrap = false
        for (c in 0 until cols) {
            buffer[cursorRow][c] = TerminalCell.EMPTY
        }
    }

    /**
     * Clear from cursor to end of line
     */
    fun clearToEndOfLine() {
        delayedWrap = false
        for (c in cursorCol until cols) {
            buffer[cursorRow][c] = TerminalCell.EMPTY
        }
    }

    /**
     * Clear from start of line to cursor
     */
    fun clearToStartOfLine() {
        delayedWrap = false
        for (c in 0..cursorCol.coerceAtMost(cols - 1)) {
            buffer[cursorRow][c] = TerminalCell.EMPTY
        }
    }

    /**
     * Scroll the screen up by one line within the scroll region
     */
    private fun scrollUp() {
        // Move lines up within scroll region only (deep copy cells, not array references)
        for (r in scrollTop until scrollBottom) {
            for (c in 0 until cols) {
                buffer[r][c] = buffer[r + 1][c]
            }
        }
        // Clear the last line in scroll region
        for (c in 0 until cols) {
            buffer[scrollBottom][c] = TerminalCell.EMPTY
        }
    }

    /**
     * Scroll the screen down by one line within the scroll region
     */
    private fun scrollDown() {
        // Move lines down within scroll region only (deep copy cells, not array references)
        for (r in scrollBottom downTo scrollTop + 1) {
            for (c in 0 until cols) {
                buffer[r][c] = buffer[r - 1][c]
            }
        }
        // Clear the first line in scroll region
        for (c in 0 until cols) {
            buffer[scrollTop][c] = TerminalCell.EMPTY
        }
    }

    /**
     * Set scrolling region (DECSTBM)
     * @param top Top line (1-indexed, inclusive)
     * @param bottom Bottom line (1-indexed, inclusive)
     */
    fun setScrollRegion(top: Int, bottom: Int) {
        delayedWrap = false
        // Convert from 1-indexed to 0-indexed
        scrollTop = (top - 1).coerceIn(0, rows - 1)
        scrollBottom = (bottom - 1).coerceIn(scrollTop, rows - 1)
    }

    /**
     * Reset scrolling region to full screen
     */
    fun resetScrollRegion() {
        delayedWrap = false
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
                // Standard foreground colors (30-37)
                30 -> currentForeground = Color.Black
                31 -> currentForeground = Color(0xFFFF5555)  // Red
                32 -> currentForeground = Color(0xFF50FA7B)  // Green
                33 -> currentForeground = Color(0xFFF1FA8C)  // Yellow
                34 -> currentForeground = Color(0xFFBD93F9)  // Blue
                35 -> currentForeground = Color(0xFFFF79C6)  // Magenta
                36 -> currentForeground = Color(0xFF8BE9FD)  // Cyan
                37 -> currentForeground = Color.White
                39 -> currentForeground = Color.White  // Default
                // Standard background colors (40-47)
                40 -> currentBackground = Color.Black
                41 -> currentBackground = Color(0xFFFF5555)  // Red
                42 -> currentBackground = Color(0xFF50FA7B)  // Green
                43 -> currentBackground = Color(0xFFF1FA8C)  // Yellow
                44 -> currentBackground = Color(0xFFBD93F9)  // Blue
                45 -> currentBackground = Color(0xFFFF79C6)  // Magenta
                46 -> currentBackground = Color(0xFF8BE9FD)  // Cyan
                47 -> currentBackground = Color.White
                49 -> currentBackground = Color.Black  // Default
                // Bright foreground colors (90-97)
                90 -> currentForeground = Color(0xFF6E6E6E)   // Bright Black (Gray)
                91 -> currentForeground = Color(0xFFFF6E6E)   // Bright Red
                92 -> currentForeground = Color(0xFF69FF94)   // Bright Green
                93 -> currentForeground = Color(0xFFFFFFA5)   // Bright Yellow
                94 -> currentForeground = Color(0xFFD6ACFF)   // Bright Blue
                95 -> currentForeground = Color(0xFFFF92DF)   // Bright Magenta
                96 -> currentForeground = Color(0xFFA4FFFF)   // Bright Cyan
                97 -> currentForeground = Color(0xFFFFFFFF)   // Bright White
                // Bright background colors (100-107)
                100 -> currentBackground = Color(0xFF6E6E6E)  // Bright Black (Gray)
                101 -> currentBackground = Color(0xFFFF6E6E)  // Bright Red
                102 -> currentBackground = Color(0xFF69FF94)  // Bright Green
                103 -> currentBackground = Color(0xFFFFFFA5)  // Bright Yellow
                104 -> currentBackground = Color(0xFFD6ACFF)  // Bright Blue
                105 -> currentBackground = Color(0xFFFF92DF)  // Bright Magenta
                106 -> currentBackground = Color(0xFFA4FFFF)  // Bright Cyan
                107 -> currentBackground = Color(0xFFFFFFFF)  // Bright White
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

    /**
     * Delete characters at cursor position (DCH - CSI P)
     * Shifts remaining characters on the line to the left
     */
    fun deleteCharacters(count: Int) {
        val charsToDelete = count.coerceAtMost(cols - cursorCol)
        if (charsToDelete <= 0) return

        // Check integrity at start of deletion (cursorCol)
        clearWideCharIntegrityAt(cursorRow, cursorCol)

        // Shift characters to the left
        for (c in cursorCol until cols - charsToDelete) {
            buffer[cursorRow][c] = buffer[cursorRow][c + charsToDelete]
        }
        // Clear the vacated positions at the end
        for (c in cols - charsToDelete until cols) {
            buffer[cursorRow][c] = TerminalCell.EMPTY
        }
    }

    /**
     * Insert blank characters at cursor position (ICH - CSI @)
     * Shifts existing characters on the line to the right
     */
    fun insertCharacters(count: Int) {
        val charsToInsert = count.coerceAtMost(cols - cursorCol)
        if (charsToInsert <= 0) return

        // Check integrity at insertion point (cursorCol)
        // Only clear if we are inserting at a padding cell (breaking the wide char to the left)
        // If we are at the start of a wide char, it will just be shifted right, so no need to clear.
        if (buffer[cursorRow][cursorCol].isWideCharPadding) {
            clearWideCharIntegrityAt(cursorRow, cursorCol)
        }

        // Shift characters to the right
        for (c in cols - 1 downTo cursorCol + charsToInsert) {
            buffer[cursorRow][c] = buffer[cursorRow][c - charsToInsert]
        }
        // Clear the inserted positions
        for (c in cursorCol until cursorCol + charsToInsert) {
            buffer[cursorRow][c] = TerminalCell.EMPTY
        }
    }

    /**
     * Scroll up N lines within the scroll region (SU - CSI S)
     */
    fun scrollUpLines(count: Int) {
        val linesToScroll = count.coerceAtMost(scrollBottom - scrollTop + 1)
        for (i in 0 until linesToScroll) {
            scrollUp()
        }
    }

    /**
     * Scroll down N lines within the scroll region (SD - CSI T)
     */
    fun scrollDownLines(count: Int) {
        val linesToScroll = count.coerceAtMost(scrollBottom - scrollTop + 1)
        for (i in 0 until linesToScroll) {
            scrollDown()
        }
    }

    /**
     * Erase characters at cursor position (ECH - CSI X)
     * Unlike DCH, this doesn't shift characters, just erases them
     */
    fun eraseCharacters(count: Int) {
        val charsToErase = count.coerceAtMost(cols - cursorCol)
        for (c in cursorCol until cursorCol + charsToErase) {
            buffer[cursorRow][c] = TerminalCell.EMPTY
        }
    }
}
