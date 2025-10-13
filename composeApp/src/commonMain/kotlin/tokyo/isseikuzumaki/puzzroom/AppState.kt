package tokyo.isseikuzumaki.puzzroom

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import tokyo.isseikuzumaki.puzzroom.domain.*

/**
 * アプリケーション全体の一時的UI状態を管理するクラス
 *
 * このクラスは永続化されないUI状態のみを管理します。
 * 永続化が必要なデータ（Project, Room, Furnitureなど）は
 * ProjectViewModelで管理されます。
 * カスタム家具テンプレートは FurnitureTemplateViewModel で管理されます。
 *
 * @see tokyo.isseikuzumaki.puzzroom.ui.viewmodel.ProjectViewModel
 * @see tokyo.isseikuzumaki.puzzroom.ui.viewmodel.FurnitureTemplateViewModel
 */
class AppState {
    /**
     * 選択中の部屋（一時的な選択状態）
     */
    var selectedRoom by mutableStateOf<Room?>(null)
        private set

    /**
     * 選択中の家具テンプレート（一時的な選択状態）
     */
    var selectedFurnitureTemplate by mutableStateOf<FurnitureTemplate?>(null)
        private set

    /**
     * Room を選択
     */
    fun selectRoom(room: Room?) {
        selectedRoom = room
    }

    /**
     * 家具テンプレートを選択
     */
    fun selectFurnitureTemplate(template: FurnitureTemplate?) {
        selectedFurnitureTemplate = template
    }
}
