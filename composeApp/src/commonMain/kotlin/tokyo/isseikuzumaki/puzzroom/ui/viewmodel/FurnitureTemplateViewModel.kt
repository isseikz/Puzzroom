package tokyo.isseikuzumaki.puzzroom.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate
import tokyo.isseikuzumaki.puzzroom.domain.usecase.DeleteFurnitureTemplateUseCase
import tokyo.isseikuzumaki.puzzroom.domain.usecase.ListFurnitureTemplatesUseCase
import tokyo.isseikuzumaki.puzzroom.domain.usecase.SaveFurnitureTemplateUseCase

/**
 * 家具テンプレート管理のViewModel
 *
 * カスタム家具テンプレートの永続化を担当
 */
class FurnitureTemplateViewModel(
    private val saveFurnitureTemplateUseCase: SaveFurnitureTemplateUseCase,
    private val listFurnitureTemplatesUseCase: ListFurnitureTemplatesUseCase,
    private val deleteFurnitureTemplateUseCase: DeleteFurnitureTemplateUseCase,
) : ViewModel() {

    // カスタム家具テンプレート（プリセット + 永続化されたカスタム）
    private val _customTemplates = MutableStateFlow<List<FurnitureTemplate>>(emptyList())
    val customTemplates: StateFlow<List<FurnitureTemplate>> = _customTemplates.asStateFlow()

    // すべての家具テンプレート（プリセット + カスタム）
    private val _allTemplates = MutableStateFlow<List<FurnitureTemplate>>(FurnitureTemplate.PRESETS)
    val allTemplates: StateFlow<List<FurnitureTemplate>> = _allTemplates.asStateFlow()

    init {
        loadCustomTemplates()
    }

    /**
     * 永続化されたカスタム家具テンプレートを読み込み
     */
    private fun loadCustomTemplates() {
        viewModelScope.launch {
            listFurnitureTemplatesUseCase()
                .onSuccess { templates ->
                    _customTemplates.value = templates
                    _allTemplates.value = FurnitureTemplate.PRESETS + templates
                }
                .onFailure { error ->
                    // エラーが発生してもプリセットは使用可能
                    _customTemplates.value = emptyList()
                    _allTemplates.value = FurnitureTemplate.PRESETS
                }
        }
    }

    /**
     * カスタム家具テンプレートを保存
     */
    fun saveCustomTemplate(template: FurnitureTemplate) {
        viewModelScope.launch {
            saveFurnitureTemplateUseCase(template)
                .onSuccess {
                    // 成功したらリストを更新
                    _customTemplates.value = _customTemplates.value + template
                    _allTemplates.value = FurnitureTemplate.PRESETS + _customTemplates.value
                }
                .onFailure { error ->
                    // エラーハンドリング（必要に応じてUIに通知）
                    println("Failed to save custom furniture template: ${error.message}")
                }
        }
    }

    /**
     * カスタム家具テンプレートを削除
     */
    fun deleteCustomTemplate(id: String) {
        viewModelScope.launch {
            deleteFurnitureTemplateUseCase(id)
                .onSuccess {
                    // 成功したらリストから削除
                    _customTemplates.value = _customTemplates.value.filter { it.id != id }
                    _allTemplates.value = FurnitureTemplate.PRESETS + _customTemplates.value
                }
                .onFailure { error ->
                    // エラーハンドリング（必要に応じてUIに通知）
                    println("Failed to delete custom furniture template: ${error.message}")
                }
        }
    }

    /**
     * カスタム家具テンプレートをリロード
     */
    fun reloadCustomTemplates() {
        loadCustomTemplates()
    }
}
