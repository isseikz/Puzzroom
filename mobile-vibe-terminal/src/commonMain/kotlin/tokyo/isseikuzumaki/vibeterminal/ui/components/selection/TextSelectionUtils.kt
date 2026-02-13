package tokyo.isseikuzumaki.vibeterminal.ui.components.selection

import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell

/**
 * Calculate safe cell position (0-based) from pixel coordinates.
 * Ensures the result is within valid bounds even when buffer is empty.
 *
 * @param pixelX X coordinate in pixels
 * @param pixelY Y coordinate in pixels
 * @param charWidth Width of a single character in pixels
 * @param charHeight Height of a single character in pixels
 * @param cols Number of columns in the buffer
 * @param rows Number of rows in the buffer
 * @return A pair of (col, row) clamped to valid bounds
 */
fun calculateCellPosition(
    pixelX: Float,
    pixelY: Float,
    charWidth: Float,
    charHeight: Float,
    cols: Int,
    rows: Int
): Pair<Int, Int> {
    val col = (pixelX / charWidth).toInt().coerceIn(0, (cols - 1).coerceAtLeast(0))
    val row = (pixelY / charHeight).toInt().coerceIn(0, (rows - 1).coerceAtLeast(0))
    return Pair(col, row)
}

/**
 * Calculate safe terminal position (1-based) from pixel coordinates for scroll events.
 * Terminal protocols typically use 1-based coordinates.
 * Ensures the result is within valid bounds even when buffer is empty.
 *
 * @param pixelX X coordinate in pixels
 * @param pixelY Y coordinate in pixels
 * @param charWidth Width of a single character in pixels
 * @param charHeight Height of a single character in pixels
 * @param cols Number of columns in the buffer
 * @param rows Number of rows in the buffer
 * @return A pair of (col, row) in 1-based coordinates, clamped to valid bounds
 */
fun calculateTerminalPosition(
    pixelX: Float,
    pixelY: Float,
    charWidth: Float,
    charHeight: Float,
    cols: Int,
    rows: Int
): Pair<Int, Int> {
    val col = ((pixelX / charWidth).toInt() + 1).coerceIn(1, cols.coerceAtLeast(1))
    val row = ((pixelY / charHeight).toInt() + 1).coerceIn(1, rows.coerceAtLeast(1))
    return Pair(col, row)
}

/**
 * 選択範囲からテキストを抽出する
 */
fun extractSelectedText(
    buffer: Array<Array<TerminalCell>>,
    selectionState: TextSelectionState
): String {
    if (!selectionState.hasSelection) return ""

    val start = selectionState.startPosition ?: return ""
    val end = selectionState.endPosition ?: return ""

    val result = StringBuilder()

    for (row in start.row..end.row) {
        if (row < 0 || row >= buffer.size) continue
        val rowBuffer = buffer[row]

        val colStart = if (row == start.row) start.col else 0
        val colEnd = if (row == end.row) end.col else rowBuffer.size - 1

        for (col in colStart..colEnd) {
            if (col < 0 || col >= rowBuffer.size) continue
            val cell = rowBuffer[col]
            // ワイド文字のパディングはスキップ
            if (!cell.isWideCharPadding) {
                result.append(cell.char)
            }
        }

        // 行の末尾の空白を削除し、改行を追加（最終行以外）
        if (row < end.row) {
            // 末尾の空白を削除
            while (result.isNotEmpty() && result.last() == ' ') {
                result.deleteCharAt(result.length - 1)
            }
            result.append('\n')
        }
    }

    // 最終行の末尾の空白を削除
    while (result.isNotEmpty() && result.last() == ' ') {
        result.deleteCharAt(result.length - 1)
    }

    return result.toString()
}
