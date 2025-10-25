package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.domain.FloorPlan
import tokyo.isseikuzumaki.puzzroom.ui.molecules.SaveStateIndicator
import tokyo.isseikuzumaki.puzzroom.ui.organisms.RoomNameDialog
import tokyo.isseikuzumaki.puzzroom.ui.templates.PlacedShape
import tokyo.isseikuzumaki.puzzroom.ui.templates.RoomCreationTemplate
import tokyo.isseikuzumaki.puzzroom.ui.templates.convertPlacedShapesToRoom
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme
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
    val saveState by viewModel.saveState.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var pendingShapes by remember { mutableStateOf<List<PlacedShape>?>(null) }

    Column(modifier = modifier) {
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
            placedShapes = emptyList(),
            onShapesChanged = { shapes ->
                pendingShapes = shapes
                showNameDialog = true
            },
            modifier = Modifier.weight(1f)
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun RoomCreationPagePreview() {
    PuzzroomTheme {
        RoomCreationPage()
    }
}
