package tokyo.isseikuzumaki.quickdeploy.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tokyo.isseikuzumaki.quickdeploy.api.QuickDeployApiClient
import java.io.File

/**
 * Utility class for downloading and installing APK files (C-003, C-004, C-005)
 */
class ApkInstaller(
    private val context: Context,
    private val apiClient: QuickDeployApiClient
) {

    /**
     * Check if the app has permission to install packages (C-005)
     */
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true // No special permission needed for older versions
        }
    }

    /**
     * Open settings to allow installation from unknown sources (C-005)
     */
    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Download APK from server (C-003)
     */
    suspend fun downloadApk(downloadUrl: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val apkDir = File(context.cacheDir, "apks")
            apkDir.mkdirs()

            val apkFile = File(apkDir, "downloaded_app.apk")

            // Delete old APK if exists
            if (apkFile.exists()) {
                apkFile.delete()
                Log.d(TAG, "Deleted old APK file")
            }

            // Download new APK
            Log.d(TAG, "Downloading APK from: $downloadUrl")
            apiClient.downloadApk(downloadUrl, apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download APK", e)
            Result.failure(e)
        }
    }

    /**
     * Install APK file (C-004)
     */
    fun installApk(apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(Exception("APK file not found: ${apkFile.absolutePath}"))
            }

            // Check install permission
            if (!canInstallPackages()) {
                return Result.failure(InstallPermissionException("Install permission not granted"))
            }

            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
            Log.d(TAG, "APK installation started: ${apkFile.absolutePath}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK", e)
            Result.failure(e)
        }
    }

    /**
     * Download and install APK in one operation
     */
    suspend fun downloadAndInstall(downloadUrl: String): Result<Unit> {
        return try {
            // Check permission first
            if (!canInstallPackages()) {
                return Result.failure(InstallPermissionException("Install permission not granted"))
            }

            // Download APK
            val downloadResult = downloadApk(downloadUrl)
            if (downloadResult.isFailure) {
                return Result.failure(downloadResult.exceptionOrNull()!!)
            }

            val apkFile = downloadResult.getOrNull()!!

            // Install APK
            installApk(apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download and install APK", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "ApkInstaller"
    }
}

/**
 * Exception thrown when install permission is not granted
 */
class InstallPermissionException(message: String) : Exception(message)
