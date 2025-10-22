package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import tokyo.isseikuzumaki.puzzroom.domain.Degree.Companion.degree
import tokyo.isseikuzumaki.puzzroom.domain.Furniture
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ButtonToCreate
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ShapeLayoutCanvas
import tokyo.isseikuzumaki.puzzroom.ui.organisms.PlacedShape
import tokyo.isseikuzumaki.puzzroom.ui.organisms.FurnitureSelector
import tokyo.isseikuzumaki.puzzroom.ui.organisms.FurnitureCreationForm
import tokyo.isseikuzumaki.puzzroom.ui.state.PlacedFurniture
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme
import androidx.compose.ui.graphics.Color

/**
 * 家具配置ページ（Page）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FurniturePlacementTemplate(
    room: Room,
    backgroundImageUrl: String? = null,
    furnitureToPlace: Furniture? = null,
    furnitureRotation: Float = 0f,
    placeableItems: List<Furniture> = emptyList(),
    placedItems: List<PlacedFurniture> = emptyList(),
    selectedFurnitureIndex: Int? = null,
    onPositionUpdate: (position: Point?) -> Unit = { _ -> },
    onFurnitureSelected: (index: Int?) -> Unit = { _ -> },
    onFurnitureMoved: (index: Int, newPosition: Point) -> Unit = { _, _ -> },
    bottomSheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    ),
    onCancelBottomSheet: () -> Unit = { },
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    // Convert PlacedFurniture to PlacedShape for the generalized canvas
    val placedShapes = remember(placedItems) {
        placedItems.map { placedFurniture ->
            PlacedShape(
                shape = placedFurniture.furniture.shape,
                position = placedFurniture.position,
                rotation = placedFurniture.rotation,
                color = Color.Green,
                name = placedFurniture.furniture.name
            )
        }
    }

    // Convert Furniture to Polygon for the generalized canvas
    val shapeToPlace = furnitureToPlace?.shape

    Column(
        verticalArrangement = Arrangement.Bottom
    ) {
        ShapeLayoutCanvas(
            backgroundShape = room.shape,
            backgroundImageUrl = backgroundImageUrl,
            shapeToPlace = shapeToPlace,
            shapeRotation = furnitureRotation,
            placedShapes = placedShapes,
            selectedShapeIndex = selectedFurnitureIndex,
            onPositionUpdate = onPositionUpdate,
            onShapeSelected = onFurnitureSelected,
            onShapeMoved = onFurnitureMoved,
            modifier = modifier.weight(1f)
        )

        FurnitureSelector(
            items = placeableItems,
            selectedShape = furnitureToPlace,
            onShapeSelected = { /* 家具選択時の処理 */ },
            modifier = Modifier.fillMaxWidth().height(100.dp)
                .background(color = MaterialTheme.colorScheme.secondaryContainer)
        )
        ButtonToCreate(
            onClick = { showBottomSheet = true },
            modifier = Modifier.fillMaxWidth().height(56.dp)
                .background(color = MaterialTheme.colorScheme.secondaryContainer)
        )

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    // User dismissed by swipe down or tapping scrim
                    showBottomSheet = false
                },
                sheetState = bottomSheetState
            ) {
                FurnitureCreationForm(
                    onDismiss = {
                        // User cancelled the form
                        onCancelBottomSheet()
                        showBottomSheet = false
                    },
                    onSave = {
                        // User saved the form}
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview()
@Composable
private fun FurniturePlacementTemplatePreview() {
    PuzzroomTheme {
        FurniturePlacementTemplate(
            room = Room(
                name = "Sample Room", shape = Polygon(
                    points = listOf(
                        Point(0.cm(), 0.cm()),
                        Point(500.cm(), 0.cm()),
                        Point(500.cm(), 400.cm()),
                        Point(0.cm(), 400.cm())
                    )
                )
            ),
            placeableItems = listOf(
                Furniture(
                    name = "Table",
                    shape = Polygon(
                        points = listOf(
                            Point(0.cm(), 0.cm()),
                            Point(120.cm(), 0.cm()),
                            Point(120.cm(), 80.cm()),
                            Point(0.cm(), 80.cm())
                        )
                    )
                ),
                Furniture(
                    name = "Chair",
                    shape = Polygon(
                        points = listOf(
                            Point(0.cm(), 0.cm()),
                            Point(50.cm(), 0.cm()),
                            Point(50.cm(), 50.cm()),
                            Point(0.cm(), 50.cm())
                        )
                    )
                ),
                Furniture(
                    name = "Piano",
                    shape = Polygon(
                        points = listOf(
                            Point(0.cm(), 0.cm()),
                            Point(150.cm(), 0.cm()),
                            Point(150.cm(), 100.cm()),
                            Point(0.cm(), 100.cm())
                        )
                    )
                )
            ),
            placedItems = listOf(
                PlacedFurniture(
                    furniture = Furniture(
                        name = "Table",
                        shape = Polygon(
                            points = listOf(
                                Point(0.cm(), 0.cm()),
                                Point(120.cm(), 0.cm()),
                                Point(120.cm(), 80.cm()),
                                Point(0.cm(), 80.cm())
                            )
                        )
                    ),
                    position = Point(100.cm(), 100.cm()),
                    rotation = 0f.degree()
                ),
                PlacedFurniture(
                    furniture = Furniture(
                        name = "Chair",
                        shape = Polygon(
                            points = listOf(
                                Point(0.cm(), 0.cm()),
                                Point(50.cm(), 0.cm()),
                                Point(50.cm(), 50.cm()),
                                Point(0.cm(), 50.cm())
                            )
                        )
                    ),
                    position = Point(200.cm(), 150.cm()),
                    rotation = 45f.degree()
                ),
                PlacedFurniture(
                    furniture = Furniture(
                        name = "Piano",
                        shape = Polygon(
                            points = listOf(
                                Point(0.cm(), 0.cm()),
                                Point(150.cm(), 0.cm()),
                                Point(150.cm(), 100.cm()),
                                Point(0.cm(), 100.cm())
                            ),
                        )
                    ),
                    position = Point(300.cm(), 250.cm()),
                    rotation = 90f.degree()
                )
            ),
        )
    }
}
