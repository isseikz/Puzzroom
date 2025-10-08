package tokyo.isseikuzumaki.puzzroom.data.repository

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.puzzroom.data.PersistenceResult
import tokyo.isseikuzumaki.puzzroom.domain.Project

/**
 * プロジェクトの永続化を管理するRepository
 *
 * ドメイン層とデータソース層の橋渡しを行い、
 * ローカルストレージとクラウドストレージの切り替えを抽象化します。
 */
interface ProjectRepository {
    /**
     * すべてのプロジェクトを取得
     */
    suspend fun getAllProjects(): PersistenceResult<List<Project>>

    /**
     * IDでプロジェクトを取得
     */
    suspend fun getProjectById(id: String): PersistenceResult<Project?>

    /**
     * プロジェクトを保存（新規作成または更新）
     */
    suspend fun saveProject(project: Project): PersistenceResult<Unit>

    /**
     * プロジェクトを削除
     */
    suspend fun deleteProject(id: String): PersistenceResult<Unit>

    /**
     * プロジェクトを更新
     */
    suspend fun updateProject(project: Project): PersistenceResult<Unit>

    /**
     * すべてのプロジェクトの変更を監視
     */
    fun observeProjects(): Flow<List<Project>>

    /**
     * 特定のプロジェクトの変更を監視
     */
    fun observeProject(id: String): Flow<Project?>
}
