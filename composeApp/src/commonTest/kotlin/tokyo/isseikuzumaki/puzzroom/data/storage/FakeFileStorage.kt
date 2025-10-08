package tokyo.isseikuzumaki.puzzroom.data.storage

import tokyo.isseikuzumaki.puzzroom.domain.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * テスト用のFakeFileStorage実装
 *
 * メモリ内でプロジェクトを保存・読み込みします。
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
        storage["$fileName.json"] = jsonString
    }

    override suspend fun readProject(fileName: String): Project? {
        val jsonString = storage["$fileName.json"] ?: return null
        return json.decodeFromString<Project>(jsonString)
    }

    override suspend fun deleteProject(fileName: String) {
        storage.remove("$fileName.json")
    }

    override suspend fun listProjects(): List<String> {
        return storage.keys
            .filter { it.endsWith(".json") }
            .map { it.removeSuffix(".json") }
            .sorted()
    }

    override fun isInitialized(): Boolean = true

    // Test helper methods
    fun clear() {
        storage.clear()
    }

    fun getStorageSize(): Int = storage.size
}
