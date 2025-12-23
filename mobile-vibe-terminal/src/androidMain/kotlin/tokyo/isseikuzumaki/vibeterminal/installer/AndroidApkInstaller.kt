package tokyo.isseikuzumaki.vibeterminal.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import timber.log.Timber
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import java.io.File

class AndroidApkInstaller(private val context: Context) : ApkInstaller {

    override fun installApk(apkFile: File): Result<Unit> {
        return try {
            Timber.d("=== Installing APK ===")
            Timber.d("APK File: ${apkFile.absolutePath}")
            Timber.d("File exists: ${apkFile.exists()}")
            Timber.d("File size: ${apkFile.length()} bytes")

            if (!apkFile.exists()) {
                return Result.failure(IllegalArgumentException("APK file does not exist"))
            }

            // Use FileProvider to get a content URI for the APK
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            Timber.d("APK URI: $apkUri")

            // Create install intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            Timber.d("Starting install intent...")
            context.startActivity(intent)
            Timber.d("=== Install Intent Started ===")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "=== Install Failed ===")
            Timber.e("Error: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getCacheDir(): File {
        return context.cacheDir
    }
}
