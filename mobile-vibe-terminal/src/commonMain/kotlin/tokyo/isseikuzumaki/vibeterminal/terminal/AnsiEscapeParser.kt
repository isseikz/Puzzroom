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
        OSC
    }

    private var state = State.NORMAL
    private val paramBuffer = StringBuilder()
    private var csiCommand = ' '

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
            }
            ']' -> {  // OSC (Operating System Command)
                state = State.OSC
                paramBuffer.clear()
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
            char in '0'..'9' || char == ';' -> {
                paramBuffer.append(char)
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

    private fun executeCsiCommand() {
        val params = parseParams()

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
            'h', 'l' -> {  // Set/Reset mode (ignore for now)
                // These control various terminal modes
            }
            'r' -> {  // Set scrolling region (ignore for now)
                // This sets the top and bottom margins
            }
        }
    }

    private fun parseParams(): List<Int> {
        if (paramBuffer.isEmpty()) return emptyList()

        return paramBuffer.toString()
            .split(';')
            .mapNotNull { it.toIntOrNull() }
    }
}
