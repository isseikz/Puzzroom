package tokyo.isseikuzumaki.puzzroom.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import tokyo.isseikuzumaki.puzzroom.domain.Degree
import tokyo.isseikuzumaki.puzzroom.domain.Degree.Companion.degree
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.domain.RoomShapeType
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ButtonToCreate
import tokyo.isseikuzumaki.puzzroom.ui.organisms.NormalizedPlacedShape
import tokyo.isseikuzumaki.puzzroom.ui.organisms.NormalizedPoint
import tokyo.isseikuzumaki.puzzroom.ui.organisms.NormalizedShape
import tokyo.isseikuzumaki.puzzroom.ui.organisms.RoomShapeSelector
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ShapeAttributeForm
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ShapeLayoutCanvas
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme

/**
 * Convert a list of PlacedShape (walls/doors) to a Room
 *
 * This function accepts any layout of shapes (walls/doors) and creates a room from them.
 * The polygon does NOT need to be closed - users can save incomplete layouts.
 *
 * The resulting room polygon contains all points from all placed shapes,
 * translated to their absolute positions.
 *
 * @param shapes List of walls/doors that make up the room layout
 * @param roomName Name for the created room
 * @return Room with polygon containing all shape points
 */
fun convertPlacedShapesToRoom(
    shapes: List<PlacedShape>,
    roomName: String = "New Room"
): Room {
    // Collect all points from all shapes, translated to absolute positions
    val allPoints = mutableListOf<Point>()

    shapes.forEach { placedShape ->
        placedShape.shape.points.forEach { point ->
            // Translate point by the shape's position
            val translatedPoint = Point(
                x = Centimeter(point.x.value + placedShape.position.x.value),
                y = Centimeter(point.y.value + placedShape.position.y.value)
            )
            allPoints.add(translatedPoint)
        }
    }

    // If no shapes provided, create a minimal polygon with at least 3 points
    // to satisfy the Room data structure
    val finalPoints = if (allPoints.isEmpty()) {
        listOf(
            Point(Centimeter(0), Centimeter(0)),
            Point(Centimeter(100), Centimeter(0)),
            Point(Centimeter(100), Centimeter(100))
        )
    } else {
        allPoints
    }

    return Room(
        name = roomName,
        shape = Polygon(points = finalPoints)
    )
}

/**
 * Placed shape with position and rotation (domain-specific types)
 */
data class PlacedShape(
    val shape: Polygon,
    val position: Point,
    val rotation: Degree = Degree(0f),
    val color: Color = Color.Green,
    val name: String = "",
) {
    fun normalize(spaceSize: IntSize): NormalizedPlacedShape {
        return NormalizedPlacedShape(
            shape = NormalizedShape(
                points = this.shape.points.map { point ->
                    NormalizedPoint(
                        x = point.x.value / spaceSize.width.toFloat(),
                        y = point.y.value / spaceSize.height.toFloat()
                    )
                }
            ),
            position = NormalizedPoint(
                x = this.position.x.value / spaceSize.width.toFloat(),
                y = this.position.y.value / spaceSize.height.toFloat()
            )
        )
    }

    companion object {
        fun createWall(width: Centimeter): PlacedShape {
            return PlacedShape(
                shape = Polygon(
                    points = listOf(
                        Point(Centimeter(0), Centimeter(0)),
                        Point(width, Centimeter(0))
                    )
                ),
                position = Point(Centimeter(0), Centimeter(0)),
                name = "Wall"
            )
        }

        fun createDoor(width: Centimeter): PlacedShape {
            // Simplified as a sector (future extension to arc)
            val points = mutableListOf<Point>()
            points.add(Point(Centimeter(0), Centimeter(0)))

            // Create vertices of the sector
            val steps = 10
            for (i in 0..steps) {
                val radians = (kotlin.math.PI * 0.5f * i / steps)
                val x = (width.value * kotlin.math.cos(radians)).toInt()
                val y = (width.value * kotlin.math.sin(radians)).toInt()
                points.add(Point(Centimeter(x), Centimeter(y)))
            }

            return PlacedShape(
                shape = Polygon(points),
                position = Point(Centimeter(0), Centimeter(0)),
                name = "Door"
            )
        }
    }
}

private data class RoomCreationUiState(
    val shapeTypeToPlace: RoomShapeType? = null,
    val editingShape: NormalizedPlacedShape? = null,
)

/**
 * 部屋作成テンプレート（Template）
 * 壁や扉を配置して部屋を作成するためのテンプレート
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomCreationTemplate(
    backgroundImageUrl: String? = null,
    placedShapes: List<PlacedShape> = emptyList(),
    onShapesChanged: (List<PlacedShape>) -> Unit = { },
    modifier: Modifier = Modifier
) {
    var spaceSize by remember { mutableStateOf(IntSize(1000, 1000)) }

    var currentShapes by remember { mutableStateOf(placedShapes.map { it.normalize(spaceSize) }) }
    var data by remember { mutableStateOf(RoomCreationUiState()) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    Column(
        verticalArrangement = Arrangement.Bottom
    ) {
        ShapeLayoutCanvas(
            selectedShape = data.editingShape,
            unselectedShapes = currentShapes,
            onSelectedShapePosition = { newPosition ->
                data = data.copy(
                    editingShape = data.editingShape?.copy(
                        position = newPosition
                    )
                )
            },
            onShapeSelected = { selectedIndex ->
                val newSelectedShape = currentShapes.getOrNull(selectedIndex)
                val newUnselectedShapes = currentShapes.toMutableList()
                    .apply {
                        removeAt(selectedIndex)
                        data.editingShape?.let { add(it) }
                    }
                currentShapes = newUnselectedShapes
                data = data.copy(
                    shapeTypeToPlace = null,
                    editingShape = newSelectedShape
                )
                showBottomSheet = true
            },
            backgroundImageUrl = backgroundImageUrl,
            modifier = modifier.weight(1f)
        )

        RoomShapeSelector(
            items = RoomShapeType.entries,
            selectedShape = data.shapeTypeToPlace,
            onShapeSelected = { shapeType: RoomShapeType ->
                data.editingShape?.let { currentShapes = currentShapes + it }

                val newShape = when (shapeType) {
                    RoomShapeType.WALL -> PlacedShape.createWall(300.cm).normalize(spaceSize)
                    RoomShapeType.DOOR -> PlacedShape.createDoor(100.cm).normalize(spaceSize)
                    else -> PlacedShape.createWall(100.cm).normalize(spaceSize) // TODO
                }
                data = data.copy(
                    shapeTypeToPlace = shapeType,
                    editingShape = newShape
                )

                showBottomSheet = true
            },
            modifier = Modifier.fillMaxWidth().height(100.dp)
                .background(color = MaterialTheme.colorScheme.secondaryContainer)
        )
        
        ButtonToCreate(
            onClick = {
                // Add current editing shape to the list (if any)
                val shapesToSave = if (data.editingShape != null) {
                    currentShapes + data.editingShape!!
                } else {
                    currentShapes
                }

                // Convert normalized shapes back to PlacedShape and notify parent
                // This works even if shapesToSave is empty - users can save an empty room layout
                val denormalizedShapes = shapesToSave.map { normalizedShape ->
                    PlacedShape(
                        shape = Polygon(
                            points = normalizedShape.shape.points.map { point ->
                                Point(
                                    Centimeter((point.x * spaceSize.width).toInt()),
                                    Centimeter((point.y * spaceSize.height).toInt())
                                )
                            }
                        ),
                        position = Point(
                            Centimeter((normalizedShape.position.x * spaceSize.width).toInt()),
                            Centimeter((normalizedShape.position.y * spaceSize.height).toInt())
                        ),
                        rotation = Degree(normalizedShape.rotation),
                        color = normalizedShape.color,
                        name = normalizedShape.name
                    )
                }

                // Notify parent to save current layout (even if incomplete/empty)
                onShapesChanged(denormalizedShapes)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
                .background(color = MaterialTheme.colorScheme.secondaryContainer)
        )

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = bottomSheetState
            ) {
                ShapeAttributeForm(
                    onDismiss = {
                        showBottomSheet = false
                    },
                    onSave = { modalData ->
                        val shape = data.editingShape ?: return@ShapeAttributeForm

                        // FIXME たぶんここの計算式間違ってる (元々の Shape の大きさと、modalData で指定された大きさの比率を計算して拡大縮小する)
                        val firstEdgeLength = spaceSize.width * shape.shape.points.first()
                            .distanceTo(shape.shape.points[1])
                        val scale = modalData.width.value / firstEdgeLength

                        data = data.copy(
                            editingShape = data.editingShape?.let {
                                it.copy(
                                    shape = it.shape.points.map { point ->
                                        NormalizedPoint(
                                            x = point.x * scale,
                                            y = point.y * scale
                                        )
                                    }.let { points ->
                                        NormalizedShape(points)
                                    },
                                    rotation = modalData.angle.value
                                )
                            }
                        )
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun RoomCreationTemplatePreview() {
    PuzzroomTheme {
        RoomCreationTemplate(
            placedShapes = listOf(
                PlacedShape(
                    shape = Polygon(
                        points = listOf(
                            Point(0.cm(), 0.cm()),
                            Point(200.cm(), 0.cm())
                        )
                    ),
                    position = Point(100.cm(), 100.cm()),
                    rotation = 0f.degree(),
                    color = Color.Gray,
                    name = "Wall 1"
                ),
                PlacedShape(
                    shape = Polygon(
                        points = listOf(
                            Point(0.cm(), 0.cm()),
                            Point(80.cm(), 0.cm()),
                            Point(80.cm(), 20.cm()),
                            Point(0.cm(), 20.cm())
                        )
                    ),
                    position = Point(250.cm(), 150.cm()),
                    rotation = 45f.degree(),
                    color = Color.Red,
                    name = "Door 1"
                )
            )
        )
    }
}
