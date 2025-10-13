package tokyo.isseikuzumaki.puzzroom.data.storage

import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate
import tokyo.isseikuzumaki.puzzroom.domain.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * テスト用のFakeFileStorage実装
 *
 * メモリ内でプロジェクトと家具テンプレートを保存・読み込みします。
 */
class FakeFileStorage : IFileStorage {
    private val storage = mutableMapOf<String, String>()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun writeProject(project: Project, fileName: String) {
        val jsonString = json.encodeToString(project)
        storage["project_$fileName.json"] = jsonString
    }

    override suspend fun readProject(fileName: String): Project? {
        val jsonString = storage["project_$fileName.json"] ?: return null
        return json.decodeFromString<Project>(jsonString)
    }

    override suspend fun deleteProject(fileName: String) {
        storage.remove("project_$fileName.json")
    }

    override suspend fun listProjects(): List<String> {
        return storage.keys
            .filter { it.startsWith("project_") && it.endsWith(".json") }
            .map { it.removePrefix("project_").removeSuffix(".json") }
            .sorted()
    }

    override fun isInitialized(): Boolean = true

    override suspend fun writeFurnitureTemplate(template: FurnitureTemplate, fileName: String) {
        val jsonString = json.encodeToString(template)
        storage["template_$fileName.json"] = jsonString
    }

    override suspend fun readFurnitureTemplate(fileName: String): FurnitureTemplate? {
        val jsonString = storage["template_$fileName.json"] ?: return null
        return json.decodeFromString<FurnitureTemplate>(jsonString)
    }

    override suspend fun deleteFurnitureTemplate(fileName: String) {
        storage.remove("template_$fileName.json")
    }

    override suspend fun listFurnitureTemplates(): List<String> {
        return storage.keys
            .filter { it.startsWith("template_") && it.endsWith(".json") }
            .map { it.removePrefix("template_").removeSuffix(".json") }
            .sorted()
    }

    // Test helper methods
    fun clear() {
        storage.clear()
    }

    fun getStorageSize(): Int = storage.size
}
