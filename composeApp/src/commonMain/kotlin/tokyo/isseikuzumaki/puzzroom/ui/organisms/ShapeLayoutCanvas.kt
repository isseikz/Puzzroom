package tokyo.isseikuzumaki.puzzroom.ui.organisms

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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.Degree
import tokyo.isseikuzumaki.puzzroom.domain.Degree.Companion.degree
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppSlider
import tokyo.isseikuzumaki.puzzroom.ui.atoms.SliderOrientation
import tokyo.isseikuzumaki.puzzroom.ui.state.SliderState
import tokyo.isseikuzumaki.puzzroom.ui.state.toCanvasOffset
import tokyo.isseikuzumaki.puzzroom.ui.state.toPoint
import tokyo.isseikuzumaki.puzzroom.ui.state.rotateAroundCenterOffsets
import tokyo.isseikuzumaki.puzzroom.ui.molecules.FloorPlanBackgroundImage
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

/**
 * Placed shape with position and rotation
 */
data class PlacedShape(
    val shape: Polygon,
    val position: Point,
    val rotation: Degree = Degree(0f),
    val color: Color = Color.Green,
    val name: String = ""
) {
    /**
     * Check if a point is inside the shape
     */
    fun contains(point: Point): Boolean {
        // Transform point to shape's local coordinates
        val relativePoint = Point(
            Centimeter(point.x.value - position.x.value),
            Centimeter(point.y.value - position.y.value)
        )
        return shape.contains(relativePoint)
    }
}

/**
 * 汎化された図形配置用キャンバス（Organism）
 * 家具や部屋の要素を配置するための汎用キャンバス
 * 
 * @param backgroundShape Canvas背景として表示するポリゴン（例：部屋の形状）
 * @param backgroundImageUrl Canvas背景として表示する画像のURL
 * @param shapeToPlace 配置予定の図形。null以外の場合、マウスカーソル位置でプレビュー表示される
 * @param shapeRotation 配置予定図形の回転角度（度数法）
 * @param placedShapes すでに配置済みの図形リスト
 * @param selectedShapeIndex 現在選択中の図形のインデックス（placedShapes内）
 * @param onPositionUpdate 配置予定図形のプレビュー位置が更新された時のコールバック。
 *                         shapeToPlaceがnullでない時、マウスカーソルの移動に応じて呼ばれる。
 *                         新しい図形を配置する際の位置トラッキングに使用。
 * @param onShapeSelected 図形が選択/選択解除された時のコールバック。
 *                        クリックで配置済み図形を選択、空白をクリックで選択解除。
 * @param onShapeMoved 配置済み図形がドラッグで移動された時のコールバック。
 *                     selectedShapeIndexで指定された図形をドラッグ中、連続的に呼ばれる。
 *                     既存図形の位置更新に使用。
 * @param modifier Canvas全体に適用されるModifier
 */
@Composable
fun ShapeLayoutCanvas(
    backgroundShape: Polygon? = null,
    backgroundImageUrl: String? = null,
    shapeToPlace: Polygon? = null,
    shapeRotation: Float = 0f,
    placedShapes: List<PlacedShape> = emptyList(),
    selectedShapeIndex: Int? = null,
    onPositionUpdate: (position: Point?) -> Unit = { _ -> },
    onShapeSelected: (index: Int?) -> Unit = { _ -> },
    onShapeMoved: (index: Int, newPosition: Point) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var shapePosition by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragStartPosition by remember { mutableStateOf<Offset?>(null) }
    var draggedShapeOriginalPosition by remember { mutableStateOf<Point?>(null) }

    // Track actual canvas size using onSizeChanged
    var canvasSize by remember { mutableStateOf(IntSize(500, 500)) }
    val canvasWidth = canvasSize.width
    val canvasHeight = canvasSize.height

    val xSliderState = remember(canvasWidth) {
        SliderState(
            initialValue = 0.5f,
            valueRange = 0f..1f,
            onValueChange = { fraction ->
                // Update selected shape position when slider moves
                selectedShapeIndex?.let { index ->
                    if (index >= 0 && index < placedShapes.size) {
                        val currentShape = placedShapes[index]
                        val newX = (fraction * canvasWidth).roundToInt()
                        val newPosition = Point(
                            Centimeter(newX),
                            currentShape.position.y
                        )
                        onShapeMoved(index, newPosition)
                    }
                }
            }
        )
    }

    val ySliderState = remember(canvasHeight) {
        SliderState(
            initialValue = 0.5f,
            valueRange = 0f..1f,
            onValueChange = { fraction ->
                // Update selected shape position when slider moves
                selectedShapeIndex?.let { index ->
                    if (index >= 0 && index < placedShapes.size) {
                        val currentShape = placedShapes[index]
                        val newY = (fraction * canvasHeight).roundToInt()
                        val newPosition = Point(
                            currentShape.position.x,
                            Centimeter(newY)
                        )
                        onShapeMoved(index, newPosition)
                    }
                }
            }
        )
    }

    // Update slider values when a shape is selected
    // Only responds to selection changes, not position changes, avoiding infinite loop
    LaunchedEffect(selectedShapeIndex) {
        selectedShapeIndex?.let { index ->
            if (index >= 0 && index < placedShapes.size) {
                val selectedShape = placedShapes[index]
                xSliderState.updateValueFromFraction(
                    selectedShape.position.x.value.toFloat() / canvasWidth
                )
                ySliderState.updateValueFromFraction(
                    selectedShape.position.y.value.toFloat() / canvasHeight
                )
            }
        }
    }

    // 位置が更新されたら通知
    LaunchedEffect(shapePosition) {
        if (!isDragging) {
            onPositionUpdate(shapePosition?.toPoint())
        }
    }

    Box(modifier = modifier) {
        FloorPlanBackgroundImage(
            imageUrl = backgroundImageUrl,
            modifier = Modifier.fillMaxSize()
        )

        AdaptiveNineGrid(
            commonSize = 20.dp,
            centerContent = {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            canvasSize = size
                        }
                        .pointerInput(shapeToPlace, selectedShapeIndex) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val position = event.changes.first().position

                                    when (event.type) {
                                        PointerEventType.Press -> {
                                            if (shapeToPlace == null) {
                                                // 配置済み図形の選択
                                                val clickedPoint = position.toPoint()
                                                val clickedIndex = placedShapes.indexOfLast {
                                                    it.contains(clickedPoint)
                                                }

                                                if (clickedIndex >= 0) {
                                                    onShapeSelected(clickedIndex)
                                                    isDragging = true
                                                    dragStartPosition = position
                                                    draggedShapeOriginalPosition =
                                                        placedShapes[clickedIndex].position
                                                } else {
                                                    onShapeSelected(null)
                                                }
                                            }
                                        }

                                        PointerEventType.Move -> {
                                            if (shapeToPlace != null) {
                                                // 新規図形の配置プレビュー
                                                shapePosition = position
                                            } else if (isDragging && selectedShapeIndex != null && dragStartPosition != null && draggedShapeOriginalPosition != null) {
                                                // 選択した図形のドラッグ
                                                val delta = position - dragStartPosition!!
                                                val newPosition = Point(
                                                    Centimeter(draggedShapeOriginalPosition!!.x.value + delta.x.roundToInt()),
                                                    Centimeter(draggedShapeOriginalPosition!!.y.value + delta.y.roundToInt())
                                                )
                                                onShapeMoved(
                                                    selectedShapeIndex,
                                                    newPosition
                                                )
                                            }
                                        }

                                        PointerEventType.Release -> {
                                            isDragging = false
                                            dragStartPosition = null
                                            draggedShapeOriginalPosition = null
                                        }

                                        PointerEventType.Enter -> {
                                            if (shapeToPlace != null) {
                                                shapePosition = position
                                            }
                                        }

                                        PointerEventType.Exit -> {
                                            if (shapeToPlace != null) {
                                                shapePosition = null
                                            }
                                        }

                                        else -> {}
                                    }
                                }
                            }
                        }
                ) {
                    // 背景図形（部屋の形状など）を描画
                    backgroundShape?.let { polygon ->
                        drawPoints(
                            points = polygon.points.map { it.toCanvasOffset() } + polygon.points.first()
                                .toCanvasOffset(),
                            pointMode = PointMode.Polygon,
                            color = Color.Gray,
                            strokeWidth = 3f
                        )
                    }

                    // 配置済みの図形を描画
                    placedShapes.forEachIndexed { index, placed ->
                        val rotatedPoints = placed.shape.rotateAroundCenterOffsets(
                            placed.position,
                            placed.rotation
                        )
                        val isSelected = index == selectedShapeIndex
                        drawPoints(
                            points = rotatedPoints + rotatedPoints.first(),
                            pointMode = PointMode.Polygon,
                            color = if (isSelected) Color.Blue else placed.color,
                            strokeWidth = if (isSelected) 5f else 3f
                        )
                    }

                    // 配置中の図形をプレビュー表示
                    if (shapeToPlace != null && shapePosition != null) {
                        val rotatedPoints = shapeToPlace.rotateAroundCenterOffsets(
                            shapePosition!!.toPoint(),
                            shapeRotation.degree()
                        )
                        drawPoints(
                            points = rotatedPoints + rotatedPoints.first(),
                            pointMode = PointMode.Polygon,
                            color = Color.Cyan,
                            strokeWidth = 5f
                        )
                    }
                }
            },
            modifier = Modifier,
            topContent = { AppSlider(state = xSliderState) },
            bottomContent = { AppSlider(state = xSliderState) },
            leftContent = {
                AppSlider(
                    state = ySliderState,
                    orientation = SliderOrientation.Vertical
                )
            },
            rightContent = {
                AppSlider(
                    state = ySliderState,
                    orientation = SliderOrientation.Vertical
                )
            },
            topLeftContent = {},
            topRightContent = {},
            bottomLeftContent = {},
            bottomRightContent = {}
        )
    }
}

@Preview
@Composable
private fun ShapeLayoutCanvasPreview() {
    ShapeLayoutCanvas(
        backgroundShape = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(500.cm(), 0.cm()),
                Point(500.cm(), 400.cm()),
                Point(0.cm(), 400.cm())
            )
        ),
        placedShapes = listOf(
            PlacedShape(
                shape = Polygon(
                    points = listOf(
                        Point(0.cm(), 0.cm()),
                        Point(120.cm(), 0.cm()),
                        Point(120.cm(), 80.cm()),
                        Point(0.cm(), 80.cm())
                    )
                ),
                position = Point(100.cm(), 100.cm()),
                rotation = 0f.degree(),
                color = Color.Green,
                name = "Rectangle"
            )
        )
    )
}
