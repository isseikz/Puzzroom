package tokyo.isseikuzumaki.vibeterminal.ui.components.selection

import androidx.compose.runtime.Immutable

/**
 * ターミナル上の位置を表すデータクラス
 * @param row 行番号（0-indexed）
 * @param col 列番号（0-indexed）
 */
@Immutable
data class TerminalPosition(
    val row: Int,
    val col: Int
) : Comparable<TerminalPosition> {
    override fun compareTo(other: TerminalPosition): Int {
        return if (row != other.row) {
            row.compareTo(other.row)
        } else {
            col.compareTo(other.col)
        }
    }
}

/**
 * テキスト選択の状態を管理するデータクラス
 */
@Immutable
data class TextSelectionState(
    /** 選択モードが有効かどうか */
    val isSelecting: Boolean = false,
    /** 選択の開始位置（ドラッグ開始点） */
    val anchorPosition: TerminalPosition? = null,
    /** 選択の終了位置（現在のドラッグ位置） */
    val currentPosition: TerminalPosition? = null
) {
    /**
     * 選択範囲が有効かどうか
     */
    val hasSelection: Boolean
        get() = isSelecting && anchorPosition != null && currentPosition != null

    /**
     * 選択範囲の開始位置（常に小さい方）
     */
    val startPosition: TerminalPosition?
        get() {
            if (anchorPosition == null || currentPosition == null) return null
            return minOf(anchorPosition, currentPosition)
        }

    /**
     * 選択範囲の終了位置（常に大きい方）
     */
    val endPosition: TerminalPosition?
        get() {
            if (anchorPosition == null || currentPosition == null) return null
            return maxOf(anchorPosition, currentPosition)
        }

    /**
     * 指定したセルが選択範囲内かどうかを判定
     */
    fun isCellSelected(row: Int, col: Int): Boolean {
        if (!hasSelection) return false
        val start = startPosition ?: return false
        val end = endPosition ?: return false

        // 単一行の選択
        if (start.row == end.row) {
            return row == start.row && col >= start.col && col <= end.col
        }

        // 複数行の選択
        return when (row) {
            start.row -> col >= start.col
            end.row -> col <= end.col
            else -> row > start.row && row < end.row
        }
    }

    companion object {
        val Empty = TextSelectionState()
    }
}
