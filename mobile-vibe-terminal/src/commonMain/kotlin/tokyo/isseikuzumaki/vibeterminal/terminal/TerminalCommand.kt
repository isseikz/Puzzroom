package tokyo.isseikuzumaki.vibeterminal.terminal

/**
 * Represents a parsed terminal command.
 *
 * This sealed class hierarchy represents all possible commands that can be
 * parsed from ANSI escape sequences. The parser produces these commands,
 * and the executor consumes them to update terminal state.
 */
sealed class TerminalCommand {

    // ===== Character Output =====

    /** Write a character at the current cursor position */
    data class WriteChar(val char: Char) : TerminalCommand()

    // ===== Cursor Movement =====

    /** Move cursor to absolute position (1-indexed) */
    data class MoveCursor(val row: Int, val col: Int) : TerminalCommand()

    /** Move cursor relative to current position */
    data class MoveCursorRelative(val deltaRow: Int, val deltaCol: Int) : TerminalCommand()

    /** Move cursor to absolute column (1-indexed) */
    data class MoveCursorToColumn(val col: Int) : TerminalCommand()

    /** Move cursor to absolute row (1-indexed), keeping current column */
    data class MoveCursorToRow(val row: Int) : TerminalCommand()

    /** Line feed - move cursor down, scroll if at bottom */
    data object LineFeed : TerminalCommand()

    /** Next line (NEL) - carriage return + line feed */
    data object NextLine : TerminalCommand()

    /** Carriage return - move cursor to column 1 */
    data object CarriageReturn : TerminalCommand()

    /** Reverse index - move cursor up, scroll if at top */
    data object ReverseIndex : TerminalCommand()

    /** Tab - move cursor to next tab stop */
    data object Tab : TerminalCommand()

    // ===== Cursor Save/Restore =====

    /** Save current cursor position */
    data object SaveCursor : TerminalCommand()

    /** Restore saved cursor position */
    data object RestoreCursor : TerminalCommand()

    // ===== Erase Operations =====

    /** Erase in display (mode: 0=to end, 1=to start, 2/3=all) */
    data class EraseInDisplay(val mode: Int) : TerminalCommand()

    /** Erase in line (mode: 0=to end, 1=to start, 2=all) */
    data class EraseInLine(val mode: Int) : TerminalCommand()

    /** Erase N characters at cursor (ECH) */
    data class EraseCharacters(val count: Int) : TerminalCommand()

    // ===== Line Operations =====

    /** Insert N blank lines at cursor (IL) */
    data class InsertLines(val count: Int) : TerminalCommand()

    /** Delete N lines at cursor (DL) */
    data class DeleteLines(val count: Int) : TerminalCommand()

    /** Insert N blank characters at cursor (ICH) */
    data class InsertCharacters(val count: Int) : TerminalCommand()

    /** Delete N characters at cursor (DCH) */
    data class DeleteCharacters(val count: Int) : TerminalCommand()

    // ===== Scroll Operations =====

    /** Scroll up N lines (SU) */
    data class ScrollUp(val lines: Int) : TerminalCommand()

    /** Scroll down N lines (SD) */
    data class ScrollDown(val lines: Int) : TerminalCommand()

    /** Set scroll region (DECSTBM) - 1-indexed */
    data class SetScrollRegion(val top: Int, val bottom: Int) : TerminalCommand()

    /** Reset scroll region to full screen */
    data object ResetScrollRegion : TerminalCommand()

    // ===== Attributes =====

    /** Set graphics mode (SGR) - colors and styles */
    data class SetGraphicsMode(val params: List<Int>) : TerminalCommand()

    /** Set character set (SCS) */
    data class SetCharset(val index: Int, val charset: TerminalCharset) : TerminalCommand()

    // ===== Mode Settings =====

    /** Set or reset a terminal mode (DECSET/DECRST) */
    data class SetMode(val mode: Int, val enabled: Boolean) : TerminalCommand()

    /** Set or reset multiple terminal modes (DECSET/DECRST with multiple parameters) */
    data class SetModes(val modes: List<Int>, val enabled: Boolean) : TerminalCommand()

    // ===== Other =====

    /** Bell - trigger audio/visual alert */
    data object Bell : TerminalCommand()

    /** No operation - command was parsed but has no effect */
    data object Noop : TerminalCommand()
}

/**
 * Character set identifiers for SCS (Select Character Set)
 */
enum class TerminalCharset {
    ASCII,
    DEC_SPECIAL_GRAPHICS
}
