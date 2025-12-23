package tokyo.isseikuzumaki.vibeterminal.terminal

import androidx.compose.ui.graphics.Color

/**
 * Represents a single cell in the terminal screen buffer.
 * Each cell contains a character and its styling attributes.
 */
data class TerminalCell(
    val char: Char = ' ',
    val foregroundColor: Color = Color.White,
    val backgroundColor: Color = Color.Black,
    val isBold: Boolean = false,
    val isUnderline: Boolean = false,
    val isReverse: Boolean = false
) {
    companion object {
        val EMPTY = TerminalCell()
    }
}
