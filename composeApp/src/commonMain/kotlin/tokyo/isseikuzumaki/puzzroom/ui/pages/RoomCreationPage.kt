package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import tokyo.isseikuzumaki.puzzroom.ui.organisms.ButtonToCreate
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

    var showNameDialog by remember { mutableStateOf(false) }
    var pendingShapes by remember { mutableStateOf<List<PlacedShape>?>(null) }
    var createRoomCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    Column(modifier = modifier) {
        // Show room creation interface when:
        // 1. Room list is empty (first-time users)
        // 2. A room is selected for editing
        if (rooms.isEmpty() || selectedRoom != null) {
            // Save state indicator
            SaveStateIndicator(
                saveState = saveState,
                onRetry = { viewModel.saveNow() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            RoomCreationTemplate(
                backgroundImageUrl = backgroundImageUrl,
                placedShapes = emptyList(), //FIXME: Load existing shapes if editing
                onShapesChanged = { shapes ->
                    pendingShapes = shapes
                    showNameDialog = true
                },
                onCreateRoomClick = { callback ->
                    createRoomCallback = callback
                },
                modifier = Modifier.weight(1f)
            )

            // ButtonToCreate to finalize and save the room
            ButtonToCreate(
                onClick = {
                    createRoomCallback?.invoke()
                },
                enabled = createRoomCallback != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(color = MaterialTheme.colorScheme.secondaryContainer)
            )
        } else {
            // Show room selection list
            Text(
                "Please select a room",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            RoomSelectionList(rooms) {
                selectedRoom = it
            }
        }
    }

    // Room name dialog
    if (showNameDialog && pendingShapes != null) {
        RoomNameDialog(
            onConfirm = { roomName ->
                pendingShapes?.let { shapes ->
                    val newRoom = convertPlacedShapesToRoom(shapes, roomName)
                    project?.let { currentProject ->
                        val currentFloorPlan = currentProject.floorPlans.firstOrNull() ?: FloorPlan()
                        val updatedFloorPlan = currentFloorPlan.copy(
                            rooms = currentFloorPlan.rooms + newRoom
                        )
                        val updatedProject = currentProject.copy(
                            floorPlans = listOf(updatedFloorPlan)
                        )
                        viewModel.updateProject(updatedProject)
                    }
                }
                showNameDialog = false
                pendingShapes = null
            },
            onDismiss = {
                showNameDialog = false
                pendingShapes = null
            }
        )
    }
}
