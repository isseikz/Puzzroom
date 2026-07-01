package tokyo.isseikuzumaki.vibeterminal.sharer

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import timber.log.Timber
import tokyo.isseikuzumaki.vibeterminal.VibeTerminalApp
import tokyo.isseikuzumaki.vibeterminal.domain.sharer.FileSharer
import java.io.File

/**
 * Android implementation of FileSharer.
 * Uses Intent.ACTION_SEND with FileProvider for secure file sharing.
 */
class AndroidFileSharer : FileSharer {

    private val context get() = VibeTerminalApp.applicationContext

    override fun getShareCacheDirectory(): File {
        val shareDir = File(context.cacheDir, "share")
        if (!shareDir.exists()) {
            shareDir.mkdirs()
        }
        return shareDir
    }

    override suspend fun shareFile(file: File, mimeType: String): Result<Unit> {
        return try {
            Timber.d("=== Sharing File ===")
            Timber.d("File: ${file.absolutePath}")
            Timber.d("MIME Type: $mimeType")
            Timber.d("File exists: ${file.exists()}")
            Timber.d("File size: ${file.length()} bytes")

            if (!file.exists()) {
                return Result.failure(IllegalArgumentException("File does not exist: ${file.absolutePath}"))
            }

            // Use FileProvider to get a content URI for the file
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Timber.d("File URI: $fileUri")

            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Create chooser intent
            val chooserIntent = Intent.createChooser(shareIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            Timber.d("Starting share chooser...")
            context.startActivity(chooserIntent)
            Timber.d("=== Share Chooser Started ===")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "=== Share Failed ===")
            Timber.e("Error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun cleanupShareCache() {
        try {
            val shareDir = getShareCacheDirectory()
            if (shareDir.exists()) {
                val files = shareDir.listFiles() ?: emptyArray()
                val now = System.currentTimeMillis()
                val maxAge = 60 * 60 * 1000L // 1 hour

                var deletedCount = 0
                for (file in files) {
                    // Delete files older than maxAge
                    if (now - file.lastModified() > maxAge) {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }

                if (deletedCount > 0) {
                    Timber.d("Cleaned up $deletedCount temporary share files")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to cleanup share cache")
        }
    }
}
