package tokyo.isseikuzumaki.vibeterminal.terminal

/**
 * Executes terminal commands on the screen buffer and related components.
 *
 * This class is responsible for interpreting TerminalCommand instances and
 * applying the corresponding operations to the TerminalScreenBuffer and
 * MouseInputHandler.
 *
 * Separation of concerns:
 * - AnsiEscapeParser: Parses byte stream into TerminalCommand
 * - TerminalCommandExecutor: Executes commands on terminal state
 */
class TerminalCommandExecutor(
    private val screenBuffer: TerminalScreenBuffer,
    private val mouseInputHandler: MouseInputHandler? = null
) {

    /**
     * Execute a single terminal command
     */
    fun execute(command: TerminalCommand) {
        when (command) {
            // Character output
            is TerminalCommand.WriteChar -> screenBuffer.writeChar(command.char)

            // Cursor movement
            is TerminalCommand.MoveCursor -> screenBuffer.moveCursor(command.row, command.col)
            is TerminalCommand.MoveCursorRelative -> screenBuffer.moveCursorRelative(command.deltaRow, command.deltaCol)
            is TerminalCommand.MoveCursorToColumn -> screenBuffer.moveCursorToColumn(command.col)
            is TerminalCommand.MoveCursorToRow -> {
                // Move to row while keeping current column (1-indexed row)
                screenBuffer.moveCursor(command.row, screenBuffer.cursorCol + 1)
            }
            TerminalCommand.LineFeed -> screenBuffer.lineFeed()
            TerminalCommand.NextLine -> {
                // NEL: Carriage return + line feed
                screenBuffer.moveCursorToColumn(1)
                screenBuffer.lineFeed()
            }
            TerminalCommand.CarriageReturn -> screenBuffer.moveCursorToColumn(1)
            TerminalCommand.ReverseIndex -> screenBuffer.reverseIndex()
            TerminalCommand.Tab -> {
                val nextTab = ((screenBuffer.cursorCol / 8) + 1) * 8
                screenBuffer.moveCursorToColumn(nextTab + 1)
            }

            // Cursor save/restore
            TerminalCommand.SaveCursor -> screenBuffer.saveCursor()
            TerminalCommand.RestoreCursor -> screenBuffer.restoreCursor()

            // Erase operations
            is TerminalCommand.EraseInDisplay -> {
                when (command.mode) {
                    0 -> screenBuffer.clearToEndOfScreen()
                    1 -> screenBuffer.clearToStartOfScreen()
                    2, 3 -> screenBuffer.clearScreen()
                }
            }
            is TerminalCommand.EraseInLine -> {
                when (command.mode) {
                    0 -> screenBuffer.clearToEndOfLine()
                    1 -> screenBuffer.clearToStartOfLine()
                    2 -> screenBuffer.clearLine()
                }
            }
            is TerminalCommand.EraseCharacters -> screenBuffer.eraseCharacters(command.count)

            // Line operations
            is TerminalCommand.InsertLines -> screenBuffer.insertLines(command.count)
            is TerminalCommand.DeleteLines -> screenBuffer.deleteLines(command.count)
            is TerminalCommand.InsertCharacters -> screenBuffer.insertCharacters(command.count)
            is TerminalCommand.DeleteCharacters -> screenBuffer.deleteCharacters(command.count)

            // Scroll operations
            is TerminalCommand.ScrollUp -> screenBuffer.scrollUpLines(command.lines)
            is TerminalCommand.ScrollDown -> screenBuffer.scrollDownLines(command.lines)
            is TerminalCommand.SetScrollRegion -> screenBuffer.setScrollRegion(command.top, command.bottom)
            TerminalCommand.ResetScrollRegion -> screenBuffer.resetScrollRegion()

            // Attributes
            is TerminalCommand.SetGraphicsMode -> screenBuffer.setGraphicsMode(command.params)
            is TerminalCommand.SetCharset -> {
                val charset = when (command.charset) {
                    TerminalCharset.ASCII -> TerminalScreenBuffer.Charset.ASCII
                    TerminalCharset.DEC_SPECIAL_GRAPHICS -> TerminalScreenBuffer.Charset.DEC_SPECIAL_GRAPHICS
                }
                screenBuffer.setCharset(command.index, charset)
            }

            // Mode settings (DECSET/DECRST)
            is TerminalCommand.SetMode -> handleSetMode(command.mode, command.enabled)
            is TerminalCommand.SetModes -> {
                command.modes.forEach { mode ->
                    handleSetMode(mode, command.enabled)
                }
            }

            // Other
            TerminalCommand.Bell -> { /* Bell: could trigger haptic feedback or sound */ }
            TerminalCommand.Noop -> { /* No operation */ }
        }
    }

    /**
     * Execute multiple commands in sequence
     */
    fun executeAll(commands: List<TerminalCommand>) {
        commands.forEach { execute(it) }
    }

    /**
     * Handle DECSET/DECRST mode changes
     */
    private fun handleSetMode(mode: Int, enabled: Boolean) {
        when (mode) {
            // Cursor visibility (DECTCEM)
            25 -> screenBuffer.setCursorVisible(enabled)

            // Mouse tracking modes
            1000, 1002, 1003 -> {
                if (enabled) {
                    mouseInputHandler?.enableTrackingMode(mode)
                } else {
                    mouseInputHandler?.disableTrackingMode(mode)
                }
            }

            // SGR extended mouse encoding
            1006 -> {
                if (enabled) {
                    mouseInputHandler?.enableSgrEncoding()
                } else {
                    mouseInputHandler?.disableSgrEncoding()
                }
            }

            // Alternate screen buffer
            1049 -> {
                if (enabled) {
                    screenBuffer.saveCursor()
                    screenBuffer.useAlternateScreenBuffer()
                    mouseInputHandler?.isAlternateScreen = true
                } else {
                    screenBuffer.usePrimaryScreenBuffer()
                    screenBuffer.restoreCursor()
                    mouseInputHandler?.isAlternateScreen = false
                }
            }

            // Bracketed paste mode (ignored for now)
            2004 -> { /* No-op */ }
        }
    }
}
