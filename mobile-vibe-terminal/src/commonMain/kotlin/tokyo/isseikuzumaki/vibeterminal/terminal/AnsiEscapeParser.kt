package tokyo.isseikuzumaki.vibeterminal.terminal

/**
 * Parses ANSI escape sequences and updates the terminal screen buffer.
 * Implements a subset of VT100/ANSI terminal control sequences.
 */
class AnsiEscapeParser(private val screenBuffer: TerminalScreenBuffer) {

    private enum class State {
        NORMAL,
        ESCAPE,
        CSI,
        OSC,
        SCS  // Character set selection
    }

    private var state = State.NORMAL
    private val paramBuffer = StringBuilder()
    private var csiCommand = ' '
    private var csiIntermediate = ' '  // Intermediate byte for CSI sequences (e.g., Space in "CSI Ps SP q")
    private var scsChar = ' '  // Character set selector (e.g., '(' or ')')

    /**
     * Process incoming text and update the screen buffer
     */
    fun processText(text: String) {
        for (char in text) {
            processChar(char)
        }
    }

    private fun processChar(char: Char) {
        when (state) {
            State.NORMAL -> handleNormalChar(char)
            State.ESCAPE -> handleEscapeChar(char)
            State.CSI -> handleCsiChar(char)
            State.OSC -> handleOscChar(char)
            State.SCS -> handleScsChar(char)
        }
    }

    private fun handleNormalChar(char: Char) {
        when (char) {
            '\u001B' -> {  // ESC
                state = State.ESCAPE
                paramBuffer.clear()
            }
            '\r' -> {  // Carriage return
                screenBuffer.moveCursorToColumn(1)
            }
            '\n' -> {  // Line feed
                screenBuffer.moveCursorRelative(1, 0)
            }
            '\b' -> {  // Backspace
                screenBuffer.moveCursorRelative(0, -1)
            }
            '\t' -> {  // Tab
                val nextTab = ((screenBuffer.cursorCol / 8) + 1) * 8
                screenBuffer.moveCursorToColumn(nextTab + 1)
            }
            '\u0007' -> {  // Bell (ignore)
            }
            else -> {
                if (char >= ' ') {  // Printable characters
                    screenBuffer.writeChar(char)
                }
            }
        }
    }

    private fun handleEscapeChar(char: Char) {
        when (char) {
            '[' -> {  // CSI (Control Sequence Introducer)
                state = State.CSI
                paramBuffer.clear()
                csiIntermediate = ' '
            }
            ']' -> {  // OSC (Operating System Command)
                state = State.OSC
                paramBuffer.clear()
            }
            '(' -> {  // SCS - Select character set into G0
                scsChar = '('
                state = State.SCS
            }
            ')' -> {  // SCS - Select character set into G1
                scsChar = ')'
                state = State.SCS
            }
            'M' -> {  // Reverse index (move up and scroll if needed)
                screenBuffer.moveCursorRelative(-1, 0)
                state = State.NORMAL
            }
            '7' -> {  // Save cursor position
                screenBuffer.saveCursor()
                state = State.NORMAL
            }
            '8' -> {  // Restore cursor position
                screenBuffer.restoreCursor()
                state = State.NORMAL
            }
            '=' -> {  // Application keypad mode (ignore)
                state = State.NORMAL
            }
            '>' -> {  // Normal keypad mode (ignore)
                state = State.NORMAL
            }
            else -> {
                // Unknown escape sequence, return to normal
                state = State.NORMAL
            }
        }
    }

    private fun handleCsiChar(char: Char) {
        when {
            char in '0'..'9' || char == ';' || char == '?' -> {
                paramBuffer.append(char)
            }
            char == ' ' -> {
                // Space is an intermediate byte (e.g., in "CSI Ps SP q" for DECSCUSR)
                csiIntermediate = char
            }
            char in 'A'..'Z' || char in 'a'..'z' || char == '@' || char == '`' -> {
                csiCommand = char
                executeCsiCommand()
                state = State.NORMAL
            }
            else -> {
                // Invalid CSI sequence
                state = State.NORMAL
            }
        }
    }

    private fun handleOscChar(char: Char) {
        when (char) {
            '\u0007' -> {  // Bell terminates OSC
                // Process OSC command if needed (title change, etc.)
                state = State.NORMAL
            }
            '\u001B' -> {  // ESC might terminate OSC
                state = State.ESCAPE
            }
            else -> {
                paramBuffer.append(char)
            }
        }
    }

    private fun handleScsChar(char: Char) {
        // Handle character set selection
        // ESC ( B = Select ASCII into G0
        // ESC ( 0 = Select DEC Special Graphics into G0
        // ESC ) B = Select ASCII into G1
        // ESC ) 0 = Select DEC Special Graphics into G1
        // For now, we just acknowledge and ignore these sequences
        // In a full implementation, we would map special graphics characters
        state = State.NORMAL
    }

    private fun executeCsiCommand() {
        val paramsStr = paramBuffer.toString()
        val isPrivate = paramsStr.startsWith("?")
        // Strip '?' for parsing numbers
        val cleanParamsStr = if (isPrivate) paramsStr.substring(1) else paramsStr

        val params = if (cleanParamsStr.isNotEmpty()) {
            cleanParamsStr.split(';').mapNotNull { it.toIntOrNull() }
        } else {
            emptyList()
        }

        // Handle sequences with intermediate bytes (e.g., CSI Ps SP q for DECSCUSR)
        if (csiIntermediate == ' ' && csiCommand == 'q') {
            // DECSCUSR - Set Cursor Style
            // Ps values:
            //   0, 1 = blinking block
            //   2 = steady block
            //   3 = blinking underline
            //   4 = steady underline
            //   5 = blinking bar
            //   6 = steady bar
            // For now, we just consume this command to prevent 'q' from appearing on screen
            // TODO: In the future, we could update cursor appearance in the UI layer
            return
        }

        when (csiCommand) {
            'A' -> {  // Cursor up
                val n = params.getOrElse(0) { 1 }
                screenBuffer.moveCursorRelative(-n, 0)
            }
            'B' -> {  // Cursor down
                val n = params.getOrElse(0) { 1 }
                screenBuffer.moveCursorRelative(n, 0)
            }
            'C' -> {  // Cursor forward
                val n = params.getOrElse(0) { 1 }
                screenBuffer.moveCursorRelative(0, n)
            }
            'D' -> {  // Cursor backward
                val n = params.getOrElse(0) { 1 }
                screenBuffer.moveCursorRelative(0, -n)
            }
            'H', 'f' -> {  // Cursor position
                val row = params.getOrElse(0) { 1 }
                val col = params.getOrElse(1) { 1 }
                screenBuffer.moveCursor(row, col)
            }
            'J' -> {  // Erase in display
                when (params.getOrElse(0) { 0 }) {
                    0 -> screenBuffer.clearToEndOfScreen()
                    1 -> screenBuffer.clearToStartOfScreen()
                    2, 3 -> screenBuffer.clearScreen()
                }
            }
            'K' -> {  // Erase in line
                when (params.getOrElse(0) { 0 }) {
                    0 -> screenBuffer.clearToEndOfLine()
                    1 -> screenBuffer.clearToStartOfLine()
                    2 -> screenBuffer.clearLine()
                }
            }
            'L' -> {  // Insert lines
                val n = params.getOrElse(0) { 1 }
                screenBuffer.insertLines(n)
            }
            'M' -> {  // Delete lines
                val n = params.getOrElse(0) { 1 }
                screenBuffer.deleteLines(n)
            }
            'G', '`' -> {  // Cursor horizontal absolute
                val col = params.getOrElse(0) { 1 }
                screenBuffer.moveCursorToColumn(col)
            }
            'd' -> {  // Cursor vertical absolute
                val row = params.getOrElse(0) { 1 }
                screenBuffer.moveCursor(row, screenBuffer.cursorCol + 1)
            }
            'm' -> {  // Select Graphic Rendition (colors, styles)
                if (params.isEmpty()) {
                    screenBuffer.setGraphicsMode(listOf(0))
                } else {
                    screenBuffer.setGraphicsMode(params)
                }
            }
            's' -> {  // Save cursor position
                screenBuffer.saveCursor()
            }
            'u' -> {  // Restore cursor position
                screenBuffer.restoreCursor()
            }
            'h' -> {  // Set Mode (DECSET)
                 if (isPrivate) {
                     when {
                         params.contains(1049) -> {
                             // Save cursor and switch to alternate screen buffer, clearing it
                             screenBuffer.saveCursor()
                             screenBuffer.useAlternateScreenBuffer()
                         }
                         params.contains(2004) -> {
                             // Enable bracketed paste mode (ignore, we don't need to track this)
                         }
                     }
                 }
            }
            'l' -> {  // Reset Mode (DECRST)
                 if (isPrivate) {
                     when {
                         params.contains(1049) -> {
                             // Switch to primary screen buffer and restore cursor
                             screenBuffer.usePrimaryScreenBuffer()
                             screenBuffer.restoreCursor()
                         }
                         params.contains(2004) -> {
                             // Disable bracketed paste mode (ignore)
                         }
                     }
                 }
            }
            'r' -> {  // DECSTBM - Set scrolling region (top and bottom margins)
                if (params.isEmpty()) {
                    // No parameters means reset to full screen
                    screenBuffer.resetScrollRegion()
                } else {
                    val top = params.getOrElse(0) { 1 }
                    val bottom = params.getOrElse(1) { 24 }  // Default based on current buffer size
                    screenBuffer.setScrollRegion(top, bottom)
                }
            }
        }
    }

    private fun parseParams(): List<Int> {
        // logic moved to executeCsiCommand to handle '?'
        return emptyList() 
    }
}
