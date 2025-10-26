package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.AppState
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.FloorPlan
import tokyo.isseikuzumaki.puzzroom.domain.Furniture
import tokyo.isseikuzumaki.puzzroom.domain.LayoutEntry
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.ui.molecules.SaveStateIndicator
import tokyo.isseikuzumaki.puzzroom.ui.organisms.RoomSelectionList
import tokyo.isseikuzumaki.puzzroom.ui.state.PlacedFurniture
import tokyo.isseikuzumaki.puzzroom.ui.templates.FurniturePlacementTemplate
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
    var selectedRoom by remember { mutableStateOf<Room?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
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
                RoomSelectionList(rooms) {
                    selectedRoom = it
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

            // Save state indicator
            SaveStateIndicator(
                saveState = saveState,
                onRetry = { viewModel.saveNow() }
            )

            // Use FurniturePlacementTemplate
            FurniturePlacementTemplate(
                room = selectedRoom!!,
                backgroundImageUrl = project?.layoutUrl,
                placeableItems = placeableItems,
                placedItems = placedFurnitures,
                onFurnitureChanged = { updatedFurniture ->
                    // Update local state
                    placedFurnitures = updatedFurniture

                    // Save to project
                    project?.let { currentProject ->
                        selectedRoom?.let { room ->
                            val currentFloorPlan = currentProject.floorPlans.firstOrNull() ?: FloorPlan()

                            // Remove old layouts for this room and add new ones
                            val otherLayouts = currentFloorPlan.layouts.filter { it.room.id != room.id }
                            val newLayouts = updatedFurniture.map { placedFurniture ->
                                LayoutEntry(
                                    room = room,
                                    furniture = placedFurniture.furniture,
                                    position = placedFurniture.position,
                                    rotation = placedFurniture.rotation
                                )
                            }

                            val updatedFloorPlan = currentFloorPlan.copy(
                                layouts = otherLayouts + newLayouts
                            )
                            val updatedProject = currentProject.copy(
                                floorPlans = listOf(updatedFloorPlan)
                            )
                            viewModel.updateProject(updatedProject)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

