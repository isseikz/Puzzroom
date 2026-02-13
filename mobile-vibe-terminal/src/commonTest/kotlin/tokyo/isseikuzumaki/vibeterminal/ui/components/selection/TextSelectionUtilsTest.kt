package tokyo.isseikuzumaki.vibeterminal.ui.components.selection

import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell
import kotlin.test.Test
import kotlin.test.assertEquals

class TextSelectionUtilsTest {

    // ========================================
    // Regression Tests for Issue #163
    // calculateTerminalPosition and calculateCellPosition
    // must not throw IllegalArgumentException when buffer is empty
    // ========================================

    @Test
    fun `calculateTerminalPosition with empty buffer does not throw`() {
        // This test reproduces Issue #163: IllegalArgumentException when cols=0 or rows=0
        // The bug was: coerceIn(1, 0) throws because max < min
        val (col, row) = calculateTerminalPosition(
            pixelX = 100f,
            pixelY = 100f,
            charWidth = 10f,
            charHeight = 20f,
            cols = 0,
            rows = 0
        )
        // With empty buffer, should return (1, 1) as minimum valid terminal position
        assertEquals(1, col)
        assertEquals(1, row)
    }

    @Test
    fun `calculateTerminalPosition with zero cols does not throw`() {
        val (col, row) = calculateTerminalPosition(
            pixelX = 100f,
            pixelY = 100f,
            charWidth = 10f,
            charHeight = 20f,
            cols = 0,
            rows = 10
        )
        assertEquals(1, col)
        assertEquals(6, row) // 100/20 + 1 = 6, clamped to 1..10
    }

    @Test
    fun `calculateTerminalPosition with zero rows does not throw`() {
        val (col, row) = calculateTerminalPosition(
            pixelX = 100f,
            pixelY = 100f,
            charWidth = 10f,
            charHeight = 20f,
            cols = 10,
            rows = 0
        )
        assertEquals(10, col) // 100/10 + 1 = 11, clamped to 1..10
        assertEquals(1, row)
    }

    @Test
    fun `calculateTerminalPosition returns valid 1-based position`() {
        val (col, row) = calculateTerminalPosition(
            pixelX = 25f,
            pixelY = 45f,
            charWidth = 10f,
            charHeight = 20f,
            cols = 80,
            rows = 24
        )
        // 25/10 + 1 = 3, 45/20 + 1 = 3
        assertEquals(3, col)
        assertEquals(3, row)
    }

    @Test
    fun `calculateTerminalPosition clamps to max bounds`() {
        val (col, row) = calculateTerminalPosition(
            pixelX = 1000f,
            pixelY = 1000f,
            charWidth = 10f,
            charHeight = 20f,
            cols = 80,
            rows = 24
        )
        assertEquals(80, col)
        assertEquals(24, row)
    }

    @Test
    fun `calculateCellPosition with empty buffer does not throw`() {
        val (col, row) = calculateCellPosition(
            pixelX = 100f,
            pixelY = 100f,
            charWidth = 10f,
            charHeight = 20f,
            cols = 0,
            rows = 0
        )
        // With empty buffer, should return (0, 0) as minimum valid cell position
        assertEquals(0, col)
        assertEquals(0, row)
    }

    @Test
    fun `calculateCellPosition returns valid 0-based position`() {
        val (col, row) = calculateCellPosition(
            pixelX = 25f,
            pixelY = 45f,
            charWidth = 10f,
            charHeight = 20f,
            cols = 80,
            rows = 24
        )
        // 25/10 = 2, 45/20 = 2
        assertEquals(2, col)
        assertEquals(2, row)
    }

    @Test
    fun `calculateCellPosition clamps to max bounds`() {
        val (col, row) = calculateCellPosition(
            pixelX = 1000f,
            pixelY = 1000f,
            charWidth = 10f,
            charHeight = 20f,
            cols = 80,
            rows = 24
        )
        assertEquals(79, col) // 0-based, so max is cols-1
        assertEquals(23, row) // 0-based, so max is rows-1
    }

    // ========================================
    // extractSelectedText Tests
    // ========================================

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
