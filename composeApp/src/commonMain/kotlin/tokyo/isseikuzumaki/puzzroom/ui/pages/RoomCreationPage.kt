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

    var showNameDialog by remember { mutableStateOf(false) }
    var pendingShapes by remember { mutableStateOf<List<PlacedShape>?>(null) }
    
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

            RoomCreationTemplate(
                backgroundImageUrl = backgroundImageUrl,
                placedShapes = emptyList(), //FIXME: Load existing shapes if editing
                onShapesChanged = { shapes ->
                    pendingShapes = shapes
                    showNameDialog = true
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Room name dialog
    if (showNameDialog) {
        RoomNameDialog(
            onConfirm = { roomName ->
                // Create room from shapes (if available) or empty list (for first-time users)
                // pendingShapes is null when dialog is auto-shown for empty room list,
                // and contains shapes when triggered from RoomCreationTemplate
                val newRoom = convertPlacedShapesToRoom(pendingShapes ?: emptyList(), roomName)
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
