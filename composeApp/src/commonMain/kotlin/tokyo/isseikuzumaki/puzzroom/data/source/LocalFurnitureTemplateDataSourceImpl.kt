package tokyo.isseikuzumaki.puzzroom.data.source

import kotlinx.coroutines.flow.*
import tokyo.isseikuzumaki.puzzroom.data.PersistenceError
import tokyo.isseikuzumaki.puzzroom.data.storage.IFileStorage
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate

/**
 * FileStorageベースのLocalFurnitureTemplateDataSource実装
 */
class LocalFurnitureTemplateDataSourceImpl(
    private val fileStorage: IFileStorage
) : LocalFurnitureTemplateDataSource {

    // 家具テンプレート変更通知用のFlow
    private val _templatesFlow = MutableStateFlow<List<FurnitureTemplate>>(emptyList())

    override suspend fun getAllTemplates(): List<FurnitureTemplate> {
        return runCatching {
            val fileNames = fileStorage.listFurnitureTemplates()
            val templates = fileNames.mapNotNull { fileName ->
                runCatching {
                    fileStorage.readFurnitureTemplate(fileName)
                }.getOrNull() // ファイルの読み込みに失敗した場合はスキップ（破損ファイル対策）
            }
            _templatesFlow.value = templates
            templates
        }.getOrElse { error ->
            throw PersistenceError.IOError(error)
        }
    }

    override suspend fun getTemplateById(id: String): FurnitureTemplate? {
        return runCatching {
            fileStorage.readFurnitureTemplate(id)
        }.getOrElse { error ->
            throw PersistenceError.IOError(error)
        }
    }

    override suspend fun insertTemplate(template: FurnitureTemplate) {
        runCatching {
            fileStorage.writeFurnitureTemplate(template, template.id)
            // テンプレートリストを更新
            _templatesFlow.value = getAllTemplates()
        }.getOrElse { error ->
            throw PersistenceError.IOError(error)
        }
    }

    override suspend fun updateTemplate(template: FurnitureTemplate) {
        runCatching {
            // 存在確認
            val existing = fileStorage.readFurnitureTemplate(template.id)
                ?: throw PersistenceError.NotFoundError(template.id)

            fileStorage.writeFurnitureTemplate(template, template.id)
            // テンプレートリストを更新
            _templatesFlow.value = getAllTemplates()
        }.getOrElse { error ->
            when (error) {
                is PersistenceError -> throw error
                else -> throw PersistenceError.IOError(error)
            }
        }
    }

    override suspend fun deleteTemplate(id: String) {
        runCatching {
            fileStorage.deleteFurnitureTemplate(id)
            // テンプレートリストを更新
            _templatesFlow.value = getAllTemplates()
        }.getOrElse { error ->
            throw PersistenceError.IOError(error)
        }
    }

    override fun observeTemplates(): Flow<List<FurnitureTemplate>> {
        return _templatesFlow.asStateFlow()
    }

    override fun observeTemplate(id: String): Flow<FurnitureTemplate?> {
        return _templatesFlow.map { templates ->
            templates.find { it.id == id }
        }
    }
}
