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
import androidx.compose.runtime.LaunchedEffect
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
import tokyo.isseikuzumaki.puzzroom.domain.Furniture
import tokyo.isseikuzumaki.puzzroom.domain.PlacedShapeData
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.ui.molecules.ZoomSlider
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ButtonToCreate
import tokyo.isseikuzumaki.puzzroom.ui.organisms.FurnitureCreationForm
import tokyo.isseikuzumaki.puzzroom.ui.organisms.FurnitureSelector
import tokyo.isseikuzumaki.puzzroom.ui.organisms.NormalizedPlacedShape
import tokyo.isseikuzumaki.puzzroom.ui.organisms.NormalizedPoint
import tokyo.isseikuzumaki.puzzroom.ui.organisms.NormalizedShape
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ShapeLayoutCanvas
import tokyo.isseikuzumaki.puzzroom.ui.state.PlacedFurniture
import tokyo.isseikuzumaki.puzzroom.ui.state.SliderState
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme

private data class FurniturePlacementUiState(
    val selectedFurnitureIndex: Int? = null,
    val editingFurniture: NormalizedPlacedShape? = null,
)

/**
 * 家具配置テンプレート（Template）
 * 部屋の形状を背景にして、家具を配置するためのテンプレート
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FurniturePlacementTemplate(
    room: Room,
    backgroundImageUrl: String? = null,
    placeableItems: List<Furniture> = emptyList(),
    placedItems: List<PlacedFurniture> = emptyList(),
    onFurnitureChanged: (List<PlacedFurniture>) -> Unit = { },
    modifier: Modifier = Modifier
) {
    var spaceSize by remember { mutableStateOf(IntSize(1000, 1000)) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    var data by remember { mutableStateOf(FurniturePlacementUiState()) }

    val zoomSliderState = remember {
        SliderState(
            initialValue = 1000f,
            valueRange = 500f..1500f,
            onValueChange = { newSize ->
                spaceSize = IntSize(newSize.toInt(), newSize.toInt())
            }
        )
    }

    // Helper function to denormalize shapes and notify parent
    fun notifyFurnitureChanged(normalizedShapes: List<NormalizedPlacedShape>) {
        val denormalizedFurniture = normalizedShapes.map { normalizedShape ->
            PlacedFurniture(
                furniture = Furniture(
                    name = normalizedShape.name,
                    shape = Polygon(
                        points = normalizedShape.shape.points.map { point ->
                            Point(
                                Centimeter((point.x * spaceSize.width).toInt()),
                                Centimeter((point.y * spaceSize.height).toInt())
                            )
                        }
                    )
                ),
                position = Point(
                    Centimeter((normalizedShape.position.x * spaceSize.width).toInt()),
                    Centimeter((normalizedShape.position.y * spaceSize.height).toInt())
                ),
                rotation = Degree(normalizedShape.rotation)
            )
        }
        onFurnitureChanged(denormalizedFurniture)
    }

    // Convert placed furniture to normalized shapes
    var currentShapes by remember(spaceSize) {
        mutableStateOf(
            placedItems.map { placedFurniture ->
                NormalizedPlacedShape(
                    shape = NormalizedShape(
                        points = placedFurniture.furniture.shape.points.map { point ->
                            NormalizedPoint(
                                x = point.x.value / spaceSize.width.toFloat(),
                                y = point.y.value / spaceSize.height.toFloat()
                            )
                        },
                        color = Color.Green
                    ),
                    position = NormalizedPoint(
                        x = placedFurniture.position.x.value / spaceSize.width.toFloat(),
                        y = placedFurniture.position.y.value / spaceSize.height.toFloat()
                    ),
                    rotation = placedFurniture.rotation.value,
                    color = Color.Green,
                    name = placedFurniture.furniture.name
                )
            }
        )
    }

    // Auto-notify parent when furniture state changes
    LaunchedEffect(data.editingFurniture) {
        val allShapes = if (currentShapes.isNotEmpty() && data.editingFurniture != null) {
            currentShapes - currentShapes.last() + data.editingFurniture!!
        } else {
            currentShapes
        }
        notifyFurnitureChanged(allShapes)
    }

    Column(
        verticalArrangement = Arrangement.Bottom
    ) {
        ShapeLayoutCanvas(
            backgroundImageUrl = backgroundImageUrl,
            backgroundShapes = room.shapes.map { (shape, position, rotation, colorArgb, name) ->
                NormalizedPlacedShape(
                    shape = NormalizedShape(
                        shape.points.map { (x, y) ->
                            NormalizedPoint(
                                x = (x.value + position.x.value) / spaceSize.width.toFloat(),
                                y = (y.value + position.y.value) / spaceSize.height.toFloat()
                            )
                        },
                        color = Color.Gray,
                        strokeWidth = 2f
                    ),
                    position = NormalizedPoint(
                        x = position.x.value / spaceSize.width.toFloat(),
                        y = position.y.value / spaceSize.height.toFloat()
                    ),
                    rotation = rotation.value,
                    color = Color.Gray,
                    name = name
                )
            },
            selectedShape = data.editingFurniture,
            unselectedShapes = currentShapes,
            onSelectedShapePosition = { newPosition ->
                data = data.copy(
                    editingFurniture = data.editingFurniture?.copy(
                        position = newPosition
                    )
                )
            },
            onShapeSelected = { index ->
                // Save current editing furniture if any
                val updatedShapes = if (data.editingFurniture != null) {
                    currentShapes + data.editingFurniture!!
                } else {
                    currentShapes
                }

                // Select the tapped shape for editing
                val selectedShape = currentShapes.getOrNull(index)
                if (selectedShape != null) {
                    // Remove the selected shape from currentShapes and set it as editingFurniture
                    currentShapes = updatedShapes.filterIndexed { i, _ -> i != index }
                    data = data.copy(
                        editingFurniture = selectedShape,
                        selectedFurnitureIndex = index
                    )
                }
            },
            modifier = modifier.weight(1f)
        )

        ZoomSlider(
            sliderState = zoomSliderState,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.secondaryContainer)
        )

        FurnitureSelector(
            items = placeableItems,
            selectedShape = null,
            onShapeSelected = { furniture ->
                // Add current editing furniture to the list
                val updatedShapes = if (data.editingFurniture != null) {
                    currentShapes + data.editingFurniture!!
                } else {
                    currentShapes
                }
                currentShapes = updatedShapes

                // Create new furniture shape to place
                val newShape = NormalizedPlacedShape(
                    shape = NormalizedShape(
                        points = furniture.shape.points.map { point ->
                            NormalizedPoint(
                                x = point.x.value / spaceSize.width.toFloat(),
                                y = point.y.value / spaceSize.height.toFloat()
                            )
                        },
                        color = Color.Green
                    ),
                    position = NormalizedPoint(0.5f, 0.5f),
                    rotation = 0f,
                    color = Color.Green,
                    name = furniture.name
                )
                data = data.copy(editingFurniture = newShape)
            },
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
                    showBottomSheet = false
                    // Finalize editing furniture when modal is dismissed
                    data.editingFurniture?.let { editingFurniture ->
                        currentShapes = currentShapes + editingFurniture
                        data = data.copy(editingFurniture = null, selectedFurnitureIndex = null)
                    }
                },
                sheetState = bottomSheetState
            ) {
                FurnitureCreationForm(
                    onDismiss = {
                        showBottomSheet = false
                        // Finalize editing furniture when form is dismissed
                        data.editingFurniture?.let { editingFurniture ->
                            currentShapes = currentShapes + editingFurniture
                            data = data.copy(editingFurniture = null, selectedFurnitureIndex = null)
                        }
                    },
                    onSave = { modalData ->
                        // Update editing furniture with modal data
                        data = data.copy(
                            editingFurniture = data.editingFurniture?.copy(
                                // TODO: update attributes based on modalData
                            )
                        )
                        showBottomSheet = false
                        // Finalize editing furniture
                        data.editingFurniture?.let { editingFurniture ->
                            currentShapes = currentShapes + editingFurniture
                            data = data.copy(editingFurniture = null, selectedFurnitureIndex = null)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun FurniturePlacementTemplatePreview() {
    PuzzroomTheme {
        FurniturePlacementTemplate(
            room = Room(
                name = "Sample Room",
                shapes = listOf(PlacedShapeData(
                    shape = Polygon(
                        points = listOf(
                            Point(0.cm(), 0.cm()),
                            Point(800.cm(), 0.cm()),
                            Point(800.cm(), 600.cm()),
                            Point(0.cm(), 600.cm())
                        )
                    ),
                    position = Point(0.cm(), 0.cm()),
                    rotation = 0f.degree()
                ))
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
                    position = Point(300.cm(), 300.cm()),
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
                    position = Point(600.cm(), 600.cm()),
                    rotation = 0f.degree()
                )
            )
        )
    }
}
