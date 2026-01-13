package tokyo.isseikuzumaki.vibeterminal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalFontConfig

@Composable
fun TerminalCanvas(
    buffer: Array<Array<TerminalCell>>,
    cursorRow: Int,
    cursorCol: Int,
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

    Canvas(modifier = modifier.fillMaxSize()) {
        // Safety check: ensure valid dimensions
        if (charWidth <= 0f || charHeight <= 0f || size.width <= 0f || size.height <= 0f) {
            // Canvas not yet sized or invalid dimensions, just draw black background
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

                // Draw cursor if active
                if (rowIndex == cursorRow && colIndex == cursorCol) {
                    drawRect(
                        color = Color(0xFF00FF00), // Cursor color
                        topLeft = Offset(x, y),
                        size = Size(cellWidth, charHeight)
                    )
                }

                // Draw Text
                // Determine raw text color
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
                                fontWeight = if (cell.isBold) FontWeight.Bold else FontWeight.Normal,
                                // Note: Underline/Strikethrough support in Canvas drawText might need TextLayoutResult or decoration
                            )
                        )
                    }
                }
            }
        }
    }
}
