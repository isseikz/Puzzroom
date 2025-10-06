package tokyo.isseikuzumaki.puzzroom.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import coil3.compose.AsyncImage
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.Degree
import tokyo.isseikuzumaki.puzzroom.domain.Degree.Companion.degree
import tokyo.isseikuzumaki.puzzroom.domain.Furniture
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate
import tokyo.isseikuzumaki.puzzroom.domain.LayoutEntry
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.Room
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 配置された家具の状態
 */
data class PlacedFurniture(
    val furniture: Furniture,
    val position: Point,
    val rotation: Degree
) {
    /**
     * 指定された点が家具の内部にあるかチェック
     */
    fun contains(point: Point): Boolean {
        val rotatedPoints = furniture.shape.rotateAroundCenterToPoints(position, rotation)
        return pointInPolygon(point, rotatedPoints)
    }
}

/**
 * Polygon を指定位置に配置し、幾何中心を軸に回転させた結果をPointのリストで返す
 */
private fun Polygon.rotateAroundCenterToPoints(position: Point, rotation: Degree): List<Point> {
    val positionOffset = position.toCanvasOffset()
    val shapeCenter = getCenter()

    return points.map { point ->
        val pointOffset = point.toCanvasOffset()
        val relativeOffset = Offset(
            pointOffset.x - shapeCenter.x,
            pointOffset.y - shapeCenter.y
        )
        val rotated = relativeOffset.rotate(Offset.Zero, rotation.value)
        Offset(
            positionOffset.x + rotated.x,
            positionOffset.y + rotated.y
        ).toPoint()
    }
}

/**
 * 点がポリゴン内にあるかチェック（Ray Casting Algorithm）
 */
private fun pointInPolygon(point: Point, polygon: List<Point>): Boolean {
    var inside = false
    var j = polygon.size - 1

    for (i in polygon.indices) {
        val pi = polygon[i]
        val pj = polygon[j]

        if ((pi.y.value > point.y.value) != (pj.y.value > point.y.value)) {
            val intersectX = (pj.x.value - pi.x.value).toLong() *
                    (point.y.value - pi.y.value).toLong() /
                    (pj.y.value - pi.y.value).toLong() +
                    pi.x.value.toLong()

            if (point.x.value < intersectX) {
                inside = !inside
            }
        }
        j = i
    }

    return inside
}

private fun Point.toCanvasOffset(): Offset {
    return Offset(x.value.toFloat(), y.value.toFloat())
}

private fun Offset.toPoint(): Point {
    return Point(Centimeter(x.roundToInt()), Centimeter(y.roundToInt()))
}

/**
 * 点を回転させる
 */
private fun Offset.rotate(center: Offset, degrees: Float): Offset {
    val radians = degrees / 180f * PI
    val cos = cos(radians).toFloat()
    val sin = sin(radians).toFloat()
    val dx = x - center.x
    val dy = y - center.y
    return Offset(
        center.x + dx * cos - dy * sin,
        center.y + dx * sin + dy * cos
    )
}

/**
 * Polygon の幾何中心を計算
 */
private fun Polygon.getCenter(): Offset {
    val sumX = points.sumOf { it.x.value.toDouble() }
    val sumY = points.sumOf { it.y.value.toDouble() }
    return Offset(
        (sumX / points.size).toFloat(),
        (sumY / points.size).toFloat()
    )
}

/**
 * Polygon を指定位置に配置し、幾何中心を軸に回転させる
 */
private fun Polygon.rotateAroundCenter(position: Point, rotation: Degree): List<Offset> {
    val positionOffset = position.toCanvasOffset()
    val shapeCenter = getCenter()

    return points.map { point ->
        val pointOffset = point.toCanvasOffset()
        // 図形の中心を原点とした相対座標
        val relativeOffset = Offset(
            pointOffset.x - shapeCenter.x,
            pointOffset.y - shapeCenter.y
        )
        // 回転
        val rotated = relativeOffset.rotate(Offset.Zero, rotation.value)
        // 配置位置に移動
        Offset(
            positionOffset.x + rotated.x,
            positionOffset.y + rotated.y
        )
    }
}

enum class CanvasMode {
    SELECT_FROM_LIBRARY,  // ライブラリから選択中
    CREATE_FURNITURE,  // 家具を作成中
    PLACE_FURNITURE,   // 家具を配置中
}

@Composable
fun FurnitureLayoutCanvas(
    room: Room,
    backgroundImageUrl: String? = null,
    furnitureToPlace: Furniture? = null,
    furnitureRotation: Float = 0f,
    placedFurnitures: List<PlacedFurniture> = emptyList(),
    selectedFurnitureIndex: Int? = null,
    onPositionUpdate: (position: Point?) -> Unit = { _ -> },
    onFurnitureSelected: (index: Int?) -> Unit = { _ -> },
    onFurnitureMoved: (index: Int, newPosition: Point) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var furniturePosition by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragStartPosition by remember { mutableStateOf<Offset?>(null) }
    var draggedFurnitureOriginalPosition by remember { mutableStateOf<Point?>(null) }

    // 位置が更新されたら通知
    LaunchedEffect(furniturePosition) {
        if (!isDragging) {
            onPositionUpdate(furniturePosition?.toPoint())
        }
    }

    Box(modifier = modifier) {
        backgroundImageUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize()
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(furnitureToPlace, selectedFurnitureIndex) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.first().position

                            when (event.type) {
                                PointerEventType.Press -> {
                                    if (furnitureToPlace == null) {
                                        // 配置済み家具の選択
                                        val clickedPoint = position.toPoint()
                                        val clickedIndex = placedFurnitures.indexOfLast { it.contains(clickedPoint) }

                                        if (clickedIndex >= 0) {
                                            onFurnitureSelected(clickedIndex)
                                            isDragging = true
                                            dragStartPosition = position
                                            draggedFurnitureOriginalPosition = placedFurnitures[clickedIndex].position
                                        } else {
                                            onFurnitureSelected(null)
                                        }
                                    }
                                }
                                PointerEventType.Move -> {
                                    if (furnitureToPlace != null) {
                                        // 新規家具の配置プレビュー
                                        furniturePosition = position
                                    } else if (isDragging && selectedFurnitureIndex != null && dragStartPosition != null && draggedFurnitureOriginalPosition != null) {
                                        // 選択した家具のドラッグ
                                        val delta = position - dragStartPosition!!
                                        val newPosition = Point(
                                            Centimeter(draggedFurnitureOriginalPosition!!.x.value + delta.x.roundToInt()),
                                            Centimeter(draggedFurnitureOriginalPosition!!.y.value + delta.y.roundToInt())
                                        )
                                        onFurnitureMoved(selectedFurnitureIndex, newPosition)
                                    }
                                }
                                PointerEventType.Release -> {
                                    isDragging = false
                                    dragStartPosition = null
                                    draggedFurnitureOriginalPosition = null
                                }
                                PointerEventType.Enter -> {
                                    if (furnitureToPlace != null) {
                                        furniturePosition = position
                                    }
                                }
                                PointerEventType.Exit -> {
                                    if (furnitureToPlace != null) {
                                        furniturePosition = null
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
        ) {
            // 部屋のポリゴンを描画
            drawPoints(
                points = room.shape.points.map { it.toCanvasOffset() } + room.shape.points.first().toCanvasOffset(),
                pointMode = PointMode.Polygon,
                color = Color.Gray,
                strokeWidth = 3f
            )

            // 配置済みの家具を描画
            placedFurnitures.forEachIndexed { index, placed ->
                val rotatedPoints = placed.furniture.shape.rotateAroundCenter(
                    placed.position,
                    placed.rotation
                )
                val isSelected = index == selectedFurnitureIndex
                drawPoints(
                    points = rotatedPoints + rotatedPoints.first(),
                    pointMode = PointMode.Polygon,
                    color = if (isSelected) Color.Blue else Color.Green,
                    strokeWidth = if (isSelected) 5f else 3f
                )
            }

            // 配置中の家具をプレビュー表示
            if (furnitureToPlace != null && furniturePosition != null) {
                val rotatedPoints = furnitureToPlace.shape.rotateAroundCenter(
                    furniturePosition!!.toPoint(),
                    furnitureRotation.degree()
                )
                drawPoints(
                    points = rotatedPoints + rotatedPoints.first(),
                    pointMode = PointMode.Polygon,
                    color = Color.Cyan,
                    strokeWidth = 5f
                )
            }
        }
    }
}
