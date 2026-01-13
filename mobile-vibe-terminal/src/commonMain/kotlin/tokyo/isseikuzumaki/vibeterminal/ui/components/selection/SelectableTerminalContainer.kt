package tokyo.isseikuzumaki.vibeterminal.ui.components.selection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalFontConfig
import tokyo.isseikuzumaki.vibeterminal.util.ClipboardManager
import tokyo.isseikuzumaki.vibeterminal.util.Logger

/** 選択ハイライトの色（半透明の青） */
private val SelectionHighlightColor = Color(0x4D2196F3)

/**
 * テキスト選択機能を持つターミナルコンテナ
 * 子コンポーネント（TerminalCanvas または TerminalBufferView）をラップし、
 * 選択のオーバーレイとジェスチャー検出を追加する
 */
@Composable
fun SelectableTerminalContainer(
    buffer: Array<Array<TerminalCell>>,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var selectionState by remember { mutableStateOf(TextSelectionState.Empty) }
    var showContextMenu by remember { mutableStateOf(false) }

    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontFamily = TerminalFontConfig.fontFamily,
        fontSize = TerminalFontConfig.fontSize
    )

    // Calculate cell dimensions
    val sampleLayout = remember(textStyle) {
        textMeasurer.measure(text = "W", style = textStyle)
    }
    val charWidth = sampleLayout.size.width.toFloat()
    val charHeight = sampleLayout.size.height.toFloat()

    val rows = buffer.size
    val cols = buffer.firstOrNull()?.size ?: 0

    Logger.d("SelectableTerminalContainer: buffer=${rows}x${cols}, charSize=${charWidth}x${charHeight}")

    Box(
        modifier = modifier
            .pointerInput(charWidth, charHeight, rows, cols) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        Logger.d("SelectableTerminalContainer: onDragStart at $offset")
                        // ピクセル座標からセル位置を計算
                        val col = (offset.x / charWidth).toInt().coerceIn(0, (cols - 1).coerceAtLeast(0))
                        val row = (offset.y / charHeight).toInt().coerceIn(0, (rows - 1).coerceAtLeast(0))
                        val position = TerminalPosition(row, col)
                        Logger.d("SelectableTerminalContainer: Selection started at row=$row, col=$col")
                        selectionState = TextSelectionState(
                            isSelecting = true,
                            anchorPosition = position,
                            currentPosition = position
                        )
                        showContextMenu = false
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val offset = change.position
                        val col = (offset.x / charWidth).toInt().coerceIn(0, (cols - 1).coerceAtLeast(0))
                        val row = (offset.y / charHeight).toInt().coerceIn(0, (rows - 1).coerceAtLeast(0))
                        selectionState = selectionState.copy(
                            currentPosition = TerminalPosition(row, col)
                        )
                    },
                    onDragEnd = {
                        Logger.d("SelectableTerminalContainer: onDragEnd, hasSelection=${selectionState.hasSelection}")
                        if (selectionState.hasSelection) {
                            showContextMenu = true
                        }
                    },
                    onDragCancel = {
                        Logger.d("SelectableTerminalContainer: onDragCancel")
                        selectionState = TextSelectionState.Empty
                        showContextMenu = false
                    }
                )
            }
            .pointerInput(selectionState.hasSelection) {
                // 選択範囲外をタップしたら選択解除
                detectTapGestures(
                    onTap = {
                        Logger.d("SelectableTerminalContainer: onTap, hasSelection=${selectionState.hasSelection}")
                        if (selectionState.hasSelection) {
                            selectionState = TextSelectionState.Empty
                            showContextMenu = false
                        }
                    }
                )
            }
    ) {
        // ターミナルコンテンツ
        content()

        // 選択ハイライトオーバーレイ
        if (selectionState.hasSelection) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                buffer.forEachIndexed { rowIndex, row ->
                    row.forEachIndexed { colIndex, cell ->
                        // Skip padding cells - the highlight is drawn from the main cell
                        if (cell.isWideCharPadding) {
                            return@forEachIndexed
                        }

                        if (selectionState.isCellSelected(rowIndex, colIndex)) {
                            val x = colIndex * charWidth
                            val y = rowIndex * charHeight
                            // Use 2x width for wide characters
                            val cellWidth = if (cell.isWideChar) charWidth * 2 else charWidth
                            drawRect(
                                color = SelectionHighlightColor,
                                topLeft = Offset(x, y),
                                size = Size(cellWidth, charHeight)
                            )
                        }
                    }
                }
            }
        }

        // コンテキストメニュー
        if (showContextMenu && selectionState.hasSelection) {
            // メニュー位置を選択範囲の上部中央に配置
            val start = selectionState.startPosition
            val end = selectionState.endPosition
            if (start != null && end != null) {
                val menuX = ((start.col + end.col) / 2f * charWidth).toInt()
                val menuY = (start.row * charHeight - 48.dp.value * LocalDensity.current.density).toInt()
                    .coerceAtLeast(0)

                SelectionContextMenu(
                    onCopy = {
                        val selectedText = extractSelectedText(buffer, selectionState)
                        if (selectedText.isNotEmpty()) {
                            ClipboardManager.copyToClipboard(selectedText)
                        }
                        selectionState = TextSelectionState.Empty
                        showContextMenu = false
                    },
                    onDismiss = {
                        selectionState = TextSelectionState.Empty
                        showContextMenu = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(menuX, menuY) }
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}
