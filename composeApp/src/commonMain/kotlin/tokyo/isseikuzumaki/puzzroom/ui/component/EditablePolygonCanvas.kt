package tokyo.isseikuzumaki.puzzroom.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import coil3.compose.AsyncImage
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.PolygonGeometry
import kotlin.math.roundToInt

/**
 * 編集モード
 */
sealed interface EditMode {
    /**
     * 作成モード - クリックで頂点を追加
     */
    data object Creation : EditMode

    /**
     * 編集モード - 頂点をドラッグで移動
     */
    data class Editing(val polygonIndex: Int) : EditMode
}

/**
 * 編集可能なポリゴンキャンバス
 *
 * 作成モードと編集モードをサポート
 */
@Composable
fun EditablePolygonCanvas(
    polygons: List<Polygon>,
    selectedPolygonIndex: Int?,
    backgroundImageUrl: String?,
    editMode: EditMode,
    onNewVertex: (Offset) -> Unit,
    onCompletePolygon: (Polygon) -> Unit,
    onVertexMove: (polygonIndex: Int, vertexIndex: Int, newPosition: Point) -> Unit,
    modifier: Modifier = Modifier
) {
    var creationVertices by remember { mutableStateOf(listOf<Offset>()) }
    var draggingVertex by remember { mutableStateOf<DragState?>(null) }

    Box(modifier = modifier) {
        // Background image
        backgroundImageUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = "背景画像",
                modifier = Modifier.fillMaxSize()
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(editMode) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.first().position

                            when (event.type) {
                                PointerEventType.Press -> {
                                    when (editMode) {
                                        is EditMode.Creation -> {
                                            // 作成モード: 頂点を追加またはポリゴンを完成
                                            if (creationVertices.size >= 2 &&
                                                (position - creationVertices.first()).getDistance() < 40f
                                            ) {
                                                // 最初の頂点近くでクリック → ポリゴン完成
                                                val polygon = Polygon(
                                                    points = creationVertices.map { it.toPoint() }
                                                )
                                                onCompletePolygon(polygon)
                                                creationVertices = emptyList()
                                            } else {
                                                // 新しい頂点を追加
                                                creationVertices = creationVertices + position
                                                onNewVertex(position)
                                            }
                                        }

                                        is EditMode.Editing -> {
                                            // 編集モード: 頂点を選択してドラッグ開始
                                            selectedPolygonIndex?.let { idx ->
                                                if (idx in polygons.indices) {
                                                    val vertexIndex = PolygonGeometry.findNearestVertex(
                                                        position.toPoint(),
                                                        polygons[idx],
                                                        threshold = 30.0
                                                    )

                                                    if (vertexIndex != null) {
                                                        draggingVertex = DragState(
                                                            polygonIndex = idx,
                                                            vertexIndex = vertexIndex,
                                                            initialPosition = position
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                PointerEventType.Move -> {
                                    // ドラッグ中の頂点を移動
                                    draggingVertex?.let { drag ->
                                        onVertexMove(
                                            drag.polygonIndex,
                                            drag.vertexIndex,
                                            position.toPoint()
                                        )
                                    }
                                }

                                PointerEventType.Release -> {
                                    // ドラッグ終了
                                    draggingVertex = null
                                }

                                else -> {}
                            }
                        }
                    }
                }
        ) {
            // Draw existing polygons
            polygons.forEachIndexed { index, polygon ->
                val isSelected = index == selectedPolygonIndex
                val color = if (isSelected) Color.Blue else Color.Red
                val strokeWidth = if (isSelected) 7f else 5f
                val isClosed = PolygonGeometry.isPolygonClosed(polygon)

                // Draw edges
                val points = polygon.points.map { it.toCanvasOffset() }
                if (isClosed) {
                    // 閉じたポリゴン: 通常描画
                    drawPoints(
                        points = points + points.first(),
                        pointMode = PointMode.Polygon,
                        color = color,
                        strokeWidth = strokeWidth
                    )
                } else {
                    // 開いたポリゴン: 実線で辺を描画
                    drawPoints(
                        points = points,
                        pointMode = PointMode.Polygon,
                        color = color,
                        strokeWidth = strokeWidth
                    )

                    // 最初と最後の頂点を赤い点線で結ぶ
                    drawLine(
                        color = Color.Red,
                        start = points.last(),
                        end = points.first(),
                        strokeWidth = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Draw vertices (if selected)
                if (isSelected && editMode is EditMode.Editing) {
                    polygon.points.forEachIndexed { vertexIndex, point ->
                        val vertexColor = if (draggingVertex?.vertexIndex == vertexIndex) {
                            Color.Green
                        } else {
                            Color.Blue
                        }
                        drawCircle(
                            color = vertexColor,
                            radius = 10f,
                            center = point.toCanvasOffset()
                        )
                    }
                }
            }

            // Draw creation vertices (in creation mode)
            if (editMode is EditMode.Creation && creationVertices.isNotEmpty()) {
                drawPoints(
                    points = creationVertices,
                    pointMode = PointMode.Polygon,
                    color = Color.Blue,
                    strokeWidth = 5f
                )

                // Draw vertices
                creationVertices.forEach { vertex ->
                    drawCircle(
                        color = Color.Blue,
                        radius = 8f,
                        center = vertex
                    )
                }
            }
        }
    }
}

/**
 * ドラッグ状態
 */
private data class DragState(
    val polygonIndex: Int,
    val vertexIndex: Int,
    val initialPosition: Offset
)

/**
 * Point to Canvas Offset conversion
 */
private fun Point.toCanvasOffset(): Offset {
    return Offset(x.value.toFloat(), y.value.toFloat())
}

/**
 * Canvas Offset to Point conversion
 */
private fun Offset.toPoint(): Point {
    return Point(Centimeter(x.roundToInt()), Centimeter(y.roundToInt()))
}
