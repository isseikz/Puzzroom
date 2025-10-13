package tokyo.isseikuzumaki.puzzroom.data.repository

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.puzzroom.data.PersistenceResult
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate

/**
 * 家具テンプレートの永続化を管理するRepository
 *
 * ドメイン層とデータソース層の橋渡しを行い、
 * ローカルストレージとクラウドストレージの切り替えを抽象化します。
 */
interface FurnitureTemplateRepository {
    /**
     * すべての家具テンプレートを取得
     */
    suspend fun getAllTemplates(): PersistenceResult<List<FurnitureTemplate>>

    /**
     * IDで家具テンプレートを取得
     */
    suspend fun getTemplateById(id: String): PersistenceResult<FurnitureTemplate?>

    /**
     * 家具テンプレートを保存（新規作成または更新）
     */
    suspend fun saveTemplate(template: FurnitureTemplate): PersistenceResult<Unit>

    /**
     * 家具テンプレートを削除
     */
    suspend fun deleteTemplate(id: String): PersistenceResult<Unit>

    /**
     * 家具テンプレートを更新
     */
    suspend fun updateTemplate(template: FurnitureTemplate): PersistenceResult<Unit>

    /**
     * すべての家具テンプレートの変更を監視
     */
    fun observeTemplates(): Flow<List<FurnitureTemplate>>

    /**
     * 特定の家具テンプレートの変更を監視
     */
    fun observeTemplate(id: String): Flow<FurnitureTemplate?>
}
