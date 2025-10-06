package tokyo.isseikuzumaki.puzzroom.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.puzzroom.AppState
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.Project
import tokyo.isseikuzumaki.puzzroom.domain.Room
import tokyo.isseikuzumaki.puzzroom.ui.InputPolygon
import tokyo.isseikuzumaki.puzzroom.ui.LoadDialog
import tokyo.isseikuzumaki.puzzroom.ui.PhotoPickerButton
import tokyo.isseikuzumaki.puzzroom.ui.SaveDialog

@Composable
fun RoomScreen(
    appState: AppState
) {
    var polygons by remember { mutableStateOf(emptyList<Polygon>()) }
    var savedJson by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    Column {
        Row {
            PhotoPickerButton { url ->
                appState.setLayoutUrl(url)
            }
            Button(onClick = {
                // polygons を rooms に変換して AppState に追加
                polygons.forEachIndexed { index, polygon ->
                    appState.addRoom(Room(
                        name = "Room ${index + 1}",
                        shape = polygon
                    ))
                }
                savedJson = Json.encodeToString(appState.project)
                showSaveDialog = true
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
            backgroundImageUrl = appState.project.layoutUrl) {
            println("New polygon: $it")
            polygons = polygons + it
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
                        val project = Json.decodeFromString<Project>(json)
                        appState.updateProject(project)
                        // rooms から polygons に変換して表示用に保持
                        polygons = project.floorPlans.flatMap { floorPlan ->
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