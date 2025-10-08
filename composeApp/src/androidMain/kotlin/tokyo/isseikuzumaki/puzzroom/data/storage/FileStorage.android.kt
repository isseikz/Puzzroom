package tokyo.isseikuzumaki.puzzroom.data.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import tokyo.isseikuzumaki.puzzroom.domain.Project
import java.io.File
import java.io.IOException

/**
 * Android実装のFileStorage
 *
 * ストレージ場所: /data/data/[package]/files/projects/
 */
actual class FileStorage(private val context: Context) : IFileStorage {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val projectsDir: File by lazy {
        File(context.filesDir, "projects").also { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    actual override suspend fun writeProject(project: Project, fileName: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(projectsDir, "$fileName.json")
                val jsonString = json.encodeToString(project)
                file.writeText(jsonString)
            } catch (e: Exception) {
                throw IOException("Failed to write project: ${e.message}", e)
            }
        }
    }

    actual override suspend fun readProject(fileName: String): Project? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(projectsDir, "$fileName.json")
                if (!file.exists()) {
                    return@withContext null
                }
                val jsonString = file.readText()
                json.decodeFromString<Project>(jsonString)
            } catch (e: Exception) {
                throw IOException("Failed to read project: ${e.message}", e)
            }
        }
    }

    actual override suspend fun deleteProject(fileName: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(projectsDir, "$fileName.json")
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                throw IOException("Failed to delete project: ${e.message}", e)
            }
        }
    }

    actual override suspend fun listProjects(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                projectsDir.listFiles()
                    ?.filter { it.isFile && it.extension == "json" }
                    ?.map { it.nameWithoutExtension }
                    ?.sorted()
                    ?: emptyList()
            } catch (e: Exception) {
                throw IOException("Failed to list projects: ${e.message}", e)
            }
        }
    }

    actual override fun isInitialized(): Boolean {
        return projectsDir.exists() && projectsDir.isDirectory
    }
}
