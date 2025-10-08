package tokyo.isseikuzumaki.puzzroom.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.puzzroom.AppState
import tokyo.isseikuzumaki.puzzroom.domain.FloorPlan
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.Project
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.ui.InputPolygon
import tokyo.isseikuzumaki.puzzroom.ui.LoadDialog
import tokyo.isseikuzumaki.puzzroom.ui.PhotoPickerButton
import tokyo.isseikuzumaki.puzzroom.ui.SaveDialog
import tokyo.isseikuzumaki.puzzroom.ui.component.SaveStateIndicator
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.ProjectViewModel

@Composable
fun RoomScreen(
    appState: AppState,
    viewModel: ProjectViewModel
) {
    val saveState by viewModel.saveState.collectAsState()
    val project by viewModel.currentProject.collectAsState()
    var polygons by remember { mutableStateOf(emptyList<Polygon>()) }
    var savedJson by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    Column {
        Row {
            // 保存状態インジケーター
            SaveStateIndicator(
                saveState = saveState,
                onRetry = { viewModel.saveNow() }
            )

            PhotoPickerButton { url ->
                // プロジェクトを更新して自動保存トリガー
                project?.let {
                    val updatedProject = it.copy(layoutUrl = url)
                    viewModel.updateProject(updatedProject)
                }
            }
            Button(onClick = {
                project?.let {
                    // polygons を rooms に変換
                    val newRooms = polygons.mapIndexed { index, polygon ->
                        Room(name = "Room ${index + 1}", shape = polygon)
                    }

                    // Projectを更新
                    val currentFloorPlan = it.floorPlans.firstOrNull() ?: FloorPlan()
                    val updatedFloorPlan = currentFloorPlan.copy(
                        rooms = currentFloorPlan.rooms + newRooms
                    )
                    val updatedProject = it.copy(
                        floorPlans = listOf(updatedFloorPlan)
                    )

                    // プロジェクトを更新して自動保存トリガー
                    viewModel.updateProject(updatedProject)
                    savedJson = Json.encodeToString(updatedProject)
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
        InputPolygon(
            polygons = polygons,
            backgroundImageUrl = project?.layoutUrl) { newPolygon ->
            println("New polygon: $newPolygon")
            polygons = polygons + newPolygon

            // 即座に永続化
            project?.let {
                val newRoom = Room(name = "Room ${polygons.size}", shape = newPolygon)
                val currentFloorPlan = it.floorPlans.firstOrNull() ?: FloorPlan()
                val updatedFloorPlan = currentFloorPlan.copy(
                    rooms = currentFloorPlan.rooms + newRoom
                )
                val updatedProject = it.copy(
                    floorPlans = listOf(updatedFloorPlan)
                )
                viewModel.updateProject(updatedProject)
            }
        }

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
                        // rooms から polygons に変換して表示用に保持
                        polygons = loadedProject.floorPlans.flatMap { floorPlan ->
                            floorPlan.rooms.map { it.shape }
                        }
                        showLoadDialog = false
                    } catch (e: Exception) {
                        println("Failed to load project: $e")
                    }
                },
                onDismiss = { showLoadDialog = false }
            )
        }
    }
}