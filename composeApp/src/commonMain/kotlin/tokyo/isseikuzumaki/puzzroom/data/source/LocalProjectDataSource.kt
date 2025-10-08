package tokyo.isseikuzumaki.puzzroom.data.source

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.puzzroom.domain.Project

/**
 * ローカルストレージからプロジェクトデータを読み書きするデータソース
 */
interface LocalProjectDataSource {
    /**
     * すべてのプロジェクトを取得
     */
    suspend fun getAllProjects(): List<Project>

    /**
     * IDでプロジェクトを取得
     */
    suspend fun getProjectById(id: String): Project?

    /**
     * プロジェクトを挿入または更新
     */
    suspend fun insertProject(project: Project)

    /**
     * プロジェクトを更新
     */
    suspend fun updateProject(project: Project)

    /**
     * プロジェクトを削除
     */
    suspend fun deleteProject(id: String)

    /**
     * すべてのプロジェクトの変更を監視
     */
    fun observeProjects(): Flow<List<Project>>

    /**
     * 特定のプロジェクトの変更を監視
     */
    fun observeProject(id: String): Flow<Project?>
}
