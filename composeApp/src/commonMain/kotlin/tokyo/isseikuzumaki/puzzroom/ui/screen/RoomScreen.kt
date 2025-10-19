package tokyo.isseikuzumaki.puzzroom.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.puzzroom.AppState
import tokyo.isseikuzumaki.puzzroom.domain.FloorPlan
import tokyo.isseikuzumaki.puzzroom.domain.PolygonGeometry
import tokyo.isseikuzumaki.puzzroom.domain.Project
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.ui.component.PhotoPickerButton
import tokyo.isseikuzumaki.puzzroom.ui.component.AngleInputPanel
import tokyo.isseikuzumaki.puzzroom.ui.component.DimensionInputPanel
import tokyo.isseikuzumaki.puzzroom.ui.component.EditMode
import tokyo.isseikuzumaki.puzzroom.ui.component.EditablePolygonCanvas
import tokyo.isseikuzumaki.puzzroom.ui.molecules.SaveStateIndicator
import tokyo.isseikuzumaki.puzzroom.ui.organisms.AngleEditSheet
import tokyo.isseikuzumaki.puzzroom.ui.organisms.EdgeDimensionSheet
import tokyo.isseikuzumaki.puzzroom.ui.organisms.LoadDialog
import tokyo.isseikuzumaki.puzzroom.ui.organisms.PolygonListPanel
import tokyo.isseikuzumaki.puzzroom.ui.organisms.SaveDialog
import tokyo.isseikuzumaki.puzzroom.ui.state.PolygonEditState
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
    
    // Edit state per polygon (track locked edges and angles)
    var polygonEditStates by remember { mutableStateOf<Map<Int, PolygonEditState>>(emptyMap()) }
    
    // ModalBottomSheet state
    var selectedEdgeIndex by remember { mutableStateOf<Int?>(null) }
    var selectedVertexIndex by remember { mutableStateOf<Int?>(null) }

    // Dialog state
    var savedJson by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    
    // Get current edit state for selected polygon
    val currentEditState = selectedPolygonIndex?.let { polygonEditStates[it] } ?: PolygonEditState()

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
                editState = currentEditState,
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
                        
                        // Initialize edit state for new polygon
                        val newIndex = polygons.size
                        polygonEditStates = polygonEditStates + (newIndex to PolygonEditState())
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
                onEdgeSelect = { polygonIndex, edgeIndex ->
                    // Edge selected in dimension editing mode
                    selectedEdgeIndex = edgeIndex
                },
                onVertexSelect = { polygonIndex, vertexIndex ->
                    // Vertex selected in angle editing mode
                    selectedVertexIndex = vertexIndex
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

                // Dimension/Angle editing mode switcher (only when polygon is selected)
                if (selectedPolygonIndex != null) {
                    // Mode selection buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "編集モード",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { 
                                    editMode = EditMode.Editing(selectedPolygonIndex!!)
                                    selectedEdgeIndex = null
                                    selectedVertexIndex = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = if (editMode is EditMode.Editing) {
                                    ButtonDefaults.buttonColors()
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Text("頂点移動")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { 
                                    editMode = EditMode.DimensionEditing(selectedPolygonIndex!!)
                                    selectedEdgeIndex = null
                                    selectedVertexIndex = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = if (editMode is EditMode.DimensionEditing) {
                                    ButtonDefaults.buttonColors()
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Text("寸法編集")
                            }
                            Button(
                                onClick = { 
                                    editMode = EditMode.AngleEditing(selectedPolygonIndex!!)
                                    selectedEdgeIndex = null
                                    selectedVertexIndex = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = if (editMode is EditMode.AngleEditing) {
                                    ButtonDefaults.buttonColors()
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Text("角度編集")
                            }
                        }
                    }

                    // Display constraint info
                    val selectedPolygon = polygons.getOrNull(selectedPolygonIndex!!)
                    if (selectedPolygon != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "拘束状態",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ロック済み辺:")
                                    Text("${currentEditState.lockedEdges.size} / ${selectedPolygon.points.size}")
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ロック済み角度:")
                                    Text("${currentEditState.lockedAngles.size} / ${selectedPolygon.points.size}")
                                }
                                
                                if (currentEditState.isFullyConstrained(selectedPolygon.points.size)) {
                                    Divider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "✅ 完全拘束",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ModalBottomSheets
    if (editMode is EditMode.DimensionEditing) {
        EdgeDimensionSheet(
            selectedEdgeIndex = selectedEdgeIndex,
            polygon = selectedPolygonIndex?.let { polygons.getOrNull(it) },
            editState = currentEditState,
            onDimensionChange = { edgeIndex, newLength, useSimilarity ->
                selectedPolygonIndex?.let { polyIndex ->
                    project?.let { currentProject ->
                        val currentFloorPlan = currentProject.floorPlans.firstOrNull()
                        if (currentFloorPlan != null && polyIndex in currentFloorPlan.rooms.indices) {
                            val room = currentFloorPlan.rooms[polyIndex]
                            
                            // Apply similarity transformation or individual adjustment
                            val updatedPolygon = if (useSimilarity) {
                                PolygonGeometry.applySimilarityTransformation(
                                    room.shape,
                                    edgeIndex,
                                    newLength
                                )
                            } else {
                                PolygonGeometry.adjustEdgeLength(
                                    room.shape,
                                    edgeIndex,
                                    newLength
                                )
                            }
                            
                            val updatedRoom = room.copy(shape = updatedPolygon)
                            val updatedRooms = currentFloorPlan.rooms.mapIndexed { index, r ->
                                if (index == polyIndex) updatedRoom else r
                            }
                            val updatedFloorPlan = currentFloorPlan.copy(rooms = updatedRooms)
                            val updatedProject = currentProject.copy(
                                floorPlans = listOf(updatedFloorPlan)
                            )
                            viewModel.updateProject(updatedProject)
                            
                            // Update edit state
                            if (useSimilarity) {
                                polygonEditStates = polygonEditStates + (polyIndex to 
                                    currentEditState.markSimilarityApplied().toggleEdgeLock(edgeIndex))
                            } else {
                                polygonEditStates = polygonEditStates + (polyIndex to 
                                    currentEditState.toggleEdgeLock(edgeIndex))
                            }
                        }
                    }
                }
            },
            onToggleLock = { edgeIndex ->
                selectedPolygonIndex?.let { polyIndex ->
                    polygonEditStates = polygonEditStates + (polyIndex to 
                        currentEditState.toggleEdgeLock(edgeIndex))
                }
            },
            onDismiss = {
                selectedEdgeIndex = null
            }
        )
    }
    
    if (editMode is EditMode.AngleEditing) {
        AngleEditSheet(
            selectedVertexIndex = selectedVertexIndex,
            polygon = selectedPolygonIndex?.let { polygons.getOrNull(it) },
            editState = currentEditState,
            onAngleChange = { vertexIndex, newAngleDegrees ->
                selectedPolygonIndex?.let { polyIndex ->
                    project?.let { currentProject ->
                        val currentFloorPlan = currentProject.floorPlans.firstOrNull()
                        if (currentFloorPlan != null && polyIndex in currentFloorPlan.rooms.indices) {
                            val room = currentFloorPlan.rooms[polyIndex]
                            val result = PolygonGeometry.adjustAngleLocal(
                                room.shape,
                                vertexIndex,
                                newAngleDegrees
                            )
                            result.getOrNull()?.let { updatedPolygon ->
                                val updatedRoom = room.copy(shape = updatedPolygon)
                                val updatedRooms = currentFloorPlan.rooms.mapIndexed { index, r ->
                                    if (index == polyIndex) updatedRoom else r
                                }
                                val updatedFloorPlan = currentFloorPlan.copy(rooms = updatedRooms)
                                val updatedProject = currentProject.copy(
                                    floorPlans = listOf(updatedFloorPlan)
                                )
                                viewModel.updateProject(updatedProject)
                            }
                        }
                    }
                }
            },
            onToggleLock = { vertexIndex ->
                selectedPolygonIndex?.let { polyIndex ->
                    polygonEditStates = polygonEditStates + (polyIndex to 
                        currentEditState.toggleAngleLock(vertexIndex))
                }
            },
            onLockTo90 = { vertexIndex ->
                selectedPolygonIndex?.let { polyIndex ->
                    project?.let { currentProject ->
                        val currentFloorPlan = currentProject.floorPlans.firstOrNull()
                        if (currentFloorPlan != null && polyIndex in currentFloorPlan.rooms.indices) {
                            val room = currentFloorPlan.rooms[polyIndex]
                            // Set angle to 90 degrees
                            val result = PolygonGeometry.adjustAngleLocal(
                                room.shape,
                                vertexIndex,
                                90.0
                            )
                            result.getOrNull()?.let { updatedPolygon ->
                                val updatedRoom = room.copy(shape = updatedPolygon)
                                val updatedRooms = currentFloorPlan.rooms.mapIndexed { index, r ->
                                    if (index == polyIndex) updatedRoom else r
                                }
                                val updatedFloorPlan = currentFloorPlan.copy(rooms = updatedRooms)
                                val updatedProject = currentProject.copy(
                                    floorPlans = listOf(updatedFloorPlan)
                                )
                                viewModel.updateProject(updatedProject)
                                
                                // Lock the angle
                                polygonEditStates = polygonEditStates + (polyIndex to 
                                    currentEditState.lockAngleTo90(vertexIndex))
                            }
                        }
                    }
                }
            },
            onAutoClose = {
                // Close polygon automatically
                selectedPolygonIndex?.let { polyIndex ->
                    project?.let { currentProject ->
                        val currentFloorPlan = currentProject.floorPlans.firstOrNull()
                        if (currentFloorPlan != null && polyIndex in currentFloorPlan.rooms.indices) {
                            val room = currentFloorPlan.rooms[polyIndex]
                            // Only auto-close if polygon has at least 3 vertices
                            if (room.shape.points.size >= 3) {
                                val closedPolygon = PolygonGeometry.autoClosePolygon(room.shape)
                                val updatedRoom = room.copy(shape = closedPolygon)
                                val updatedRooms = currentFloorPlan.rooms.mapIndexed { index, r ->
                                    if (index == polyIndex) updatedRoom else r
                                }
                                val updatedFloorPlan = currentFloorPlan.copy(rooms = updatedRooms)
                                val updatedProject = currentProject.copy(
                                    floorPlans = listOf(updatedFloorPlan)
                                )
                                viewModel.updateProject(updatedProject)
                            }
                        }
                    }
                }
            },
            onDismiss = {
                selectedVertexIndex = null
            }
        )
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
                    // Reset edit states on load
                    polygonEditStates = emptyMap()
                    showLoadDialog = false
                } catch (e: Exception) {
                    println("Failed to load project: $e")
                }
            },
            onDismiss = { showLoadDialog = false }
        )
    }
}
