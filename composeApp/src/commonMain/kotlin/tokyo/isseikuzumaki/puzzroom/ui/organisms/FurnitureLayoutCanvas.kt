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
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import tokyo.isseikuzumaki.puzzroom.domain.Degree.Companion.degree
import tokyo.isseikuzumaki.puzzroom.domain.Furniture
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppSlider
import tokyo.isseikuzumaki.puzzroom.ui.atoms.SliderOrientation
import tokyo.isseikuzumaki.puzzroom.ui.molecules.FloorPlanBackgroundImage
import tokyo.isseikuzumaki.puzzroom.ui.state.PlacedFurniture
import tokyo.isseikuzumaki.puzzroom.ui.state.SliderState
import tokyo.isseikuzumaki.puzzroom.ui.state.rotateAroundCenterOffsets
import tokyo.isseikuzumaki.puzzroom.ui.state.toCanvasOffset
import tokyo.isseikuzumaki.puzzroom.ui.state.toPoint
import kotlin.math.roundToInt

/**
 * 家具配置用のキャンバス（Organism）
 */
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

    val xSliderState = remember {
        SliderState(
            initialValue = 0.5f,
            valueRange = 0f..1f
        )
    }

    val ySliderState = remember {
        SliderState(
            initialValue = 0.5f,
            valueRange = 0f..1f
        )
    }

    // 位置が更新されたら通知
    LaunchedEffect(furniturePosition) {
        if (!isDragging) {
            onPositionUpdate(furniturePosition?.toPoint())
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
                                                val clickedIndex = placedFurnitures.indexOfLast {
                                                    it.contains(clickedPoint)
                                                }

                                                if (clickedIndex >= 0) {
                                                    onFurnitureSelected(clickedIndex)
                                                    isDragging = true
                                                    dragStartPosition = position
                                                    draggedFurnitureOriginalPosition =
                                                        placedFurnitures[clickedIndex].position
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
                                                    draggedFurnitureOriginalPosition!!.x + delta.x.roundToInt().cm,
                                                    draggedFurnitureOriginalPosition!!.y + delta.y.roundToInt().cm
                                                )
                                                onFurnitureMoved(
                                                    selectedFurnitureIndex,
                                                    newPosition
                                                )
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
                        points = room.shape.points.map { it.toCanvasOffset() } + room.shape.points.first()
                            .toCanvasOffset(),
                        pointMode = PointMode.Polygon,
                        color = Color.Gray,
                        strokeWidth = 3f
                    )

                    // 配置済みの家具を描画
                    placedFurnitures.forEachIndexed { index, placed ->
                        val rotatedPoints = placed.furniture.shape.rotateAroundCenterOffsets(
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
                        val rotatedPoints = furnitureToPlace.shape.rotateAroundCenterOffsets(
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

