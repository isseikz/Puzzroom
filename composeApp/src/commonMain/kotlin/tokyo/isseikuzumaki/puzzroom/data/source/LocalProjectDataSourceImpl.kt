package tokyo.isseikuzumaki.puzzroom.data.source

import kotlinx.coroutines.flow.*
import tokyo.isseikuzumaki.puzzroom.data.PersistenceError
import tokyo.isseikuzumaki.puzzroom.data.storage.IFileStorage
import tokyo.isseikuzumaki.puzzroom.domain.Project

/**
 * FileStorageベースのLocalProjectDataSource実装
 */
class LocalProjectDataSourceImpl(
    private val fileStorage: IFileStorage
) : LocalProjectDataSource {

    // プロジェクト変更通知用のFlow
    private val _projectsFlow = MutableStateFlow<List<Project>>(emptyList())

    override suspend fun getAllProjects(): List<Project> {
        return try {
            val fileNames = fileStorage.listProjects()
            val projects = fileNames.mapNotNull { fileName ->
                try {
                    fileStorage.readProject(fileName)
                } catch (e: Exception) {
                    // ファイルの読み込みに失敗した場合はスキップ（破損ファイル対策）
                    null
                }
            }
            _projectsFlow.value = projects
            projects
        } catch (e: Exception) {
            throw PersistenceError.IOError(e)
        }
    }

    override suspend fun getProjectById(id: String): Project? {
        return try {
            fileStorage.readProject(id)
        } catch (e: Exception) {
            throw PersistenceError.IOError(e)
        }
    }

    override suspend fun insertProject(project: Project) {
        try {
            fileStorage.writeProject(project, project.id)
            // プロジェクトリストを更新
            _projectsFlow.value = getAllProjects()
        } catch (e: Exception) {
            throw PersistenceError.IOError(e)
        }
    }

    override suspend fun updateProject(project: Project) {
        try {
            // 存在確認
            val existing = fileStorage.readProject(project.id)
                ?: throw PersistenceError.NotFoundError(project.id)

            fileStorage.writeProject(project, project.id)
            // プロジェクトリストを更新
            _projectsFlow.value = getAllProjects()
        } catch (e: PersistenceError) {
            throw e
        } catch (e: Exception) {
            throw PersistenceError.IOError(e)
        }
    }

    override suspend fun deleteProject(id: String) {
        try {
            fileStorage.deleteProject(id)
            // プロジェクトリストを更新
            _projectsFlow.value = getAllProjects()
        } catch (e: Exception) {
            throw PersistenceError.IOError(e)
        }
    }

    override fun observeProjects(): Flow<List<Project>> {
        return _projectsFlow.asStateFlow()
    }

    override fun observeProject(id: String): Flow<Project?> {
        return _projectsFlow.map { projects ->
            projects.find { it.id == id }
        }
    }
}
