package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.AppState
import tokyo.isseikuzumaki.puzzroom.domain.*
import tokyo.isseikuzumaki.puzzroom.ui.molecules.SaveStateIndicator
import tokyo.isseikuzumaki.puzzroom.ui.state.PlacedFurniture
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.FurnitureTemplateViewModel
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FurniturePlacementPage(
    appState: AppState,
    viewModel: ProjectViewModel,
    furnitureTemplateViewModel: FurnitureTemplateViewModel
) {
    val saveState by viewModel.saveState.collectAsState()
    val project by viewModel.currentProject.collectAsState()
    val furnitureTemplates by furnitureTemplateViewModel.allTemplates.collectAsState()
    val currentFloorPlan = project?.floorPlans?.firstOrNull()
    val rooms = currentFloorPlan?.rooms ?: emptyList()
    var selectedRoom by remember { mutableStateOf<Room?>(appState.selectedRoom) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Save state indicator
        SaveStateIndicator(
            saveState = saveState,
            onRetry = { viewModel.saveNow() }
        )

        // Room selection
        if (selectedRoom == null) {
            Text(
                "Please select a room",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            if (rooms.isEmpty()) {
                Text(
                    "No rooms are registered. Please create a room on the Room screen.",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rooms) { room ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            onClick = {
                                selectedRoom = room
                                appState.selectRoom(room)
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    room.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Number of vertices: ${room.shape.points.size}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Furniture placement - using FurniturePlacementTemplate
            var furnitureWidth by remember { mutableStateOf(Centimeter(100)) }
            var furnitureDepth by remember { mutableStateOf(Centimeter(100)) }
            var furnitureRotation by remember { mutableStateOf(0f) }
            var currentFurniture by remember { mutableStateOf<Furniture?>(null) }
            var placedFurnitures by remember { mutableStateOf<List<PlacedFurniture>>(emptyList()) }
            var currentPosition by remember { mutableStateOf<Point?>(null) }
            var selectedFurnitureIndex by remember { mutableStateOf<Int?>(null) }

            // Load placed furniture from Project
            LaunchedEffect(selectedRoom, currentFloorPlan?.layouts) {
                selectedRoom?.let { room ->
                    placedFurnitures = currentFloorPlan?.layouts
                        ?.filter { it.room.id == room.id }
                        ?.map { layout ->
                            PlacedFurniture(
                                furniture = layout.furniture,
                                position = layout.position,
                                rotation = layout.rotation
                            )
                        } ?: emptyList()
                }
            }

            // Create furniture when template is selected
            LaunchedEffect(appState.selectedFurnitureTemplate) {
                appState.selectedFurnitureTemplate?.let { template ->
                    furnitureWidth = template.width
                    furnitureDepth = template.depth
                    furnitureRotation = 0f
                    selectedFurnitureIndex = null
                    currentFurniture = Furniture(
                        name = template.name,
                        shape = Polygon(
                            points = listOf(
                                Point(Centimeter(0), Centimeter(0)),
                                Point(furnitureWidth, Centimeter(0)),
                                Point(furnitureWidth, furnitureDepth),
                                Point(Centimeter(0), furnitureDepth)
                            )
                        )
                    )
                }
            }

            // Recreate furniture when size is changed
            LaunchedEffect(furnitureWidth, furnitureDepth) {
                if (appState.selectedFurnitureTemplate != null) {
                    currentFurniture = Furniture(
                        name = appState.selectedFurnitureTemplate!!.name,
                        shape = Polygon(
                            points = listOf(
                                Point(Centimeter(0), Centimeter(0)),
                                Point(furnitureWidth, Centimeter(0)),
                                Point(furnitureWidth, furnitureDepth),
                                Point(Centimeter(0), furnitureDepth)
                            )
                        )
                    )
                }
            }

            // Convert furniture templates to placeable items (Furniture objects)
            val placeableItems = remember(furnitureTemplates) {
                furnitureTemplates.map { template ->
                    Furniture(
                        name = template.name,
                        shape = Polygon(
                            points = listOf(
                                Point(Centimeter(0), Centimeter(0)),
                                Point(template.width, Centimeter(0)),
                                Point(template.width, template.depth),
                                Point(Centimeter(0), template.depth)
                            )
                        )
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Room header with back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Room: ${selectedRoom!!.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = {
                        selectedRoom = null
                    }) {
                        Text("Back to room selection")
                    }
                }

                // Use FurniturePlacementTemplate
                FurniturePlacementTemplate(
                    room = selectedRoom!!,
                    backgroundImageUrl = project?.layoutUrl,
                    furnitureToPlace = currentFurniture,
                    furnitureRotation = furnitureRotation,
                    placeableItems = placeableItems,
                    placedItems = placedFurnitures,
                    selectedFurnitureIndex = selectedFurnitureIndex,
                    onPositionUpdate = { position ->
                        currentPosition = position
                    },
                    onFurnitureSelected = { index ->
                        selectedFurnitureIndex = index
                        if (index != null) {
                            appState.selectFurnitureTemplate(null)
                        }
                    },
                    onFurnitureMoved = { index, newPosition ->
                        placedFurnitures = placedFurnitures.mapIndexed { i, furniture ->
                            if (i == index) {
                                furniture.copy(position = newPosition)
                            } else {
                                furniture
                            }
                        }
                        // Update ViewModel
                        project?.let { currentProject ->
                            currentFloorPlan?.let { floorPlan ->
                                val updatedLayoutEntry = floorPlan.layouts.getOrNull(index)?.copy(position = newPosition)
                                if (updatedLayoutEntry != null) {
                                    val updatedFloorPlan = floorPlan.copy(
                                        layouts = floorPlan.layouts.mapIndexed { i, layout ->
                                            if (i == index) updatedLayoutEntry else layout
                                        }
                                    )
                                    val updatedProject = currentProject.copy(
                                        floorPlans = listOf(updatedFloorPlan)
                                    )
                                    viewModel.updateProject(updatedProject)
                                }
                            }
                        }
                    },
                    onCancelBottomSheet = {
                        // Handle bottom sheet cancellation if needed
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

