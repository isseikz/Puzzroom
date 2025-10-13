package tokyo.isseikuzumaki.puzzroom.data.source

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate

/**
 * ローカルストレージから家具テンプレートデータを読み書きするデータソース
 */
interface LocalFurnitureTemplateDataSource {
    /**
     * すべての家具テンプレートを取得
     */
    suspend fun getAllTemplates(): List<FurnitureTemplate>

    /**
     * IDで家具テンプレートを取得
     */
    suspend fun getTemplateById(id: String): FurnitureTemplate?

    /**
     * 家具テンプレートを挿入または更新
     */
    suspend fun insertTemplate(template: FurnitureTemplate)

    /**
     * 家具テンプレートを更新
     */
    suspend fun updateTemplate(template: FurnitureTemplate)

    /**
     * 家具テンプレートを削除
     */
    suspend fun deleteTemplate(id: String)

    /**
     * すべての家具テンプレートの変更を監視
     */
    fun observeTemplates(): Flow<List<FurnitureTemplate>>

    /**
     * 特定の家具テンプレートの変更を監視
     */
    fun observeTemplate(id: String): Flow<FurnitureTemplate?>
}
