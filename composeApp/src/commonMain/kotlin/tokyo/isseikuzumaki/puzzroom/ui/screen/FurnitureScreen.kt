package tokyo.isseikuzumaki.puzzroom.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.AppState
import tokyo.isseikuzumaki.puzzroom.domain.*
import tokyo.isseikuzumaki.puzzroom.domain.Degree.Companion.degree
import tokyo.isseikuzumaki.puzzroom.ui.component.FurnitureLayoutCanvas
import tokyo.isseikuzumaki.puzzroom.ui.component.FurnitureLibraryPanel
import tokyo.isseikuzumaki.puzzroom.ui.component.FurniturePlacementToolbar
import tokyo.isseikuzumaki.puzzroom.ui.component.PlacedFurniture
import tokyo.isseikuzumaki.puzzroom.ui.component.SaveStateIndicator
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.ProjectViewModel

@Composable
fun FurnitureScreen(
    appState: AppState,
    viewModel: ProjectViewModel
) {
    val saveState by viewModel.saveState.collectAsState()
    val project by viewModel.currentProject.collectAsState()
    val currentFloorPlan = project?.floorPlans?.firstOrNull()
    val rooms = currentFloorPlan?.rooms ?: emptyList()
    var selectedRoom by remember { mutableStateOf<Room?>(appState.selectedRoom) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 保存状態インジケーター
        SaveStateIndicator(
            saveState = saveState,
            onRetry = { viewModel.saveNow() }
        )

        // 部屋選択
        if (selectedRoom == null) {
            Text(
                "部屋を選択してください",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            if (rooms.isEmpty()) {
                Text(
                    "部屋が登録されていません。Room 画面で部屋を作成してください。",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rooms) { room ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(),
                            onClick = {
                                selectedRoom = room
                                appState.selectRoom(room)
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    room.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "頂点数: ${room.shape.points.size}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // 家具配置
            var furnitureWidth by remember { mutableStateOf(Centimeter(100)) }
            var furnitureDepth by remember { mutableStateOf(Centimeter(100)) }
            var furnitureRotation by remember { mutableStateOf(0f) }
            var currentFurniture by remember { mutableStateOf<Furniture?>(null) }
            var placedFurnitures by remember { mutableStateOf<List<PlacedFurniture>>(emptyList()) }
            var currentPosition by remember { mutableStateOf<Point?>(null) }
            var selectedFurnitureIndex by remember { mutableStateOf<Int?>(null) }

            // Projectから配置済み家具を読み込む
            LaunchedEffect(selectedRoom, currentFloorPlan?.layouts) {
                selectedRoom?.let { room ->
                    placedFurnitures = currentFloorPlan?.layouts
                        ?.filter { it.room.id == room.id }  // 選択中の部屋の家具のみ
                        ?.map { layout ->
                            PlacedFurniture(
                                furniture = layout.furniture,
                                position = layout.position,
                                rotation = layout.rotation
                            )
                        } ?: emptyList()
                }
            }

            // テンプレート選択時に家具を作成
            LaunchedEffect(appState.selectedFurnitureTemplate) {
                appState.selectedFurnitureTemplate?.let { template ->
                    furnitureWidth = template.width
                    furnitureDepth = template.depth
                    furnitureRotation = 0f
                    selectedFurnitureIndex = null  // 選択をクリア
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

            // サイズ変更時に家具を再作成
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

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "部屋: ${selectedRoom!!.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = {
                        selectedRoom = null
                    }) {
                        Text("部屋選択に戻る")
                    }
                }

                Row(modifier = Modifier.weight(1f)) {
                    // 家具ライブラリパネル
                    FurnitureLibraryPanel(
                        templates = appState.furnitureTemplates,
                        selectedTemplate = appState.selectedFurnitureTemplate,
                        onTemplateSelected = { template ->
                            appState.selectFurnitureTemplate(template)
                        },
                        onCreateCustom = {
                            // TODO: カスタム家具作成ダイアログ
                        },
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                    )

                    // 家具配置キャンバス
                    FurnitureLayoutCanvas(
                        room = selectedRoom!!,
                        backgroundImageUrl = project?.layoutUrl,
                        furnitureToPlace = currentFurniture,
                        furnitureRotation = furnitureRotation,
                        placedFurnitures = placedFurnitures,
                        selectedFurnitureIndex = selectedFurnitureIndex,
                        onPositionUpdate = { position ->
                            currentPosition = position
                        },
                        onFurnitureSelected = { index ->
                            selectedFurnitureIndex = index
                            if (index != null) {
                                // 家具選択時は配置モードを解除
                                appState.selectFurnitureTemplate(null)
                            }
                        },
                        onFurnitureMoved = { index, newPosition ->
                            placedFurnitures = placedFurnitures.mapIndexed { i, furniture ->
                                if (i == index) {
                                    furniture.copy(position = newPosition)
                                } else {
                                    furniture
                                }
                            }
                            // ViewModelを更新
                            project?.let { currentProject ->
                                currentFloorPlan?.let { floorPlan ->
                                    val updatedLayoutEntry = floorPlan.layouts.getOrNull(index)?.copy(position = newPosition)
                                    if (updatedLayoutEntry != null) {
                                        val updatedFloorPlan = floorPlan.copy(
                                            layouts = floorPlan.layouts.mapIndexed { i, layout ->
                                                if (i == index) updatedLayoutEntry else layout
                                            }
                                        )
                                        val updatedProject = currentProject.copy(
                                            floorPlans = listOf(updatedFloorPlan)
                                        )
                                        // 自動保存トリガー
                                        viewModel.updateProject(updatedProject)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // ツールバー（家具選択時のみ表示）
                if (appState.selectedFurnitureTemplate != null && currentFurniture != null) {
                    FurniturePlacementToolbar(
                        furnitureName = appState.selectedFurnitureTemplate!!.name,
                        width = furnitureWidth,
                        depth = furnitureDepth,
                        rotation = furnitureRotation,
                        onWidthChange = { furnitureWidth = it },
                        onDepthChange = { furnitureDepth = it },
                        onRotationChange = { furnitureRotation = it },
                        onConfirm = {
                            currentPosition?.let { position ->
                                project?.let { currentProject ->
                                    currentFloorPlan?.let { floorPlan ->
                                        currentFurniture?.let { furniture ->
                                            selectedRoom?.let { room ->
                                                val layoutEntry = LayoutEntry(
                                                    room = room,
                                                    furniture = furniture,
                                                    position = position,
                                                    rotation = furnitureRotation.degree()
                                                )
                                                placedFurnitures = placedFurnitures + PlacedFurniture(
                                                    furniture = furniture,
                                                    position = position,
                                                    rotation = furnitureRotation.degree()
                                                )

                                                // Projectを更新
                                                val updatedFloorPlan = floorPlan.copy(
                                                    layouts = floorPlan.layouts + layoutEntry,
                                                    furnitures = floorPlan.furnitures + furniture
                                                )
                                                val updatedProject = currentProject.copy(
                                                    floorPlans = listOf(updatedFloorPlan)
                                                )

                                                appState.selectFurnitureTemplate(null)
                                                currentPosition = null

                                                // 自動保存トリガー
                                                viewModel.updateProject(updatedProject)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onCancel = {
                            appState.selectFurnitureTemplate(null)
                            currentPosition = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}