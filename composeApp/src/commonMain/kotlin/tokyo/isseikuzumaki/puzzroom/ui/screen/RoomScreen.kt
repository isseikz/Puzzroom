package tokyo.isseikuzumaki.puzzroom.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.puzzroom.AppState
import tokyo.isseikuzumaki.puzzroom.domain.FloorPlan
import tokyo.isseikuzumaki.puzzroom.domain.PolygonGeometry
import tokyo.isseikuzumaki.puzzroom.domain.Project
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.ui.LoadDialog
import tokyo.isseikuzumaki.puzzroom.ui.component.PhotoPickerButton
import tokyo.isseikuzumaki.puzzroom.ui.SaveDialog
import tokyo.isseikuzumaki.puzzroom.ui.component.AngleInputPanel
import tokyo.isseikuzumaki.puzzroom.ui.component.DimensionInputPanel
import tokyo.isseikuzumaki.puzzroom.ui.component.EditMode
import tokyo.isseikuzumaki.puzzroom.ui.component.EditablePolygonCanvas
import tokyo.isseikuzumaki.puzzroom.ui.component.PolygonListPanel
import tokyo.isseikuzumaki.puzzroom.ui.component.SaveStateIndicator
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.ProjectViewModel

/**
 * Editing type (Dimension or Angle)
 */
private enum class EditingType {
    Dimension,
    Angle
}

@Composable
fun RoomScreen(
    appState: AppState,
    viewModel: ProjectViewModel
) {
    val saveState by viewModel.saveState.collectAsState()
    val project by viewModel.currentProject.collectAsState()

    // Polygon list (acquired from Project)
    val polygons = remember(project) {
        project?.floorPlans?.firstOrNull()?.rooms?.map { it.shape } ?: emptyList()
    }

    // Editing state
    var editMode by remember { mutableStateOf<EditMode>(EditMode.Creation) }
    var selectedPolygonIndex by remember { mutableStateOf<Int?>(null) }
    var editingDimensionOrAngle by remember { mutableStateOf<EditingType>(EditingType.Dimension) }

    // Dialog state
    var savedJson by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header: Save state and actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SaveStateIndicator(
                saveState = saveState,
                onRetry = { viewModel.saveNow() }
            )

            PhotoPickerButton { url ->
                project?.let {
                    val updatedProject = it.copy(layoutUrl = url)
                    viewModel.updateProject(updatedProject)
                }
            }

            Button(onClick = {
                project?.let {
                    savedJson = Json.encodeToString(Project.serializer(), it)
                    showSaveDialog = true
                }
            }) {
                Text("Save")
            }

            Button(onClick = {
                showLoadDialog = true
            }) {
                Text("Load")
            }
        }

        // Main content
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Left: Canvas
            EditablePolygonCanvas(
                polygons = polygons,
                selectedPolygonIndex = selectedPolygonIndex,
                backgroundImageUrl = project?.layoutUrl,
                editMode = editMode,
                onNewVertex = { offset ->
                    // Adding a vertex in creation mode (visual feedback only)
                },
                onCompletePolygon = { newPolygon ->
                    // Polygon complete -> Add to Project
                    project?.let {
                        val newRoom = Room(
                            name = "Room ${polygons.size + 1}",
                            shape = newPolygon
                        )
                        val currentFloorPlan = it.floorPlans.firstOrNull() ?: FloorPlan()
                        val updatedFloorPlan = currentFloorPlan.copy(
                            rooms = currentFloorPlan.rooms + newRoom
                        )
                        val updatedProject = it.copy(
                            floorPlans = listOf(updatedFloorPlan)
                        )
                        viewModel.updateProject(updatedProject)
                    }
                },
                onVertexMove = { polygonIndex, vertexIndex, newPosition ->
                    // Move vertex by dragging
                    project?.let { currentProject ->
                        val currentFloorPlan = currentProject.floorPlans.firstOrNull()
                        if (currentFloorPlan != null && polygonIndex in currentFloorPlan.rooms.indices) {
                            val room = currentFloorPlan.rooms[polygonIndex]
                            val updatedPolygon = PolygonGeometry.moveVertex(
                                room.shape,
                                vertexIndex,
                                newPosition
                            )
                            val updatedRoom = room.copy(shape = updatedPolygon)
                            val updatedRooms = currentFloorPlan.rooms.mapIndexed { index, r ->
                                if (index == polygonIndex) updatedRoom else r
                            }
                            val updatedFloorPlan = currentFloorPlan.copy(rooms = updatedRooms)
                            val updatedProject = currentProject.copy(
                                floorPlans = listOf(updatedFloorPlan)
                            )
                            viewModel.updateProject(updatedProject)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // Right sidebar
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Polygon list
                PolygonListPanel(
                    polygons = polygons,
                    selectedIndex = selectedPolygonIndex,
                    onSelect = { index ->
                        selectedPolygonIndex = index
                        editMode = EditMode.Creation // Return to creation mode when selected
                    },
                    onEdit = { index ->
                        selectedPolygonIndex = index
                        editMode = EditMode.Editing(index)
                    },
                    onDelete = { index ->
                        // Delete polygon
                        project?.let { currentProject ->
                            val currentFloorPlan = currentProject.floorPlans.firstOrNull()
                            if (currentFloorPlan != null && index in currentFloorPlan.rooms.indices) {
                                val updatedRooms = currentFloorPlan.rooms.filterIndexed { i, _ -> i != index }
                                val updatedFloorPlan = currentFloorPlan.copy(rooms = updatedRooms)
                                val updatedProject = currentProject.copy(
                                    floorPlans = listOf(updatedFloorPlan)
                                )
                                viewModel.updateProject(updatedProject)

                                // Clear selection state
                                if (selectedPolygonIndex == index) {
                                    selectedPolygonIndex = null
                                    editMode = EditMode.Creation
                                } else if (selectedPolygonIndex != null && selectedPolygonIndex!! > index) {
                                    selectedPolygonIndex = selectedPolygonIndex!! - 1
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Dimension/Angle input (only displayed in editing mode)
                if (editMode is EditMode.Editing && selectedPolygonIndex != null) {
                    // Toggle between Dimension and Angle editing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { editingDimensionOrAngle = EditingType.Dimension },
                            modifier = Modifier.weight(1f),
                            colors = if (editingDimensionOrAngle == EditingType.Dimension) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            }
                        ) {
                            Text("Dimension")
                        }
                        Button(
                            onClick = { editingDimensionOrAngle = EditingType.Angle },
                            modifier = Modifier.weight(1f),
                            colors = if (editingDimensionOrAngle == EditingType.Angle) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            }
                        ) {
                            Text("Angle")
                        }
                    }

                    val selectedPolygon = polygons.getOrNull(selectedPolygonIndex!!)

                    when (editingDimensionOrAngle) {
                        EditingType.Dimension -> {
                            DimensionInputPanel(
                                polygon = selectedPolygon,
                                onDimensionChange = { edgeIndex, newLength ->
                                    // Change dimension
                                    project?.let { currentProject ->
                                        val currentFloorPlan = currentProject.floorPlans.firstOrNull()
                                        if (currentFloorPlan != null && selectedPolygonIndex!! in currentFloorPlan.rooms.indices) {
                                            val room = currentFloorPlan.rooms[selectedPolygonIndex!!]
                                            val updatedPolygon = PolygonGeometry.adjustEdgeLength(
                                                room.shape,
                                                edgeIndex,
                                                newLength
                                            )
                                            val updatedRoom = room.copy(shape = updatedPolygon)
                                            val updatedRooms = currentFloorPlan.rooms.mapIndexed { index, r ->
                                                if (index == selectedPolygonIndex) updatedRoom else r
                                            }
                                            val updatedFloorPlan = currentFloorPlan.copy(rooms = updatedRooms)
                                            val updatedProject = currentProject.copy(
                                                floorPlans = listOf(updatedFloorPlan)
                                            )
                                            viewModel.updateProject(updatedProject)
                                        }
                                    }
                                },
                                onClose = {
                                    editMode = EditMode.Creation
                                    selectedPolygonIndex = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        EditingType.Angle -> {
                            AngleInputPanel(
                                polygon = selectedPolygon,
                                onAngleChange = { vertexIndex, newAngleDegrees ->
                                    // Change angle
                                    project?.let { currentProject ->
                                        val currentFloorPlan = currentProject.floorPlans.firstOrNull()
                                        if (currentFloorPlan != null && selectedPolygonIndex!! in currentFloorPlan.rooms.indices) {
                                            val room = currentFloorPlan.rooms[selectedPolygonIndex!!]
                                            val result = PolygonGeometry.adjustAngleLocal(
                                                room.shape,
                                                vertexIndex,
                                                newAngleDegrees
                                            )
                                            result.getOrNull()?.let { updatedPolygon ->
                                                val updatedRoom = room.copy(shape = updatedPolygon)
                                                val updatedRooms = currentFloorPlan.rooms.mapIndexed { index, r ->
                                                    if (index == selectedPolygonIndex) updatedRoom else r
                                                }
                                                val updatedFloorPlan = currentFloorPlan.copy(rooms = updatedRooms)
                                                val updatedProject = currentProject.copy(
                                                    floorPlans = listOf(updatedFloorPlan)
                                                )
                                                viewModel.updateProject(updatedProject)
                                            }
                                        }
                                    }
                                },
                                onAutoClose = {
                                    // Close automatically
                                    project?.let { currentProject ->
                                        val currentFloorPlan = currentProject.floorPlans.firstOrNull()
                                        if (currentFloorPlan != null && selectedPolygonIndex!! in currentFloorPlan.rooms.indices) {
                                            val room = currentFloorPlan.rooms[selectedPolygonIndex!!]
                                            val closedPolygon = PolygonGeometry.autoClosePolygon(room.shape)
                                            val updatedRoom = room.copy(shape = closedPolygon)
                                            val updatedRooms = currentFloorPlan.rooms.mapIndexed { index, r ->
                                                if (index == selectedPolygonIndex) updatedRoom else r
                                            }
                                            val updatedFloorPlan = currentFloorPlan.copy(rooms = updatedRooms)
                                            val updatedProject = currentProject.copy(
                                                floorPlans = listOf(updatedFloorPlan)
                                            )
                                            viewModel.updateProject(updatedProject)
                                        }
                                    }
                                },
                                onClose = {
                                    editMode = EditMode.Creation
                                    selectedPolygonIndex = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showSaveDialog && savedJson != null) {
        SaveDialog(
            json = savedJson!!,
            onDismiss = { showSaveDialog = false }
        )
    }

    if (showLoadDialog) {
        LoadDialog(
            onLoad = { json ->
                try {
                    val loadedProject = Json.decodeFromString<Project>(json)
                    viewModel.updateProject(loadedProject)
                    showLoadDialog = false
                } catch (e: Exception) {
                    println("Failed to load project: $e")
                }
            },
            onDismiss = { showLoadDialog = false }
        )
    }
}
