package tokyo.isseikuzumaki.vibeterminal.terminal

/**
 * Parses ANSI escape sequences and produces terminal commands.
 * Implements a subset of VT100/ANSI terminal control sequences.
 *
 * This parser is a pure state machine that converts input text into
 * a list of TerminalCommand objects. It does not directly modify
 * terminal state - that responsibility belongs to TerminalCommandExecutor.
 *
 * @param defaultScrollBottom Default bottom row for scroll region reset (0-indexed rows - 1)
 */
class AnsiEscapeParser(
    private val defaultScrollBottom: Int = 23
) {

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
     * Process incoming text and return list of terminal commands.
     *
     * @param text The text to parse
     * @return List of TerminalCommand to be executed
     */
    fun parse(text: String): List<TerminalCommand> {
        val commands = mutableListOf<TerminalCommand>()
        for (char in text) {
            parseChar(char)?.let { commands.add(it) }
        }
        return commands
    }

    private fun parseChar(char: Char): TerminalCommand? {
        return when (state) {
            State.NORMAL -> handleNormalChar(char)
            State.ESCAPE -> handleEscapeChar(char)
            State.CSI -> handleCsiChar(char)
            State.OSC -> handleOscChar(char)
            State.SCS -> handleScsChar(char)
        }
    }

    private fun handleNormalChar(char: Char): TerminalCommand? {
        return when (char) {
            '\u001B' -> {  // ESC
                state = State.ESCAPE
                paramBuffer.clear()
                null
            }
            '\r' -> TerminalCommand.CarriageReturn
            '\n' -> TerminalCommand.LineFeed
            '\b' -> TerminalCommand.MoveCursorRelative(0, -1)
            '\t' -> TerminalCommand.Tab
            '\u0007' -> TerminalCommand.Bell
            else -> {
                if (char >= ' ') {  // Printable characters
                    TerminalCommand.WriteChar(char)
                } else {
                    null
                }
            }
        }
    }

    private fun handleEscapeChar(char: Char): TerminalCommand? {
        return when (char) {
            '[' -> {  // CSI (Control Sequence Introducer)
                state = State.CSI
                paramBuffer.clear()
                csiIntermediate = ' '
                null
            }
            ']' -> {  // OSC (Operating System Command)
                state = State.OSC
                paramBuffer.clear()
                null
            }
            '(' -> {  // SCS - Select character set into G0
                scsChar = '('
                state = State.SCS
                null
            }
            ')' -> {  // SCS - Select character set into G1
                scsChar = ')'
                state = State.SCS
                null
            }
            'D' -> {  // Index (IND) - same as LF
                state = State.NORMAL
                TerminalCommand.LineFeed
            }
            'E' -> {  // Next Line (NEL) - CR + LF
                state = State.NORMAL
                TerminalCommand.NextLine
            }
            'M' -> {  // Reverse Index (RI)
                state = State.NORMAL
                TerminalCommand.ReverseIndex
            }
            '7' -> {  // Save cursor position
                state = State.NORMAL
                TerminalCommand.SaveCursor
            }
            '8' -> {  // Restore cursor position
                state = State.NORMAL
                TerminalCommand.RestoreCursor
            }
            '=' -> {  // Application keypad mode (ignore)
                state = State.NORMAL
                TerminalCommand.Noop
            }
            '>' -> {  // Normal keypad mode (ignore)
                state = State.NORMAL
                TerminalCommand.Noop
            }
            else -> {
                // Unknown escape sequence, return to normal
                state = State.NORMAL
                null
            }
        }
    }

    private fun handleCsiChar(char: Char): TerminalCommand? {
        return when {
            char in '0'..'9' || char == ';' || char == '?' -> {
                paramBuffer.append(char)
                null
            }
            char == ' ' -> {
                // Space is an intermediate byte (e.g., in "CSI Ps SP q" for DECSCUSR)
                csiIntermediate = char
                null
            }
            char in 'A'..'Z' || char in 'a'..'z' || char == '@' || char == '`' -> {
                csiCommand = char
                val command = buildCsiCommand()
                state = State.NORMAL
                command
            }
            else -> {
                // Invalid CSI sequence
                state = State.NORMAL
                null
            }
        }
    }

    private fun handleOscChar(char: Char): TerminalCommand? {
        return when (char) {
            '\u0007' -> {  // Bell terminates OSC
                // Process OSC command if needed (title change, etc.)
                state = State.NORMAL
                TerminalCommand.Noop
            }
            '\u001B' -> {  // ESC might terminate OSC
                state = State.ESCAPE
                null
            }
            else -> {
                paramBuffer.append(char)
                null
            }
        }
    }

    private fun handleScsChar(char: Char): TerminalCommand? {
        val charset = when (char) {
            '0' -> TerminalCharset.DEC_SPECIAL_GRAPHICS
            'B' -> TerminalCharset.ASCII
            else -> TerminalCharset.ASCII // Default/Fallback
        }

        val index = when (scsChar) {
            '(' -> 0 // G0
            ')' -> 1 // G1
            else -> 0
        }

        state = State.NORMAL
        return TerminalCommand.SetCharset(index, charset)
    }

    private fun buildCsiCommand(): TerminalCommand {
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
            // DECSCUSR - Set Cursor Style (ignored for now)
            return TerminalCommand.Noop
        }

        return when (csiCommand) {
            'A' -> {  // Cursor up
                val n = params.getOrElse(0) { 1 }
                TerminalCommand.MoveCursorRelative(-n, 0)
            }
            'B' -> {  // Cursor down
                val n = params.getOrElse(0) { 1 }
                TerminalCommand.MoveCursorRelative(n, 0)
            }
            'C' -> {  // Cursor forward
                val n = params.getOrElse(0) { 1 }
                TerminalCommand.MoveCursorRelative(0, n)
            }
            'D' -> {  // Cursor backward
                val n = params.getOrElse(0) { 1 }
                TerminalCommand.MoveCursorRelative(0, -n)
            }
            'H', 'f' -> {  // Cursor position
                val row = params.getOrElse(0) { 1 }
                val col = params.getOrElse(1) { 1 }
                TerminalCommand.MoveCursor(row, col)
            }
            'J' -> {  // Erase in display
                val mode = params.getOrElse(0) { 0 }
                TerminalCommand.EraseInDisplay(mode)
            }
            'K' -> {  // Erase in line
                val mode = params.getOrElse(0) { 0 }
                TerminalCommand.EraseInLine(mode)
            }
            'L' -> {  // Insert lines
                val n = params.getOrElse(0) { 1 }
                TerminalCommand.InsertLines(n)
            }
            'M' -> {  // Delete lines
                val n = params.getOrElse(0) { 1 }
                TerminalCommand.DeleteLines(n)
            }
            'P' -> {  // DCH - Delete character(s)
                val n = params.getOrElse(0) { 1 }
                TerminalCommand.DeleteCharacters(n)
            }
            '@' -> {  // ICH - Insert blank character(s)
                val n = params.getOrElse(0) { 1 }
                TerminalCommand.InsertCharacters(n)
            }
            'S' -> {  // SU - Scroll up
                val n = params.getOrElse(0) { 1 }
                TerminalCommand.ScrollUp(n)
            }
            'T' -> {  // SD - Scroll down
                val n = params.getOrElse(0) { 1 }
                TerminalCommand.ScrollDown(n)
            }
            'X' -> {  // ECH - Erase character(s)
                val n = params.getOrElse(0) { 1 }
                TerminalCommand.EraseCharacters(n)
            }
            'G', '`' -> {  // Cursor horizontal absolute
                val col = params.getOrElse(0) { 1 }
                TerminalCommand.MoveCursorToColumn(col)
            }
            'd' -> {  // Cursor vertical absolute (VPA)
                val row = params.getOrElse(0) { 1 }
                TerminalCommand.MoveCursorToRow(row)
            }
            'm' -> {  // Select Graphic Rendition (colors, styles)
                if (params.isEmpty()) {
                    TerminalCommand.SetGraphicsMode(listOf(0))
                } else {
                    TerminalCommand.SetGraphicsMode(params)
                }
            }
            's' -> {  // Save cursor position
                TerminalCommand.SaveCursor
            }
            'u' -> {  // Restore cursor position
                TerminalCommand.RestoreCursor
            }
            'h' -> {  // Set Mode (DECSET)
                if (isPrivate && params.isNotEmpty()) {
                    if (params.size == 1) {
                        TerminalCommand.SetMode(params[0], enabled = true)
                    } else {
                        TerminalCommand.SetModes(params, enabled = true)
                    }
                } else {
                    TerminalCommand.Noop
                }
            }
            'l' -> {  // Reset Mode (DECRST)
                if (isPrivate && params.isNotEmpty()) {
                    if (params.size == 1) {
                        TerminalCommand.SetMode(params[0], enabled = false)
                    } else {
                        TerminalCommand.SetModes(params, enabled = false)
                    }
                } else {
                    TerminalCommand.Noop
                }
            }
            'r' -> {  // DECSTBM - Set scrolling region (top and bottom margins)
                if (params.isEmpty()) {
                    TerminalCommand.ResetScrollRegion
                } else {
                    val top = params.getOrElse(0) { 1 }
                    val bottom = params.getOrElse(1) { defaultScrollBottom + 1 }
                    TerminalCommand.SetScrollRegion(top, bottom)
                }
            }
            else -> TerminalCommand.Noop
        }
    }
}
