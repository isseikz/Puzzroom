package tokyo.isseikuzumaki.vibeterminal.ui.components.selection

import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell

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
