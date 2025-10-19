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
import tokyo.isseikuzumaki.puzzroom.ui.state.PolygonEditState
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
    
    /**
     * 寸法編集モード - 辺を選択して長さを編集
     */
    data class DimensionEditing(val polygonIndex: Int) : EditMode
    
    /**
     * 角度編集モード - 頂点を選択して角度を編集
     */
    data class AngleEditing(val polygonIndex: Int) : EditMode
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
    editState: PolygonEditState? = null,
    onNewVertex: (Offset) -> Unit,
    onCompletePolygon: (Polygon) -> Unit,
    onVertexMove: (polygonIndex: Int, vertexIndex: Int, newPosition: Point) -> Unit,
    onEdgeSelect: (polygonIndex: Int, edgeIndex: Int) -> Unit = { _, _ -> },
    onVertexSelect: (polygonIndex: Int, vertexIndex: Int) -> Unit = { _, _ -> },
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
                                        
                                        is EditMode.DimensionEditing -> {
                                            // 寸法編集モード: 辺を選択
                                            val idx = editMode.polygonIndex
                                            if (idx in polygons.indices) {
                                                val edgeIndex = PolygonGeometry.findNearestEdge(
                                                    position.toPoint(),
                                                    polygons[idx],
                                                    threshold = 30.0
                                                )
                                                if (edgeIndex != null) {
                                                    onEdgeSelect(idx, edgeIndex)
                                                }
                                            }
                                        }
                                        
                                        is EditMode.AngleEditing -> {
                                            // 角度編集モード: 頂点を選択
                                            val idx = editMode.polygonIndex
                                            if (idx in polygons.indices) {
                                                val vertexIndex = PolygonGeometry.findNearestVertex(
                                                    position.toPoint(),
                                                    polygons[idx],
                                                    threshold = 30.0
                                                )
                                                if (vertexIndex != null) {
                                                    onVertexSelect(idx, vertexIndex)
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
                
                // 辺を個別に描画（ロック状態を反映）
                polygon.points.indices.forEach { edgeIndex ->
                    val p1 = points[edgeIndex]
                    val p2 = points[(edgeIndex + 1) % points.size]
                    
                    // ロック状態に応じて色と太さを変更
                    val isLocked = editState?.isEdgeLocked(edgeIndex) ?: false
                    val edgeColor = if (isLocked) Color(0xFFFFD700) else color // Gold for locked
                    val edgeWidth = if (isLocked) strokeWidth + 2f else strokeWidth
                    
                    // 最後の辺で開いている場合は点線
                    if (!isClosed && edgeIndex == polygon.points.size - 1) {
                        drawLine(
                            color = Color.Red,
                            start = p1,
                            end = p2,
                            strokeWidth = edgeWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    } else {
                        drawLine(
                            color = edgeColor,
                            start = p1,
                            end = p2,
                            strokeWidth = edgeWidth
                        )
                    }
                }

                // Draw vertices (if selected)
                if (isSelected) {
                    when (editMode) {
                        is EditMode.Editing -> {
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
                        is EditMode.DimensionEditing -> {
                            // 寸法編集モード: 辺の中点を表示
                            polygon.points.indices.forEach { edgeIndex ->
                                val p1 = points[edgeIndex]
                                val p2 = points[(edgeIndex + 1) % points.size]
                                val midPoint = Offset((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
                                val isLocked = editState?.isEdgeLocked(edgeIndex) ?: false
                                drawCircle(
                                    color = if (isLocked) Color(0xFFFFD700) else Color.Blue,
                                    radius = 8f,
                                    center = midPoint
                                )
                            }
                        }
                        is EditMode.AngleEditing -> {
                            // 角度編集モード: 頂点を表示（ロック状態を反映）
                            polygon.points.forEachIndexed { vertexIndex, point ->
                                val isLocked = editState?.isAngleLocked(vertexIndex) ?: false
                                drawCircle(
                                    color = if (isLocked) Color(0xFFFFD700) else Color.Blue,
                                    radius = 10f,
                                    center = point.toCanvasOffset()
                                )
                            }
                        }
                        else -> {}
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
