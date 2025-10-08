package tokyo.isseikuzumaki.puzzroom.data.repository

import kotlinx.coroutines.flow.*
import tokyo.isseikuzumaki.puzzroom.data.PersistenceError
import tokyo.isseikuzumaki.puzzroom.data.PersistenceResult
import tokyo.isseikuzumaki.puzzroom.data.source.LocalProjectDataSource
import tokyo.isseikuzumaki.puzzroom.domain.Project

/**
 * ProjectRepositoryの実装（Phase 1: ローカルのみ）
 *
 * Phase 1では、ローカルストレージのみを使用します。
 * 将来的にはRemoteProjectDataSourceを追加してクラウド同期を実装します。
 */
class ProjectRepositoryImpl(
    private val localDataSource: LocalProjectDataSource,
    // Phase 3で追加: private val remoteDataSource: RemoteProjectDataSource? = null
) : ProjectRepository {

    // In-memory cache for quick access
    private val cache = MutableStateFlow<Map<String, Project>>(emptyMap())

    override suspend fun getAllProjects(): PersistenceResult<List<Project>> {
        return runCatching {
            val projects = localDataSource.getAllProjects()
            cache.value = projects.associateBy { it.id }
            projects
        }.recoverCatching { error ->
            when (error) {
                is PersistenceError -> throw error
                else -> throw PersistenceError.IOError(error)
            }
        }
    }

    override suspend fun getProjectById(id: String): PersistenceResult<Project?> {
        return runCatching {
            // まずキャッシュをチェック
            cache.value[id]?.let { return@runCatching it }

            // キャッシュになければローカルストレージから読み込み
            val project = localDataSource.getProjectById(id)
            if (project != null) {
                cache.value = cache.value + (id to project)
            }
            project
        }.recoverCatching { error ->
            when (error) {
                is PersistenceError -> throw error
                else -> throw PersistenceError.IOError(error)
            }
        }
    }

    override suspend fun saveProject(project: Project): PersistenceResult<Unit> {
        return runCatching {
            localDataSource.insertProject(project)
            cache.value = cache.value + (project.id to project)
        }.recoverCatching { error ->
            when (error) {
                is PersistenceError -> throw error
                else -> throw PersistenceError.IOError(error)
            }
        }
    }

    override suspend fun deleteProject(id: String): PersistenceResult<Unit> {
        return runCatching {
            localDataSource.deleteProject(id)
            cache.value = cache.value - id
        }.recoverCatching { error ->
            when (error) {
                is PersistenceError -> throw error
                else -> throw PersistenceError.IOError(error)
            }
        }
    }

    override suspend fun updateProject(project: Project): PersistenceResult<Unit> {
        return runCatching {
            localDataSource.updateProject(project)
            cache.value = cache.value + (project.id to project)
        }.recoverCatching { error ->
            when (error) {
                is PersistenceError -> throw error
                else -> throw PersistenceError.IOError(error)
            }
        }
    }

    override fun observeProjects(): Flow<List<Project>> {
        return localDataSource.observeProjects()
            .onEach { projects ->
                cache.value = projects.associateBy { it.id }
            }
    }

    override fun observeProject(id: String): Flow<Project?> {
        return localDataSource.observeProject(id)
            .onEach { project ->
                if (project != null) {
                    cache.value = cache.value + (id to project)
                }
            }
    }
}
