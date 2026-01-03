package tokyo.isseikuzumaki.vibeterminal.ui.components.selection

import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell
import kotlin.test.Test
import kotlin.test.assertEquals

class TextSelectionUtilsTest {

    private fun createBuffer(vararg lines: String): Array<Array<TerminalCell>> {
        val maxCols = lines.maxOfOrNull { it.length } ?: 0
        return lines.map { line ->
            (0 until maxCols).map { col ->
                if (col < line.length) {
                    TerminalCell(char = line[col])
                } else {
                    TerminalCell(char = ' ')
                }
            }.toTypedArray()
        }.toTypedArray()
    }

    @Test
    fun `選択範囲がない場合は空文字列を返す`() {
        val buffer = createBuffer("Hello World")
        val state = TextSelectionState.Empty

        val result = extractSelectedText(buffer, state)

        assertEquals("", result)
    }

    @Test
    fun `単一行の一部を選択した場合、その部分のテキストを返す`() {
        val buffer = createBuffer("Hello World")
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(0, 0),
            currentPosition = TerminalPosition(0, 4)
        )

        val result = extractSelectedText(buffer, state)

        assertEquals("Hello", result)
    }

    @Test
    fun `単一行の全体を選択した場合、末尾の空白を除去する`() {
        val buffer = createBuffer("Hello   ")
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(0, 0),
            currentPosition = TerminalPosition(0, 7)
        )

        val result = extractSelectedText(buffer, state)

        assertEquals("Hello", result)
    }

    @Test
    fun `複数行を選択した場合、改行で区切られたテキストを返す`() {
        val buffer = createBuffer(
            "Line 1",
            "Line 2",
            "Line 3"
        )
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(0, 0),
            currentPosition = TerminalPosition(2, 5)
        )

        val result = extractSelectedText(buffer, state)

        assertEquals("Line 1\nLine 2\nLine 3", result)
    }

    @Test
    fun `複数行選択で各行の末尾空白を除去する`() {
        val buffer = createBuffer(
            "AAA   ",
            "BBB   ",
            "CCC   "
        )
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(0, 0),
            currentPosition = TerminalPosition(2, 5)
        )

        val result = extractSelectedText(buffer, state)

        assertEquals("AAA\nBBB\nCCC", result)
    }

    @Test
    fun `複数行で途中から選択を開始した場合`() {
        val buffer = createBuffer(
            "Hello World",
            "Foo Bar"
        )
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(0, 6),
            currentPosition = TerminalPosition(1, 2)
        )

        val result = extractSelectedText(buffer, state)

        assertEquals("World\nFoo", result)
    }

    @Test
    fun `逆方向に選択しても正しいテキストを返す`() {
        val buffer = createBuffer("Hello World")
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(0, 4),
            currentPosition = TerminalPosition(0, 0)
        )

        val result = extractSelectedText(buffer, state)

        assertEquals("Hello", result)
    }

    @Test
    fun `空のバッファでも例外を発生させない`() {
        val buffer = arrayOf<Array<TerminalCell>>()
        val state = TextSelectionState(
            isSelecting = true,
            anchorPosition = TerminalPosition(0, 0),
            currentPosition = TerminalPosition(0, 5)
        )

        val result = extractSelectedText(buffer, state)

        assertEquals("", result)
    }
}
