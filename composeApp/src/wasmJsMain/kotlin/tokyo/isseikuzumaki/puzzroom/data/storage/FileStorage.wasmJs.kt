package tokyo.isseikuzumaki.puzzroom.data.storage

import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.puzzroom.domain.Project

actual class FileStorage : IFileStorage {
    private val projectKeyPrefix = "puzzroom_project_"

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
}
