package tokyo.isseikuzumaki.quickdeploy.api

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.quickdeploy.model.DownloadProgress
import tokyo.isseikuzumaki.quickdeploy.model.RegisterRequest
import tokyo.isseikuzumaki.quickdeploy.model.RegisterResponse
import java.io.File

/**
 * API client for Quick Deploy backend (Firebase Functions)
 */
class QuickDeployApiClient(
    private val baseUrl: String
) {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Register device and get upload/download URLs (A-001)
     */
    suspend fun registerDevice(request: RegisterRequest): Result<RegisterResponse> {
        return try {
            val response: RegisterResponse = client.post("$baseUrl/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            Log.d(TAG, "Device registered successfully: ${response.deviceToken}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device", e)
            Result.failure(e)
        }
    }

    /**
     * Download APK file from the server (A-005)
     */
    suspend fun downloadApk(downloadUrl: String, destinationFile: File): Result<File> {
        return try {
            Log.d(TAG, "Starting APK download from: $downloadUrl")

            val response: HttpResponse = client.get(downloadUrl)

            if (response.status.isSuccess()) {
                val bytes = response.body<ByteArray>()
                destinationFile.parentFile?.mkdirs()
                destinationFile.writeBytes(bytes)

                Log.d(TAG, "APK downloaded successfully: ${destinationFile.absolutePath} (${bytes.size} bytes)")
                Result.success(destinationFile)
            } else {
                val error = "Failed to download APK: ${response.status}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download APK", e)
            Result.failure(e)
        }
    }

    /**
     * Download APK file from the server with progress reporting (A-005)
     */
    suspend fun downloadApkWithProgress(
        downloadUrl: String,
        destinationFile: File,
        onProgress: (DownloadProgress) -> Unit
    ): Result<File> {
        return try {
            Log.d(TAG, "Starting APK download with progress from: $downloadUrl")
            val startTime = System.currentTimeMillis()

            client.prepareGet(downloadUrl).execute { response ->
                if (response.status.isSuccess()) {
                    val totalBytes = response.contentLength() ?: -1L
                    val channel: ByteReadChannel = response.bodyAsChannel()

                    destinationFile.parentFile?.mkdirs()
                    destinationFile.outputStream().use { output ->
                        var bytesDownloaded = 0L
                        var lastProgressUpdate = 0L
                        val buffer = ByteArray(8192)

                        while (!channel.isClosedForRead) {
                            val bytesRead = channel.readAvailable(buffer)
                            if (bytesRead == -1) break
                            if (bytesRead > 0) {
                                output.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead
                                
                                // Throttle progress updates to avoid overwhelming UI (update every ~100ms or 1% progress)
                                val currentTime = System.currentTimeMillis()
                                val shouldUpdateByTime = currentTime - lastProgressUpdate >= 100
                                if (shouldUpdateByTime || totalBytes > 0) {
                                    val progressPercent = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes * 100).toInt() else 0
                                    val lastProgressPercent = if (totalBytes > 0) ((bytesDownloaded - bytesRead).toFloat() / totalBytes * 100).toInt() else 0
                                    if (shouldUpdateByTime || progressPercent != lastProgressPercent) {
                                        onProgress(DownloadProgress(bytesDownloaded, totalBytes, startTime))
                                        lastProgressUpdate = currentTime
                                    }
                                }
                            }
                        }
                        
                        // Final progress update
                        onProgress(DownloadProgress(bytesDownloaded, totalBytes, startTime))
                    }

                    Log.d(TAG, "APK downloaded successfully with progress: ${destinationFile.absolutePath}")
                    Result.success(destinationFile)
                } else {
                    val error = "Failed to download APK: ${response.status}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download APK with progress", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "QuickDeployApiClient"
    }
}
