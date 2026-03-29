package tokyo.isseikuzumaki.vibeterminal.sharer

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import okio.Path
import okio.Path.Companion.toOkioPath
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

    override fun getShareCacheDirectory(): Path {
        val shareDir = File(context.cacheDir, "share")
        if (!shareDir.exists()) shareDir.mkdirs()
        return shareDir.toOkioPath()
    }

    override suspend fun shareFile(file: Path, mimeType: String): Result<Unit> {
        return try {
            val javaFile = file.toFile()
            Timber.d("=== Sharing File ===")
            Timber.d("File: ${javaFile.absolutePath}")
            Timber.d("MIME Type: $mimeType")

            if (!javaFile.exists()) {
                return Result.failure(IllegalArgumentException("File does not exist: ${javaFile.absolutePath}"))
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                javaFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(shareIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooserIntent)
            Timber.d("=== Share Chooser Started ===")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "=== Share Failed ===")
            Result.failure(e)
        }
    }

    override suspend fun cleanupShareCache() {
        try {
            val shareDir = getShareCacheDirectory().toFile()
            if (shareDir.exists()) {
                val now = System.currentTimeMillis()
                val maxAge = 60 * 60 * 1000L
                shareDir.listFiles()?.forEach { file ->
                    if (now - file.lastModified() > maxAge) file.delete()
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to cleanup share cache")
        }
    }
}
