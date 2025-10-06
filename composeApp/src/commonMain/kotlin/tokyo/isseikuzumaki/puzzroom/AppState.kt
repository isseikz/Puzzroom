package tokyo.isseikuzumaki.puzzroom

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import tokyo.isseikuzumaki.puzzroom.domain.*

/**
 * アプリケーション全体の状態を管理するクラス
 */
class AppState {
    var project by mutableStateOf(Project())
        private set

    var selectedRoom by mutableStateOf<Room?>(null)
        private set

    var furnitureTemplates by mutableStateOf<List<FurnitureTemplate>>(FurnitureTemplate.PRESETS)
        private set

    var selectedFurnitureTemplate by mutableStateOf<FurnitureTemplate?>(null)
        private set

    /**
     * プロジェクトを更新
     */
    fun updateProject(newProject: Project) {
        project = newProject
    }

    /**
     * Room を追加
     */
    fun addRoom(room: Room) {
        val currentFloorPlan = project.floorPlans.firstOrNull() ?: FloorPlan()
        val updatedFloorPlan = currentFloorPlan.copy(
            rooms = currentFloorPlan.rooms + room
        )
        project = project.copy(
            floorPlans = listOf(updatedFloorPlan)
        )
    }

    /**
     * Room を選択
     */
    fun selectRoom(room: Room) {
        selectedRoom = room
    }

    /**
     * LayoutEntry を追加
     */
    fun addLayoutEntry(layoutEntry: LayoutEntry) {
        val currentFloorPlan = project.floorPlans.firstOrNull() ?: FloorPlan()
        val updatedFloorPlan = currentFloorPlan.copy(
            layouts = currentFloorPlan.layouts + layoutEntry
        )
        project = project.copy(
            floorPlans = listOf(updatedFloorPlan)
        )
    }

    /**
     * Furniture を追加
     */
    fun addFurniture(furniture: Furniture) {
        val currentFloorPlan = project.floorPlans.firstOrNull() ?: FloorPlan()
        val updatedFloorPlan = currentFloorPlan.copy(
            furnitures = currentFloorPlan.furnitures + furniture
        )
        project = project.copy(
            floorPlans = listOf(updatedFloorPlan)
        )
    }

    /**
     * レイアウト URL を設定
     */
    fun setLayoutUrl(url: String?) {
        project = project.copy(layoutUrl = url)
    }

    /**
     * 現在の FloorPlan を取得
     */
    fun getCurrentFloorPlan(): FloorPlan? {
        return project.floorPlans.firstOrNull()
    }

    /**
     * 家具テンプレートを選択
     */
    fun selectFurnitureTemplate(template: FurnitureTemplate?) {
        selectedFurnitureTemplate = template
    }

    /**
     * カスタム家具テンプレートを追加
     */
    fun addCustomFurnitureTemplate(template: FurnitureTemplate) {
        furnitureTemplates = furnitureTemplates + template
    }
}
