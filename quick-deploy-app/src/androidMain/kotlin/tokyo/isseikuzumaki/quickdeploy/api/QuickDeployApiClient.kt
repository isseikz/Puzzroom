package tokyo.isseikuzumaki.quickdeploy.api

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
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
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
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

    companion object {
        private const val TAG = "QuickDeployApiClient"
        
        // Timeout configurations for large file downloads (140MB+ APK files)
        private const val CONNECT_TIMEOUT_MS = 30_000L       // 30 seconds for connection establishment
        private const val SOCKET_TIMEOUT_MS = 300_000L      // 5 minutes for socket read/write (handles slow networks)
        private const val REQUEST_TIMEOUT_MS = 600_000L     // 10 minutes total request timeout for large files
    }
}
