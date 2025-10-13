package tokyo.isseikuzumaki.puzzroom.data.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate
import tokyo.isseikuzumaki.puzzroom.domain.Project
import java.io.File
import java.io.IOException

/**
 * Android実装のFileStorage
 *
 * ストレージ場所: 
 * - Projects: /data/data/[package]/files/projects/
 * - Furniture Templates: /data/data/[package]/files/furniture_templates/
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

    private val furnitureTemplatesDir: File by lazy {
        File(context.filesDir, "furniture_templates").also { dir ->
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

    actual override suspend fun writeFurnitureTemplate(template: FurnitureTemplate, fileName: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(furnitureTemplatesDir, "$fileName.json")
                val jsonString = json.encodeToString(template)
                file.writeText(jsonString)
            } catch (e: Exception) {
                throw IOException("Failed to write furniture template: ${e.message}", e)
            }
        }
    }

    actual override suspend fun readFurnitureTemplate(fileName: String): FurnitureTemplate? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(furnitureTemplatesDir, "$fileName.json")
                if (!file.exists()) {
                    return@withContext null
                }
                val jsonString = file.readText()
                json.decodeFromString<FurnitureTemplate>(jsonString)
            } catch (e: Exception) {
                throw IOException("Failed to read furniture template: ${e.message}", e)
            }
        }
    }

    actual override suspend fun deleteFurnitureTemplate(fileName: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(furnitureTemplatesDir, "$fileName.json")
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                throw IOException("Failed to delete furniture template: ${e.message}", e)
            }
        }
    }

    actual override suspend fun listFurnitureTemplates(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                furnitureTemplatesDir.listFiles()
                    ?.filter { it.isFile && it.extension == "json" }
                    ?.map { it.nameWithoutExtension }
                    ?.sorted()
                    ?: emptyList()
            } catch (e: Exception) {
                throw IOException("Failed to list furniture templates: ${e.message}", e)
            }
        }
    }
}
