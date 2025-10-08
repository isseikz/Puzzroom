package tokyo.isseikuzumaki.puzzroom.data.storage

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import platform.Foundation.*
import tokyo.isseikuzumaki.puzzroom.domain.Project

/**
 * iOS実装のFileStorage
 *
 * ストレージ場所: Documents/projects/
 */
@OptIn(ExperimentalForeignApi::class)
actual class FileStorage : IFileStorage {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val fileManager = NSFileManager.defaultManager

    private val documentsPath: String by lazy {
        NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String ?: throw IllegalStateException("Documents directory not found")
    }

    private val projectsPath: String by lazy {
        "$documentsPath/projects"
    }

    init {
        // プロジェクトディレクトリを作成
        if (!fileManager.fileExistsAtPath(projectsPath)) {
            fileManager.createDirectoryAtPath(
                projectsPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }
    }

    actual override suspend fun writeProject(project: Project, fileName: String) {
        withContext(Dispatchers.IO) {
            try {
                val filePath = "$projectsPath/$fileName.json"
                val jsonString = json.encodeToString(project)
                val nsString = jsonString as NSString
                nsString.writeToFile(
                    filePath,
                    atomically = true,
                    encoding = NSUTF8StringEncoding,
                    error = null
                )
            } catch (e: Exception) {
                throw Exception("Failed to write project: ${e.message}", e)
            }
        }
    }

    actual override suspend fun readProject(fileName: String): Project? {
        return withContext(Dispatchers.IO) {
            try {
                val filePath = "$projectsPath/$fileName.json"
                if (!fileManager.fileExistsAtPath(filePath)) {
                    return@withContext null
                }
                val nsString = NSString.stringWithContentsOfFile(
                    filePath,
                    encoding = NSUTF8StringEncoding,
                    error = null
                ) ?: return@withContext null

                json.decodeFromString<Project>(nsString as String)
            } catch (e: Exception) {
                throw Exception("Failed to read project: ${e.message}", e)
            }
        }
    }

    actual override suspend fun deleteProject(fileName: String) {
        withContext(Dispatchers.IO) {
            try {
                val filePath = "$projectsPath/$fileName.json"
                if (fileManager.fileExistsAtPath(filePath)) {
                    fileManager.removeItemAtPath(filePath, error = null)
                }
            } catch (e: Exception) {
                throw Exception("Failed to delete project: ${e.message}", e)
            }
        }
    }

    actual override suspend fun listProjects(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val contents = fileManager.contentsOfDirectoryAtPath(projectsPath, error = null)
                    as? List<*> ?: emptyList<Any>()

                contents
                    .filterIsInstance<String>()
                    .filter { it.endsWith(".json") }
                    .map { it.removeSuffix(".json") }
                    .sorted()
            } catch (e: Exception) {
                throw Exception("Failed to list projects: ${e.message}", e)
            }
        }
    }

    actual override fun isInitialized(): Boolean {
        return fileManager.fileExistsAtPath(projectsPath)
    }
}
