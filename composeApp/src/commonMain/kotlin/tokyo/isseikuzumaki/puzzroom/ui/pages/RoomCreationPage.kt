package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import tokyo.isseikuzumaki.puzzroom.domain.FloorPlan
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.ui.molecules.SaveStateIndicator
import tokyo.isseikuzumaki.puzzroom.ui.organisms.RoomNameDialog
import tokyo.isseikuzumaki.puzzroom.ui.organisms.RoomSelectionList
import tokyo.isseikuzumaki.puzzroom.ui.templates.PlacedShape
import tokyo.isseikuzumaki.puzzroom.ui.templates.RoomCreationTemplate
import tokyo.isseikuzumaki.puzzroom.ui.templates.convertPlacedShapesToRoom
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.ProjectViewModel
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.rememberProjectViewModel

/**
 * Room creation page - allows users to create rooms by placing walls and doors
 *
 * Integrates with ProjectViewModel to save created rooms to the project
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomCreationPage(
    backgroundImageUrl: String? = null,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = rememberProjectViewModel()
) {
    val project by viewModel.currentProject.collectAsState()
    val currentFloorPlan = project?.floorPlans?.firstOrNull()
    val rooms = currentFloorPlan?.rooms ?: emptyList()
    var selectedRoom by remember { mutableStateOf<Room?>(null) }
    val saveState by viewModel.saveState.collectAsState()

    // Sync selectedRoom with the latest room data from the project
    LaunchedEffect(rooms, selectedRoom?.id) {
        selectedRoom?.let { currentSelection ->
            // Find the updated version of the selected room
            rooms.find { it.id == currentSelection.id }?.let { updatedRoom ->
                if (updatedRoom != currentSelection) {
                    selectedRoom = updatedRoom
                }
            }
        }
    }

    var showNameDialog by remember { mutableStateOf(false) }

    // Auto-show dialog when room list becomes empty (first-time user experience)
    LaunchedEffect(rooms.isEmpty()) {
        if (rooms.isEmpty()) {
            showNameDialog = true
        }
    }

    Column(modifier = modifier) {
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
            // Save state indicator
            SaveStateIndicator(
                saveState = saveState,
                onRetry = { viewModel.saveNow() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Load existing shapes from the selected room
            val existingShapes = selectedRoom?.shapes?.map { shapeData ->
                PlacedShape.fromData(shapeData)
            } ?: emptyList()

            RoomCreationTemplate(
                backgroundImageUrl = backgroundImageUrl,
                placedShapes = existingShapes,
                onShapesChanged = { shapes ->
                    // Update the room immediately when shapes change
                    selectedRoom?.let { currentRoom ->
                        project?.let { currentProject ->
                            val updatedRoom = convertPlacedShapesToRoom(shapes, currentRoom.name)
                                .copy(id = currentRoom.id) // Preserve room ID

                            val currentFloorPlan = currentProject.floorPlans.firstOrNull() ?: FloorPlan()
                            val updatedRooms = currentFloorPlan.rooms.map { room ->
                                if (room.id == currentRoom.id) updatedRoom else room
                            }

                            val updatedFloorPlan = currentFloorPlan.copy(rooms = updatedRooms)
                            val updatedProject = currentProject.copy(floorPlans = listOf(updatedFloorPlan))

                            viewModel.updateProject(updatedProject)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Room name dialog - only for creating NEW rooms
    if (showNameDialog) {
        RoomNameDialog(
            onConfirm = { roomName ->
                // Create a new empty room (shapes are auto-saved via onShapesChanged)
                val newRoom = convertPlacedShapesToRoom(emptyList(), roomName)
                project?.let { currentProject ->
                    val currentFloorPlan = currentProject.floorPlans.firstOrNull() ?: FloorPlan()

                    val updatedRooms = currentFloorPlan.rooms + newRoom
                    val updatedFloorPlan = currentFloorPlan.copy(rooms = updatedRooms)
                    val updatedProject = currentProject.copy(floorPlans = listOf(updatedFloorPlan))

                    viewModel.updateProject(updatedProject)

                    // Select the newly created room
                    selectedRoom = newRoom
                }
                showNameDialog = false
            },
            onDismiss = {
                showNameDialog = false
            }
        )
    }
}
