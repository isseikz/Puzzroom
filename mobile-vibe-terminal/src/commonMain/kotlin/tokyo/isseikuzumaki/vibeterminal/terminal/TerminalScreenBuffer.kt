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
    // 2D array representing the screen buffer
    private var buffer: Array<Array<TerminalCell>> = createBuffer(cols, rows)

    // Cursor position (0-indexed)
    var cursorRow: Int = 0
        private set
    var cursorCol: Int = 0
        private set

    // Current text styling
    private var currentForeground = Color.White
    private var currentBackground = Color.Black
    private var isBold = false
    private var isUnderline = false
    private var isReverse = false

    // Saved cursor position (for save/restore cursor operations)
    private var savedCursorRow = 0
    private var savedCursorCol = 0

    private fun createBuffer(cols: Int, rows: Int): Array<Array<TerminalCell>> {
        return Array(rows) { Array(cols) { TerminalCell.EMPTY } }
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
                if (cursorRow >= rows) {
                    scrollUp()
                    cursorRow = rows - 1
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
        buffer = createBuffer(cols, rows)
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
     * Scroll the screen up by one line
     */
    private fun scrollUp() {
        // Move all lines up
        for (r in 0 until rows - 1) {
            buffer[r] = buffer[r + 1]
        }
        // Clear the last line
        buffer[rows - 1] = Array(cols) { TerminalCell.EMPTY }
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

        val newBuffer = createBuffer(newCols, newRows)

        // Copy existing content
        val copyRows = minOf(rows, newRows)
        val copyCols = minOf(cols, newCols)

        for (r in 0 until copyRows) {
            for (c in 0 until copyCols) {
                newBuffer[r][c] = buffer[r][c]
            }
        }

        cols = newCols
        rows = newRows
        buffer = newBuffer

        // Adjust cursor position if needed
        cursorRow = cursorRow.coerceIn(0, rows - 1)
        cursorCol = cursorCol.coerceIn(0, cols - 1)
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
