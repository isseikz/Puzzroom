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
        return try {
            val fileNames = fileStorage.listFurnitureTemplates()
            val templates = fileNames.mapNotNull { fileName ->
                try {
                    fileStorage.readFurnitureTemplate(fileName)
                } catch (e: Exception) {
                    // ファイルの読み込みに失敗した場合はスキップ（破損ファイル対策）
                    null
                }
            }
            _templatesFlow.value = templates
            templates
        } catch (e: Exception) {
            throw PersistenceError.IOError(e)
        }
    }

    override suspend fun getTemplateById(id: String): FurnitureTemplate? {
        return try {
            fileStorage.readFurnitureTemplate(id)
        } catch (e: Exception) {
            throw PersistenceError.IOError(e)
        }
    }

    override suspend fun insertTemplate(template: FurnitureTemplate) {
        try {
            fileStorage.writeFurnitureTemplate(template, template.id)
            // テンプレートリストを更新
            _templatesFlow.value = getAllTemplates()
        } catch (e: Exception) {
            throw PersistenceError.IOError(e)
        }
    }

    override suspend fun updateTemplate(template: FurnitureTemplate) {
        try {
            // 存在確認
            val existing = fileStorage.readFurnitureTemplate(template.id)
                ?: throw PersistenceError.NotFoundError(template.id)

            fileStorage.writeFurnitureTemplate(template, template.id)
            // テンプレートリストを更新
            _templatesFlow.value = getAllTemplates()
        } catch (e: PersistenceError) {
            throw e
        } catch (e: Exception) {
            throw PersistenceError.IOError(e)
        }
    }

    override suspend fun deleteTemplate(id: String) {
        try {
            fileStorage.deleteFurnitureTemplate(id)
            // テンプレートリストを更新
            _templatesFlow.value = getAllTemplates()
        } catch (e: Exception) {
            throw PersistenceError.IOError(e)
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
