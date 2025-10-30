package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppSlider
import tokyo.isseikuzumaki.puzzroom.ui.atoms.SliderOrientation
import tokyo.isseikuzumaki.puzzroom.ui.molecules.FloorPlanBackgroundImage
import tokyo.isseikuzumaki.puzzroom.ui.state.SliderState
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme
import kotlin.math.sqrt


/**
 * Normalized point with dimensionless coordinates (0.0 - 1.0)
 * 無次元化された座標点（0.0 - 1.0）
 */
data class NormalizedPoint(
    val x: Float = 0.5f,  // 0.0 - 1.0
    val y: Float = 0.5f  // 0.0 - 1.0
) {
    fun distanceTo(other: NormalizedPoint): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return sqrt(dx * dx + dy * dy)
    }
    
    operator fun plus(other: NormalizedPoint) = NormalizedPoint(x + other.x, y + other.y)
    operator fun minus(other: NormalizedPoint) = NormalizedPoint(x - other.x, y - other.y)
    operator fun times(scalar: Float) = NormalizedPoint(x * scalar, y * scalar)
    
    fun dot(other: NormalizedPoint): Float = x * other.x + y * other.y
    fun lengthSquared(): Float = x * x + y * y
}

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
 * Calculate the shortest distance from a point to a line segment
 * 点と線分間の最短距離を計算
 * 
 * @param point The point to measure from
 * @param segmentStart The start point of the line segment
 * @param segmentEnd The end point of the line segment
 * @return The shortest distance from the point to the line segment
 */
internal fun distanceFromPointToLineSegment(
    point: NormalizedPoint,
    segmentStart: NormalizedPoint,
    segmentEnd: NormalizedPoint
): Float {
    // Vector from segment start to end
    val segment = segmentEnd - segmentStart
    val segmentLengthSquared = segment.lengthSquared()
    
    // Handle degenerate case where segment is a point
    if (segmentLengthSquared < 1e-10f) {
        return point.distanceTo(segmentStart)
    }
    
    // Vector from segment start to point
    val toPoint = point - segmentStart
    
    // Project point onto the line (infinite)
    // t represents the position along the segment (0 = start, 1 = end)
    val t = (toPoint.dot(segment) / segmentLengthSquared).coerceIn(0f, 1f)
    
    // Find the closest point on the segment
    val closestPoint = segmentStart + (segment * t)
    
    // Return distance from point to closest point on segment
    return point.distanceTo(closestPoint)
}

/**
 * Check if a normalized point is near a placed shape
 * Checks if the point is within hitTolerance distance from any line segment of the shape
 * 図形を構成する線分のいずれかの付近にポイントがあるかチェック
 */
internal fun isPointNearShape(
    point: NormalizedPoint,
    shape: NormalizedPlacedShape,
    canvasSize: IntSize
): Boolean {
    // Set hit tolerance to approximately 30 pixels (reasonable for touch/click targets)
    // 画面サイズに対する相対的な許容範囲を設定（約30ピクセル相当）
    val hitTolerance = 30f / canvasSize.width.coerceAtLeast(1)

    // Transform shape points to world coordinates
    val shapePoints = shape.shape.points.map { shapePoint ->
        NormalizedPoint(
            x = shape.position.x + shapePoint.x,
            y = shape.position.y + shapePoint.y
        )
    }

    if (shapePoints.isEmpty()) return false

    // Check distance from point to each line segment of the shape
    for (i in shapePoints.indices) {
        val segmentStart = shapePoints[i]
        val segmentEnd = shapePoints[(i + 1) % shapePoints.size]
        
        val distance = distanceFromPointToLineSegment(point, segmentStart, segmentEnd)
        
        if (distance <= hitTolerance) {
            return true
        }
    }
    
    return false
}

/**
 * シンプル化された図形配置用キャンバス（Organism）
 * UI ライブラリとして分離可能な汎用コンポーネント
 *
 * @param backgroundImageUrl Canvas背景として表示する画像のURL
 * @param backgroundShape 背景図形（無次元座標）
 * @param selectedShape 選択中の図形（無次元座標）。スライダーで移動可能。
 * @param unselectedShapes 選択されていない図形リスト（無次元座標）。表示のみ。
 * @param onSelectedShapePosition 選択中の図形の位置変更時のコールバック
 * @param onShapeSelected タップされた図形のインデックスを返すコールバック
 * @param modifier Canvas全体に適用されるModifier
 */
@Composable
fun ShapeLayoutCanvas(
    backgroundImageUrl: String? = null,
    backgroundShape: NormalizedShape? = null,
    selectedShape: NormalizedPlacedShape? = null,
    unselectedShapes: List<NormalizedPlacedShape> = emptyList(),
    onSelectedShapePosition: (NormalizedPoint) -> Unit = { _ -> },
    onShapeSelected: (Int) -> Unit = { _ -> },
    modifier: Modifier = Modifier,
) {
    var sliderPosition by remember { mutableStateOf(NormalizedPoint()) }

    val xSliderState = remember {
        SliderState(
            initialValue = selectedShape?.position?.x ?: 0.5f,
            valueRange = 0f..1f,
            onValueChange = { fraction ->
                sliderPosition = sliderPosition.copy(x = fraction)
            },
            onSliderReleased = {
                onSelectedShapePosition(sliderPosition)
            }
        )
    }

    val ySliderState = remember {
        SliderState(
            initialValue = selectedShape?.position?.y ?: 0.5f,
            valueRange = 0f..1f,
            onValueChange = { fraction ->
                sliderPosition = sliderPosition.copy(y = 1f - fraction)
            },
            onSliderReleased = {
                onSelectedShapePosition(sliderPosition)
            }
        )
    }

    // Update slider values when a shape is selected
    LaunchedEffect(selectedShape) {
        selectedShape?.let { shape ->
            xSliderState.updateValueFromFraction(shape.position.x)
            ySliderState.updateValueFromFraction(1f - shape.position.y)
        }
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

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
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                // Convert tap to normalized coordinates
                                if (canvasSize.width > 0 && canvasSize.height > 0) {
                                    val normalizedTap = NormalizedPoint(
                                        x = tapOffset.x / canvasSize.width,
                                        y = tapOffset.y / canvasSize.height
                                    )

                                    // Check if tap hits any unselected shape (in reverse order - top to bottom)
                                    unselectedShapes.asReversed().forEachIndexed { reversedIndex, shape ->
                                        val actualIndex = unselectedShapes.size - 1 - reversedIndex
                                        if (isPointNearShape(normalizedTap, shape, canvasSize)) {
                                            onShapeSelected(actualIndex)
                                            return@detectTapGestures
                                        }
                                    }
                                }
                            }
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

                    // Draw unselected shapes
                    unselectedShapes.forEach { placedShape ->
                        val shapeOffsets = placedShape.shape.points.map { point ->
                            val centerX = placedShape.position.x * width
                            val centerY = placedShape.position.y * height

                            // Apply rotation
                            val angleRad = placedShape.rotation * (kotlin.math.PI / 180.0)
                            val cos = kotlin.math.cos(angleRad)
                            val sin = kotlin.math.sin(angleRad)

                            val x = point.x * width
                            val y = point.y * height

                            val rotatedX = x * cos - y * sin
                            val rotatedY = x * sin + y * cos

                            Offset(centerX + rotatedX.toFloat(), centerY + rotatedY.toFloat())
                        }

                        if (shapeOffsets.isNotEmpty()) {
                            drawPoints(
                                points = shapeOffsets + shapeOffsets.first(),
                                pointMode = PointMode.Polygon,
                                color = placedShape.color,
                                strokeWidth = placedShape.shape.strokeWidth
                            )
                        }
                    }

                    // Draw selected shape (highlighted)
                    selectedShape?.let { placedShape ->
                        val shapeOffsets = placedShape.shape.points.map { point ->
                            val centerX = sliderPosition.x * width
                            val centerY = sliderPosition.y * height

                            // Apply rotation
                            val angleRad = placedShape.rotation * (kotlin.math.PI / 180.0)
                            val cos = kotlin.math.cos(angleRad)
                            val sin = kotlin.math.sin(angleRad)

                            val x = point.x * width
                            val y = point.y * height

                            val rotatedX = x * cos - y * sin
                            val rotatedY = x * sin + y * cos

                            Offset(centerX + rotatedX.toFloat(), centerY + rotatedY.toFloat())
                        }

                        if (shapeOffsets.isNotEmpty()) {
                            drawPoints(
                                points = shapeOffsets + shapeOffsets.first(),
                                pointMode = PointMode.Polygon,
                                color = Color.Blue,  // Highlight selected shape
                                strokeWidth = 5f
                            )
                        }
                    }
                }
            },
            modifier = Modifier,
            topContent = { selectedShape?.let { AppSlider(state = xSliderState) } },
            bottomContent = { selectedShape?.let { AppSlider(state = xSliderState) } },
            leftContent = {
                selectedShape?.let {
                    AppSlider(state = ySliderState, orientation = SliderOrientation.Vertical)
                }
            },
            rightContent = {
                selectedShape?.let {
                    AppSlider(state = ySliderState, orientation = SliderOrientation.Vertical)
                }
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
        val shape1 = NormalizedPlacedShape(
            shape = NormalizedShape(
                points = listOf(
                    NormalizedPoint(0f, 0f),
                    NormalizedPoint(0.2f, 0f),
                    NormalizedPoint(0.2f, 0.15f),
                    NormalizedPoint(0f, 0.15f)
                ),
                color = Color.Green
            ),
            position = NormalizedPoint(0.5f, 0.5f),
            color = Color.Green,
            name = "Shape 1"
        )
        
        val shape2 = NormalizedPlacedShape(
            shape = NormalizedShape(
                points = listOf(
                    NormalizedPoint(0f, 0f),
                    NormalizedPoint(0.15f, 0f),
                    NormalizedPoint(0.15f, 0.15f),
                    NormalizedPoint(0f, 0.15f)
                ),
                color = Color.Red
            ),
            position = NormalizedPoint(0.3f, 0.3f),
            color = Color.Red,
            name = "Shape 2"
        )
        
        ShapeLayoutCanvas(
            selectedShape = shape1,
            unselectedShapes = listOf(shape2),
            backgroundShape = NormalizedShape(
                points = listOf(
                    NormalizedPoint(0.1f, 0.1f),
                    NormalizedPoint(0.9f, 0.1f),
                    NormalizedPoint(0.9f, 0.9f),
                    NormalizedPoint(0.1f, 0.9f)
                ),
                color = Color.Gray,
                strokeWidth = 2f
            ),
        )
    }
}
