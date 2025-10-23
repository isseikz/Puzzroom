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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppSlider
import tokyo.isseikuzumaki.puzzroom.ui.atoms.SliderOrientation
import tokyo.isseikuzumaki.puzzroom.ui.state.SliderState
import tokyo.isseikuzumaki.puzzroom.ui.molecules.FloorPlanBackgroundImage
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme

// === Compatibility layer for domain-specific types ===

import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.Degree
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon

/**
 * Placed shape with position and rotation (domain-specific types)
 * 互換性のための旧形式データクラス
 */
data class PlacedShape(
    val shape: Polygon,
    val position: Point,
    val rotation: Degree = Degree(0f),
    val color: Color = Color.Green,
    val name: String = ""
)

/**
 * Convert domain-specific PlacedShape to normalized format
 * ドメイン固有の PlacedShape を無次元形式に変換
 */
fun PlacedShape.toNormalized(canvasWidth: Int, canvasHeight: Int): NormalizedPlacedShape {
    return NormalizedPlacedShape(
        shape = NormalizedShape(
            points = shape.points.map { point ->
                NormalizedPoint(
                    x = point.x.value / canvasWidth.toFloat(),
                    y = point.y.value / canvasHeight.toFloat()
                )
            },
            color = color
        ),
        position = NormalizedPoint(
            x = position.x.value / canvasWidth.toFloat(),
            y = position.y.value / canvasHeight.toFloat()
        ),
        rotation = rotation.value,
        color = color,
        name = name
    )
}

/**
 * Convert normalized PlacedShape to domain-specific format
 * 無次元形式を ドメイン固有の PlacedShape に変換
 */
fun NormalizedPlacedShape.toDomain(canvasWidth: Int, canvasHeight: Int): PlacedShape {
    return PlacedShape(
        shape = Polygon(
            points = shape.points.map { point ->
                Point(
                    Centimeter(point.x * canvasWidth),
                    Centimeter(point.y * canvasHeight)
                )
            }
        ),
        position = Point(
            Centimeter(position.x * canvasWidth),
            Centimeter(position.y * canvasHeight)
        ),
        rotation = Degree(rotation),
        color = color,
        name = name
    )
}

/**
 * Normalized point with dimensionless coordinates (0.0 - 1.0)
 * 無次元化された座標点（0.0 - 1.0）
 */
data class NormalizedPoint(
    val x: Float,  // 0.0 - 1.0
    val y: Float   // 0.0 - 1.0
)

/**
 * Normalized shape - generic shape representation with dimensionless coordinates
 * 無次元化された図形 - 汎用的な図形表現
 */
data class NormalizedShape(
    val points: List<NormalizedPoint>,  // Normalized vertices (0.0 - 1.0)
    val color: Color = Color.Green,
    val strokeWidth: Float = 3f
)

/**
 * Placed shape with normalized position
 * 配置された図形（無次元座標）
 */
data class NormalizedPlacedShape(
    val shape: NormalizedShape,
    val position: NormalizedPoint,  // Normalized position (0.0 - 1.0)
    val rotation: Float = 0f,       // Rotation in degrees
    val color: Color = Color.Green,
    val name: String = ""
)

/**
 * シンプル化された図形配置用キャンバス（Organism）
 * UI ライブラリとして分離可能な汎用コンポーネント
 * 
 * @param backgroundImageUrl Canvas背景として表示する画像のURL
 * @param backgroundShape 背景図形（無次元座標）
 * @param shapes 配置済みの図形リスト（無次元座標）
 * @param selectedShapeIndex 現在選択中の図形のインデックス
 * @param onShapePositionChanged スライダーによる図形位置変更時のコールバック
 * @param modifier Canvas全体に適用されるModifier
 */
@Composable
fun ShapeLayoutCanvas(
    backgroundImageUrl: String? = null,
    backgroundShape: NormalizedShape? = null,
    shapes: List<NormalizedPlacedShape> = emptyList(),
    selectedShapeIndex: Int? = null,
    onShapePositionChanged: (index: Int, newPosition: NormalizedPoint) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // Track actual canvas size using onSizeChanged
    var canvasSize by remember { mutableStateOf(IntSize(500, 500)) }

    val xSliderState = remember {
        SliderState(
            initialValue = 0.5f,
            valueRange = 0f..1f,
            onValueChange = { fraction ->
                // Update selected shape position when slider moves
                selectedShapeIndex?.let { index ->
                    if (index >= 0 && index < shapes.size) {
                        val currentShape = shapes[index]
                        val newPosition = NormalizedPoint(
                            x = fraction,
                            y = currentShape.position.y
                        )
                        onShapePositionChanged(index, newPosition)
                    }
                }
            }
        )
    }

    val ySliderState = remember {
        SliderState(
            initialValue = 0.5f,
            valueRange = 0f..1f,
            onValueChange = { fraction ->
                // Update selected shape position when slider moves
                selectedShapeIndex?.let { index ->
                    if (index >= 0 && index < shapes.size) {
                        val currentShape = shapes[index]
                        val newPosition = NormalizedPoint(
                            x = currentShape.position.x,
                            y = fraction
                        )
                        onShapePositionChanged(index, newPosition)
                    }
                }
            }
        )
    }

    // Update slider values when a shape is selected
    LaunchedEffect(selectedShapeIndex) {
        selectedShapeIndex?.let { index ->
            if (index >= 0 && index < shapes.size) {
                val selectedShape = shapes[index]
                xSliderState.updateValueFromFraction(selectedShape.position.x)
                ySliderState.updateValueFromFraction(selectedShape.position.y)
            }
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
                ) {
                    val width = size.width
                    val height = size.height

                    // Draw background shape
                    backgroundShape?.let { bgShape ->
                        val bgOffsets = bgShape.points.map { point ->
                            Offset(point.x * width, point.y * height)
                        }
                        if (bgOffsets.isNotEmpty()) {
                            drawPoints(
                                points = bgOffsets + bgOffsets.first(),
                                pointMode = PointMode.Polygon,
                                color = bgShape.color,
                                strokeWidth = bgShape.strokeWidth
                            )
                        }
                    }

                    // Draw all placed shapes
                    shapes.forEachIndexed { index, placedShape ->
                        val isSelected = index == selectedShapeIndex
                        val shapeOffsets = placedShape.shape.points.map { point ->
                            val centerX = placedShape.position.x * width
                            val centerY = placedShape.position.y * height
                            
                            // Apply rotation if needed
                            val x = point.x * width
                            val y = point.y * height
                            
                            if (placedShape.rotation != 0f) {
                                val angle = Math.toRadians(placedShape.rotation.toDouble())
                                val cos = kotlin.math.cos(angle).toFloat()
                                val sin = kotlin.math.sin(angle).toFloat()
                                val rotatedX = centerX + (x * cos - y * sin)
                                val rotatedY = centerY + (x * sin + y * cos)
                                Offset(rotatedX, rotatedY)
                            } else {
                                Offset(centerX + x, centerY + y)
                            }
                        }
                        
                        if (shapeOffsets.isNotEmpty()) {
                            drawPoints(
                                points = shapeOffsets + shapeOffsets.first(),
                                pointMode = PointMode.Polygon,
                                color = if (isSelected) Color.Blue else placedShape.color,
                                strokeWidth = if (isSelected) 5f else placedShape.shape.strokeWidth
                            )
                        }
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
    PuzzroomTheme {
        val shapes = listOf(
            NormalizedPlacedShape(
                shape = NormalizedShape(
                    points = listOf(
                        NormalizedPoint(0f, 0f),
                        NormalizedPoint(0.2f, 0f),
                        NormalizedPoint(0.2f, 0.15f),
                        NormalizedPoint(0f, 0.15f)
                    ),
                    color = Color.Green
                ),
                position = NormalizedPoint(0.3f, 0.3f),
                color = Color.Green,
                name = "Shape 1"
            ),
            NormalizedPlacedShape(
                shape = NormalizedShape(
                    points = listOf(
                        NormalizedPoint(0f, 0f),
                        NormalizedPoint(0.15f, 0f),
                        NormalizedPoint(0.15f, 0.15f),
                        NormalizedPoint(0f, 0.15f)
                    ),
                    color = Color.Red
                ),
                position = NormalizedPoint(0.6f, 0.5f),
                color = Color.Red,
                name = "Shape 2"
            )
        )
        
        ShapeLayoutCanvas(
            shapes = shapes,
            selectedShapeIndex = 0,
            backgroundShape = NormalizedShape(
                points = listOf(
                    NormalizedPoint(0.1f, 0.1f),
                    NormalizedPoint(0.9f, 0.1f),
                    NormalizedPoint(0.9f, 0.9f),
                    NormalizedPoint(0.1f, 0.9f)
                ),
                color = Color.Gray,
                strokeWidth = 2f
            )
        )
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
