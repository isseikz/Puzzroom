package tokyo.isseikuzumaki.vibeterminal.ui.components.selection

import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell
import kotlin.test.Test
import kotlin.test.assertEquals

class TextSelectionUtilsTest {

    // ========================================
    // Parameterized Test Cases
    // ========================================

    /**
     * Test case for calculateTerminalPosition (1-based coordinates)
     */
    private data class TerminalPositionTestCase(
        val name: String,
        val pixelX: Float,
        val pixelY: Float,
        val charWidth: Float,
        val charHeight: Float,
        val cols: Int,
        val rows: Int,
        val expectedCol: Int,
        val expectedRow: Int
    )

    /**
     * Test case for calculateCellPosition (0-based coordinates)
     */
    private data class CellPositionTestCase(
        val name: String,
        val pixelX: Float,
        val pixelY: Float,
        val charWidth: Float,
        val charHeight: Float,
        val cols: Int,
        val rows: Int,
        val expectedCol: Int,
        val expectedRow: Int
    )

    /**
     * Test case for extractSelectedText
     */
    private data class ExtractTextTestCase(
        val name: String,
        val lines: List<String>,
        val anchorRow: Int,
        val anchorCol: Int,
        val currentRow: Int,
        val currentCol: Int,
        val isSelecting: Boolean,
        val expected: String
    )

    // ========================================
    // Regression Tests for Issue #163
    // calculateTerminalPosition and calculateCellPosition
    // must not throw IllegalArgumentException when buffer is empty
    // ========================================

    private val terminalPositionTestCases = listOf(
        TerminalPositionTestCase(
            name = "empty buffer (cols=0, rows=0)",
            pixelX = 100f, pixelY = 100f,
            charWidth = 10f, charHeight = 20f,
            cols = 0, rows = 0,
            expectedCol = 1, expectedRow = 1
        ),
        TerminalPositionTestCase(
            name = "zero cols only",
            pixelX = 100f, pixelY = 100f,
            charWidth = 10f, charHeight = 20f,
            cols = 0, rows = 10,
            expectedCol = 1, expectedRow = 6  // 100/20 + 1 = 6, clamped to 1..10
        ),
        TerminalPositionTestCase(
            name = "zero rows only",
            pixelX = 100f, pixelY = 100f,
            charWidth = 10f, charHeight = 20f,
            cols = 10, rows = 0,
            expectedCol = 10, expectedRow = 1  // 100/10 + 1 = 11, clamped to 1..10
        ),
        TerminalPositionTestCase(
            name = "normal position",
            pixelX = 25f, pixelY = 45f,
            charWidth = 10f, charHeight = 20f,
            cols = 80, rows = 24,
            expectedCol = 3, expectedRow = 3  // 25/10 + 1 = 3, 45/20 + 1 = 3
        ),
        TerminalPositionTestCase(
            name = "clamps to max bounds",
            pixelX = 1000f, pixelY = 1000f,
            charWidth = 10f, charHeight = 20f,
            cols = 80, rows = 24,
            expectedCol = 80, expectedRow = 24
        ),
        TerminalPositionTestCase(
            name = "position at origin",
            pixelX = 0f, pixelY = 0f,
            charWidth = 10f, charHeight = 20f,
            cols = 80, rows = 24,
            expectedCol = 1, expectedRow = 1
        ),
        TerminalPositionTestCase(
            name = "position at cell boundary",
            pixelX = 10f, pixelY = 20f,
            charWidth = 10f, charHeight = 20f,
            cols = 80, rows = 24,
            expectedCol = 2, expectedRow = 2  // exactly on boundary = next cell
        )
    )

    @Test
    fun calculateTerminalPosition_parameterized() {
        terminalPositionTestCases.forEach { case ->
            val (col, row) = calculateTerminalPosition(
                pixelX = case.pixelX,
                pixelY = case.pixelY,
                charWidth = case.charWidth,
                charHeight = case.charHeight,
                cols = case.cols,
                rows = case.rows
            )
            assertEquals(case.expectedCol, col, "col mismatch for: ${case.name}")
            assertEquals(case.expectedRow, row, "row mismatch for: ${case.name}")
        }
    }

    private val cellPositionTestCases = listOf(
        CellPositionTestCase(
            name = "empty buffer (cols=0, rows=0)",
            pixelX = 100f, pixelY = 100f,
            charWidth = 10f, charHeight = 20f,
            cols = 0, rows = 0,
            expectedCol = 0, expectedRow = 0
        ),
        CellPositionTestCase(
            name = "normal position",
            pixelX = 25f, pixelY = 45f,
            charWidth = 10f, charHeight = 20f,
            cols = 80, rows = 24,
            expectedCol = 2, expectedRow = 2  // 25/10 = 2, 45/20 = 2
        ),
        CellPositionTestCase(
            name = "clamps to max bounds (0-based)",
            pixelX = 1000f, pixelY = 1000f,
            charWidth = 10f, charHeight = 20f,
            cols = 80, rows = 24,
            expectedCol = 79, expectedRow = 23  // 0-based, so max is cols-1, rows-1
        ),
        CellPositionTestCase(
            name = "position at origin",
            pixelX = 0f, pixelY = 0f,
            charWidth = 10f, charHeight = 20f,
            cols = 80, rows = 24,
            expectedCol = 0, expectedRow = 0
        ),
        CellPositionTestCase(
            name = "position at cell boundary",
            pixelX = 10f, pixelY = 20f,
            charWidth = 10f, charHeight = 20f,
            cols = 80, rows = 24,
            expectedCol = 1, expectedRow = 1  // exactly on boundary = next cell
        )
    )

    @Test
    fun calculateCellPosition_parameterized() {
        cellPositionTestCases.forEach { case ->
            val (col, row) = calculateCellPosition(
                pixelX = case.pixelX,
                pixelY = case.pixelY,
                charWidth = case.charWidth,
                charHeight = case.charHeight,
                cols = case.cols,
                rows = case.rows
            )
            assertEquals(case.expectedCol, col, "col mismatch for: ${case.name}")
            assertEquals(case.expectedRow, row, "row mismatch for: ${case.name}")
        }
    }

    // ========================================
    // extractSelectedText Tests
    // ========================================

    private fun createBuffer(lines: List<String>): Array<Array<TerminalCell>> {
        if (lines.isEmpty()) return arrayOf()
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

    private val extractTextTestCases = listOf(
        ExtractTextTestCase(
            name = "no selection returns empty",
            lines = listOf("Hello World"),
            anchorRow = 0, anchorCol = 0,
            currentRow = 0, currentCol = 0,
            isSelecting = false,
            expected = ""
        ),
        ExtractTextTestCase(
            name = "single line partial selection",
            lines = listOf("Hello World"),
            anchorRow = 0, anchorCol = 0,
            currentRow = 0, currentCol = 4,
            isSelecting = true,
            expected = "Hello"
        ),
        ExtractTextTestCase(
            name = "single line trims trailing spaces",
            lines = listOf("Hello   "),
            anchorRow = 0, anchorCol = 0,
            currentRow = 0, currentCol = 7,
            isSelecting = true,
            expected = "Hello"
        ),
        ExtractTextTestCase(
            name = "multiple lines with newlines",
            lines = listOf("Line 1", "Line 2", "Line 3"),
            anchorRow = 0, anchorCol = 0,
            currentRow = 2, currentCol = 5,
            isSelecting = true,
            expected = "Line 1\nLine 2\nLine 3"
        ),
        ExtractTextTestCase(
            name = "multiple lines trim trailing spaces",
            lines = listOf("AAA   ", "BBB   ", "CCC   "),
            anchorRow = 0, anchorCol = 0,
            currentRow = 2, currentCol = 5,
            isSelecting = true,
            expected = "AAA\nBBB\nCCC"
        ),
        ExtractTextTestCase(
            name = "partial selection across lines",
            lines = listOf("Hello World", "Foo Bar"),
            anchorRow = 0, anchorCol = 6,
            currentRow = 1, currentCol = 2,
            isSelecting = true,
            expected = "World\nFoo"
        ),
        ExtractTextTestCase(
            name = "reverse selection returns same text",
            lines = listOf("Hello World"),
            anchorRow = 0, anchorCol = 4,
            currentRow = 0, currentCol = 0,
            isSelecting = true,
            expected = "Hello"
        ),
        ExtractTextTestCase(
            name = "empty buffer returns empty",
            lines = emptyList(),
            anchorRow = 0, anchorCol = 0,
            currentRow = 0, currentCol = 5,
            isSelecting = true,
            expected = ""
        )
    )

    @Test
    fun extractSelectedText_parameterized() {
        extractTextTestCases.forEach { case ->
            val buffer = createBuffer(case.lines)
            val state = if (case.isSelecting) {
                TextSelectionState(
                    isSelecting = true,
                    anchorPosition = TerminalPosition(case.anchorRow, case.anchorCol),
                    currentPosition = TerminalPosition(case.currentRow, case.currentCol)
                )
            } else {
                TextSelectionState.Empty
            }

            val result = extractSelectedText(buffer, state)

            assertEquals(case.expected, result, "Failed for: ${case.name}")
        }
    }
}
