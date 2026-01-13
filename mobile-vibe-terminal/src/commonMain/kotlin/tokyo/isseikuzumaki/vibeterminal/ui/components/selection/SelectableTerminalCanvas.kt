package tokyo.isseikuzumaki.vibeterminal.ui.components.selection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalFontConfig

/** 選択ハイライトの色（半透明の青） */
private val SelectionHighlightColor = Color(0x4D2196F3)

/**
 * テキスト選択機能付きのターミナルキャンバス
 */
@Composable
fun SelectableTerminalCanvas(
    buffer: Array<Array<TerminalCell>>,
    cursorRow: Int,
    cursorCol: Int,
    selectionState: TextSelectionState,
    onSelectionStart: (TerminalPosition) -> Unit,
    onSelectionChange: (TerminalPosition) -> Unit,
    onSelectionEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontFamily = TerminalFontConfig.fontFamily,
        fontSize = TerminalFontConfig.fontSize
    )

    // Calculate cell dimensions based on a sample char
    val sampleLayout = textMeasurer.measure(
        text = "W",
        style = textStyle
    )
    val charWidth = sampleLayout.size.width.toFloat()
    val charHeight = sampleLayout.size.height.toFloat()

    // 現在のセル寸法を保持（ジェスチャーハンドラで使用）
    var currentCharWidth by remember { mutableStateOf(charWidth) }
    var currentCharHeight by remember { mutableStateOf(charHeight) }
    var currentRows by remember { mutableStateOf(buffer.size) }
    var currentCols by remember { mutableStateOf(buffer.firstOrNull()?.size ?: 0) }

    // Update cell dimensions when buffer or font changes
    currentCharWidth = charWidth
    currentCharHeight = charHeight
    currentRows = buffer.size
    currentCols = buffer.firstOrNull()?.size ?: 0

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        // ピクセル座標からセル位置を計算
                        val col = (offset.x / currentCharWidth).toInt()
                            .coerceIn(0, currentCols - 1)
                        val row = (offset.y / currentCharHeight).toInt()
                            .coerceIn(0, currentRows - 1)
                        onSelectionStart(TerminalPosition(row, col))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val offset = change.position
                        val col = (offset.x / currentCharWidth).toInt()
                            .coerceIn(0, currentCols - 1)
                        val row = (offset.y / currentCharHeight).toInt()
                            .coerceIn(0, currentRows - 1)
                        onSelectionChange(TerminalPosition(row, col))
                    },
                    onDragEnd = {
                        onSelectionEnd()
                    },
                    onDragCancel = {
                        onSelectionEnd()
                    }
                )
            }
    ) {
        // Safety check: ensure valid dimensions
        if (charWidth <= 0f || charHeight <= 0f || size.width <= 0f || size.height <= 0f) {
            drawRect(color = Color.Black)
            return@Canvas
        }

        // Draw background for the whole terminal
        drawRect(color = Color.Black)

        buffer.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, cell ->
                val x = colIndex * charWidth
                val y = rowIndex * charHeight

                // Safety check: ensure position is within canvas bounds
                if (x < 0f || y < 0f || x >= size.width || y >= size.height) {
                    return@forEachIndexed
                }

                // Skip rendering for wide character padding cells
                // The wide character is already drawn from the previous cell position
                if (cell.isWideCharPadding) {
                    return@forEachIndexed
                }

                // Calculate cell width (2x for wide characters)
                val cellWidth = if (cell.isWideChar) charWidth * 2 else charWidth

                // Draw cell background
                if (cell.backgroundColor != Color.Black) {
                    drawRect(
                        color = cell.backgroundColor,
                        topLeft = Offset(x, y),
                        size = Size(cellWidth, charHeight)
                    )
                }

                // Draw selection highlight
                if (selectionState.isCellSelected(rowIndex, colIndex)) {
                    drawRect(
                        color = SelectionHighlightColor,
                        topLeft = Offset(x, y),
                        size = Size(cellWidth, charHeight)
                    )
                }

                // Draw cursor if active (on top of selection)
                if (rowIndex == cursorRow && colIndex == cursorCol) {
                    drawRect(
                        color = Color(0xFF00FF00), // Cursor color
                        topLeft = Offset(x, y),
                        size = Size(cellWidth, charHeight)
                    )
                }

                // Determine text color
                val textColor = if (rowIndex == cursorRow && colIndex == cursorCol) {
                    Color.Black // Text inside cursor
                } else {
                    cell.foregroundColor
                }

                if (cell.char != ' ' && cell.char.code != 0) {
                    // Additional safety check before drawing text
                    if (x + cellWidth <= size.width && y + charHeight <= size.height) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = cell.char.toString(),
                            topLeft = Offset(x, y),
                            style = textStyle.copy(
                                color = textColor,
                                fontWeight = if (cell.isBold) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }
    }
}
