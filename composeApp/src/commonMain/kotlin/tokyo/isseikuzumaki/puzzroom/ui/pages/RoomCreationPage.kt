package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import tokyo.isseikuzumaki.puzzroom.domain.Degree.Companion.degree
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.RoomShapeType
import tokyo.isseikuzumaki.puzzroom.ui.organisms.PlacedShape
import tokyo.isseikuzumaki.puzzroom.ui.templates.RoomCreationTemplate
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme

/**
 * Room creation page - allows users to create rooms by placing walls and doors
 * 
 * State management for room creation, following the same pattern as FurniturePlacementPage
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomCreationPage(
    backgroundImageUrl: String? = null,
    onRoomCreated: (List<PlacedShape>) -> Unit = { _ -> },
    onCancel: () -> Unit = { },
    modifier: Modifier = Modifier
) {
    var selectedShapeType by remember { mutableStateOf<RoomShapeType?>(null) }
    var shapeRotation by remember { mutableStateOf(0f) }
    var placedShapes by remember { mutableStateOf<List<PlacedShape>>(emptyList()) }
    var selectedShapeIndex by remember { mutableStateOf<Int?>(null) }
    var currentPosition by remember { mutableStateOf<Point?>(null) }
    
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    
    RoomCreationTemplate(
        backgroundImageUrl = backgroundImageUrl,
        shapeTypeToPlace = selectedShapeType,
        shapeRotation = shapeRotation,
        placeableShapeTypes = listOf(
            RoomShapeType.WALL,
            RoomShapeType.DOOR,
            RoomShapeType.WINDOW
        ),
        placedShapes = placedShapes,
        selectedShapeIndex = selectedShapeIndex,
        onPositionUpdate = { position ->
            currentPosition = position
            // Auto-place shape when position is updated
            if (position != null && selectedShapeType != null) {
                val newShape = when (selectedShapeType!!) {
                    RoomShapeType.WALL -> {
                        // Create a simple wall as a line
                        PlacedShape(
                            shape = Polygon(
                                points = listOf(
                                    Point(0.cm(), 0.cm()),
                                    Point(200.cm(), 0.cm())
                                )
                            ),
                            position = position,
                            rotation = shapeRotation.degree(),
                            color = Color.Gray,
                            name = "Wall ${placedShapes.size + 1}"
                        )
                    }
                    RoomShapeType.DOOR -> {
                        // Create a door shape
                        PlacedShape(
                            shape = Polygon(
                                points = listOf(
                                    Point(0.cm(), 0.cm()),
                                    Point(80.cm(), 0.cm()),
                                    Point(80.cm(), 20.cm()),
                                    Point(0.cm(), 20.cm())
                                )
                            ),
                            position = position,
                            rotation = shapeRotation.degree(),
                            color = Color.Red,
                            name = "Door ${placedShapes.size + 1}"
                        )
                    }
                    RoomShapeType.WINDOW -> {
                        // Create a window shape
                        PlacedShape(
                            shape = Polygon(
                                points = listOf(
                                    Point(0.cm(), 0.cm()),
                                    Point(60.cm(), 0.cm()),
                                    Point(60.cm(), 10.cm()),
                                    Point(0.cm(), 10.cm())
                                )
                            ),
                            position = position,
                            rotation = shapeRotation.degree(),
                            color = Color.Blue,
                            name = "Window ${placedShapes.size + 1}"
                        )
                    }
                }
                // Only add if not already present at this position
                if (!placedShapes.any { it.position == position }) {
                    placedShapes = placedShapes + newShape
                }
            }
        },
        onShapeSelected = { index ->
            selectedShapeIndex = index
        },
        onShapeMoved = { index, newPosition ->
            placedShapes = placedShapes.mapIndexed { i, shape ->
                if (i == index) {
                    shape.copy(position = newPosition)
                } else {
                    shape
                }
            }
        },
        onShapeTypeSelected = { shapeType ->
            selectedShapeType = shapeType
            selectedShapeIndex = null
        },
        bottomSheetState = bottomSheetState,
        onCancelBottomSheet = {
            // Handle bottom sheet cancel
        },
        onSaveBottomSheet = {
            // Handle bottom sheet save
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun RoomCreationPagePreview() {
    PuzzroomTheme {
        RoomCreationPage()
    }
}
