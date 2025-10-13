package tokyo.isseikuzumaki.puzzroom.data.storage

import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate
import tokyo.isseikuzumaki.puzzroom.domain.Project

actual class FileStorage : IFileStorage {
    private val projectKeyPrefix = "puzzroom_project_"
    private val furnitureTemplateKeyPrefix = "puzzroom_furniture_template_"

    actual override suspend fun writeProject(project: Project, fileName: String) {
        val key = projectKeyPrefix + fileName
        val json = Json.encodeToString(project)
        localStorage.setItem(key, json)
    }

    actual override suspend fun readProject(fileName: String): Project? {
        val key = projectKeyPrefix + fileName
        val json = localStorage.getItem(key)
        return if (json != null) {
            Json.decodeFromString<Project>(json)
        } else {
            null
        }
    }

    actual override suspend fun deleteProject(fileName: String) {
        val key = projectKeyPrefix + fileName
        localStorage.removeItem(key)
    }

    actual override suspend fun listProjects(): List<String> {
        val projectKeys = (0 until localStorage.length).mapNotNull { i ->
            localStorage.key(i)?.takeIf { it.startsWith(projectKeyPrefix) }
        }
        return projectKeys.map { it.removePrefix(projectKeyPrefix) }
    }

    actual override fun isInitialized(): Boolean {
        return true
    }

    actual override suspend fun writeFurnitureTemplate(template: FurnitureTemplate, fileName: String) {
        val key = furnitureTemplateKeyPrefix + fileName
        val json = Json.encodeToString(template)
        localStorage.setItem(key, json)
    }

    actual override suspend fun readFurnitureTemplate(fileName: String): FurnitureTemplate? {
        val key = furnitureTemplateKeyPrefix + fileName
        val json = localStorage.getItem(key)
        return if (json != null) {
            Json.decodeFromString<FurnitureTemplate>(json)
        } else {
            null
        }
    }

    actual override suspend fun deleteFurnitureTemplate(fileName: String) {
        val key = furnitureTemplateKeyPrefix + fileName
        localStorage.removeItem(key)
    }

    actual override suspend fun listFurnitureTemplates(): List<String> {
        val templateKeys = (0 until localStorage.length).mapNotNull { i ->
            localStorage.key(i)?.takeIf { it.startsWith(furnitureTemplateKeyPrefix) }
        }
        return templateKeys.map { it.removePrefix(furnitureTemplateKeyPrefix) }
    }
}
