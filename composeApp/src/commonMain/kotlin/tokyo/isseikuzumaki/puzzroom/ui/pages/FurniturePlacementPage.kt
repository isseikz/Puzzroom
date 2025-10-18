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
import tokyo.isseikuzumaki.puzzroom.domain.Degree.Companion.degree
import tokyo.isseikuzumaki.puzzroom.ui.molecules.SaveStateIndicator
import tokyo.isseikuzumaki.puzzroom.ui.organisms.FurnitureLayoutCanvas
import tokyo.isseikuzumaki.puzzroom.ui.organisms.FurnitureLibraryPanel
import tokyo.isseikuzumaki.puzzroom.ui.organisms.FurniturePlacementToolbar
import tokyo.isseikuzumaki.puzzroom.ui.state.PlacedFurniture
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.FurnitureTemplateViewModel
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.ProjectViewModel

@Composable
fun FurnitureLayoutPage(
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
            // Furniture placement
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
                        ?.filter { it.room.id == room.id }  // Only furniture in the selected room
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
                    selectedFurnitureIndex = null  // Clear selection
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

            Column(modifier = Modifier.fillMaxSize()) {
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

                Row(modifier = Modifier.weight(1f)) {
                    // Furniture library panel
                    FurnitureLibraryPanel(
                        templates = furnitureTemplates,
                        selectedTemplate = appState.selectedFurnitureTemplate,
                        onTemplateSelected = { template ->
                            appState.selectFurnitureTemplate(template)
                        },
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                    )

                    // Furniture placement canvas
                    FurnitureLayoutCanvas(
                        room = selectedRoom!!,
                        backgroundImageUrl = project?.layoutUrl,
                        furnitureToPlace = currentFurniture,
                        furnitureRotation = furnitureRotation,
                        placedFurnitures = placedFurnitures,
                        selectedFurnitureIndex = selectedFurnitureIndex,
                        onPositionUpdate = { position ->
                            currentPosition = position
                        },
                        onFurnitureSelected = { index ->
                            selectedFurnitureIndex = index
                            if (index != null) {
                                // Deselect placement mode when furniture is selected
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
                                        // Auto-save trigger
                                        viewModel.updateProject(updatedProject)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Toolbar (only displayed when furniture is selected)
                if (appState.selectedFurnitureTemplate != null && currentFurniture != null) {
                    FurniturePlacementToolbar(
                        furnitureName = appState.selectedFurnitureTemplate!!.name,
                        width = furnitureWidth,
                        depth = furnitureDepth,
                        rotation = furnitureRotation,
                        onWidthChange = { furnitureWidth = it },
                        onDepthChange = { furnitureDepth = it },
                        onRotationChange = { furnitureRotation = it },
                        onConfirm = {
                            currentPosition?.let { position ->
                                project?.let { currentProject ->
                                    currentFloorPlan?.let { floorPlan ->
                                        currentFurniture?.let { furniture ->
                                            selectedRoom?.let { room ->
                                                val layoutEntry = LayoutEntry(
                                                    room = room,
                                                    furniture = furniture,
                                                    position = position,
                                                    rotation = furnitureRotation.degree()
                                                )
                                                placedFurnitures = placedFurnitures + PlacedFurniture(
                                                    furniture = furniture,
                                                    position = position,
                                                    rotation = furnitureRotation.degree()
                                                )

                                                // Update Project
                                                val updatedFloorPlan = floorPlan.copy(
                                                    layouts = floorPlan.layouts + layoutEntry,
                                                    furnitures = floorPlan.furnitures + furniture
                                                )
                                                val updatedProject = currentProject.copy(
                                                    floorPlans = listOf(updatedFloorPlan)
                                                )

                                                appState.selectFurnitureTemplate(null)
                                                currentPosition = null

                                                // Auto-save trigger
                                                viewModel.updateProject(updatedProject)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onCancel = {
                            appState.selectFurnitureTemplate(null)
                            currentPosition = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}