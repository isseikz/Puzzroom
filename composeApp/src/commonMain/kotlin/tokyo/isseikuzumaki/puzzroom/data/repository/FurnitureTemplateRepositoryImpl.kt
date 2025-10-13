package tokyo.isseikuzumaki.puzzroom.data.repository

import kotlinx.coroutines.flow.*
import tokyo.isseikuzumaki.puzzroom.data.PersistenceError
import tokyo.isseikuzumaki.puzzroom.data.PersistenceResult
import tokyo.isseikuzumaki.puzzroom.data.source.LocalFurnitureTemplateDataSource
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate

/**
 * FurnitureTemplateRepositoryの実装（Phase 1: ローカルのみ）
 *
 * Phase 1では、ローカルストレージのみを使用します。
 * 将来的にはRemoteFurnitureTemplateDataSourceを追加してクラウド同期を実装します。
 */
class FurnitureTemplateRepositoryImpl(
    private val localDataSource: LocalFurnitureTemplateDataSource,
    // Phase 3で追加: private val remoteDataSource: RemoteFurnitureTemplateDataSource? = null
) : FurnitureTemplateRepository {

    // In-memory cache for quick access
    private val cache = MutableStateFlow<Map<String, FurnitureTemplate>>(emptyMap())

    override suspend fun getAllTemplates(): PersistenceResult<List<FurnitureTemplate>> {
        return runCatching {
            val templates = localDataSource.getAllTemplates()
            cache.value = templates.associateBy { it.id }
            templates
        }.recoverCatching { error ->
            when (error) {
                is PersistenceError -> throw error
                else -> throw PersistenceError.IOError(error)
            }
        }
    }

    override suspend fun getTemplateById(id: String): PersistenceResult<FurnitureTemplate?> {
        return runCatching {
            // まずキャッシュをチェック
            cache.value[id]?.let { return@runCatching it }

            // キャッシュになければローカルストレージから読み込み
            val template = localDataSource.getTemplateById(id)
            if (template != null) {
                cache.value = cache.value + (id to template)
            }
            template
        }.recoverCatching { error ->
            when (error) {
                is PersistenceError -> throw error
                else -> throw PersistenceError.IOError(error)
            }
        }
    }

    override suspend fun saveTemplate(template: FurnitureTemplate): PersistenceResult<Unit> {
        return runCatching {
            localDataSource.insertTemplate(template)
            cache.value = cache.value + (template.id to template)
        }.recoverCatching { error ->
            when (error) {
                is PersistenceError -> throw error
                else -> throw PersistenceError.IOError(error)
            }
        }
    }

    override suspend fun deleteTemplate(id: String): PersistenceResult<Unit> {
        return runCatching {
            localDataSource.deleteTemplate(id)
            cache.value = cache.value - id
        }.recoverCatching { error ->
            when (error) {
                is PersistenceError -> throw error
                else -> throw PersistenceError.IOError(error)
            }
        }
    }

    override suspend fun updateTemplate(template: FurnitureTemplate): PersistenceResult<Unit> {
        return runCatching {
            localDataSource.updateTemplate(template)
            cache.value = cache.value + (template.id to template)
        }.recoverCatching { error ->
            when (error) {
                is PersistenceError -> throw error
                else -> throw PersistenceError.IOError(error)
            }
        }
    }

    override fun observeTemplates(): Flow<List<FurnitureTemplate>> {
        return localDataSource.observeTemplates()
            .onEach { templates ->
                cache.value = templates.associateBy { it.id }
            }
    }

    override fun observeTemplate(id: String): Flow<FurnitureTemplate?> {
        return localDataSource.observeTemplate(id)
            .onEach { template ->
                if (template != null) {
                    cache.value = cache.value + (id to template)
                }
            }
    }
}
