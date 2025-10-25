package tokyo.isseikuzumaki.puzzroom.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.ui.templates.PlacedShape

/**
 * Shape placement ViewModel
 * 図形配置の状態管理とイベント処理を担当
 * 
 * RoomCreationPage と FurniturePlacementPage で共有される
 */
class ShapePlacementViewModel : ViewModel() {
    
    // 配置済みの図形リスト
    private val _placedShapes = MutableStateFlow<List<PlacedShape>>(emptyList())
    val placedShapes: StateFlow<List<PlacedShape>> = _placedShapes.asStateFlow()
    
    // 選択中の図形のインデックス
    private val _selectedShapeIndex = MutableStateFlow<Int?>(null)
    val selectedShapeIndex: StateFlow<Int?> = _selectedShapeIndex.asStateFlow()
    
    // 現在のカーソル位置（プレビュー用）
    private val _currentPosition = MutableStateFlow<Point?>(null)
    val currentPosition: StateFlow<Point?> = _currentPosition.asStateFlow()
    
    /**
     * 図形を追加
     */
    fun addShape(shape: PlacedShape) {
        _placedShapes.value = _placedShapes.value + shape
        // 追加した図形を自動選択
        _selectedShapeIndex.value = _placedShapes.value.size - 1
    }
    
    /**
     * 図形を選択
     */
    fun selectShape(index: Int?) {
        _selectedShapeIndex.value = index
    }
    
    /**
     * 図形の位置を更新
     */
    fun moveShape(index: Int, newPosition: Point) {
        if (index >= 0 && index < _placedShapes.value.size) {
            _placedShapes.value = _placedShapes.value.mapIndexed { i, shape ->
                if (i == index) shape.copy(position = newPosition) else shape
            }
        }
    }
    
    /**
     * 位置更新（プレビュー用）
     */
    fun updatePosition(position: Point?) {
        _currentPosition.value = position
    }
    
    /**
     * 全ての図形をクリア
     */
    fun clearShapes() {
        _placedShapes.value = emptyList()
        _selectedShapeIndex.value = null
    }
    
    /**
     * 図形リストを設定（初期化時など）
     */
    fun setShapes(shapes: List<PlacedShape>) {
        _placedShapes.value = shapes
    }
}
