package tokyo.isseikuzumaki.puzzroom.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.puzzroom.AppState
import tokyo.isseikuzumaki.puzzroom.domain.FloorPlan
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.PolygonGeometry
import tokyo.isseikuzumaki.puzzroom.domain.Project
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.ui.LoadDialog
import tokyo.isseikuzumaki.puzzroom.ui.PhotoPickerButton
import tokyo.isseikuzumaki.puzzroom.ui.SaveDialog
import tokyo.isseikuzumaki.puzzroom.ui.component.AngleInputPanel
import tokyo.isseikuzumaki.puzzroom.ui.component.DimensionInputPanel
import tokyo.isseikuzumaki.puzzroom.ui.component.EditMode
import tokyo.isseikuzumaki.puzzroom.ui.component.EditablePolygonCanvas
import tokyo.isseikuzumaki.puzzroom.ui.component.PolygonListPanel
import tokyo.isseikuzumaki.puzzroom.ui.component.SaveStateIndicator
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.ProjectViewModel

/**
 * 編集タイプ（寸法 or 角度）
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

    // ポリゴンリスト（Projectから取得）
    val polygons = remember(project) {
        project?.floorPlans?.firstOrNull()?.rooms?.map { it.shape } ?: emptyList()
    }

    // 編集状態
    var editMode by remember { mutableStateOf<EditMode>(EditMode.Creation) }
    var selectedPolygonIndex by remember { mutableStateOf<Int?>(null) }
    var editingDimensionOrAngle by remember { mutableStateOf<EditingType>(EditingType.Dimension) }

    // ダイアログ状態
    var savedJson by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header: 保存状態とアクション
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
                Text("保存")
            }

            Button(onClick = {
                showLoadDialog = true
            }) {
                Text("読み込み")
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
                    // 作成モード中の頂点追加（視覚的フィードバックのみ）
                },
                onCompletePolygon = { newPolygon ->
                    // ポリゴン完成 → Projectに追加
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
                    // 頂点をドラッグで移動
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
                        editMode = EditMode.Creation // 選択時は作成モードに戻す
                    },
                    onEdit = { index ->
                        selectedPolygonIndex = index
                        editMode = EditMode.Editing(index)
                    },
                    onDelete = { index ->
                        // ポリゴンを削除
                        project?.let { currentProject ->
                            val currentFloorPlan = currentProject.floorPlans.firstOrNull()
                            if (currentFloorPlan != null && index in currentFloorPlan.rooms.indices) {
                                val updatedRooms = currentFloorPlan.rooms.filterIndexed { i, _ -> i != index }
                                val updatedFloorPlan = currentFloorPlan.copy(rooms = updatedRooms)
                                val updatedProject = currentProject.copy(
                                    floorPlans = listOf(updatedFloorPlan)
                                )
                                viewModel.updateProject(updatedProject)

                                // 選択状態をクリア
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

                // Dimension/Angle input (編集モード時のみ表示)
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
                            Text("寸法")
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
                            Text("角度")
                        }
                    }

                    val selectedPolygon = polygons.getOrNull(selectedPolygonIndex!!)

                    when (editingDimensionOrAngle) {
                        EditingType.Dimension -> {
                            DimensionInputPanel(
                                polygon = selectedPolygon,
                                onDimensionChange = { edgeIndex, newLength ->
                                    // 寸法変更
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
                                    // 角度変更
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
                                    // 自動的に閉じる
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
