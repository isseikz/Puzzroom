package tokyo.isseikuzumaki.vibeterminal.sharer

import tokyo.isseikuzumaki.vibeterminal.domain.sharer.FileSharer
import java.awt.Desktop
import java.io.File

/**
 * JVM/Desktop implementation of FileSharer.
 * Opens files with the default system application as a fallback for sharing.
 */
class JvmFileSharer : FileSharer {
    override fun getShareCacheDirectory(): File {
        val tempDir = System.getProperty("java.io.tmpdir")
        val shareDir = File(tempDir, "vibeterminal-share")
        if (!shareDir.exists()) {
            shareDir.mkdirs()
        }
        return shareDir
    }

    override suspend fun shareFile(file: File, mimeType: String): Result<Unit> {
        return try {
            if (!file.exists()) {
                return Result.failure(IllegalArgumentException("File does not exist: ${file.absolutePath}"))
            }

            // On desktop, open with default application as a fallback
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file)
                Result.success(Unit)
            } else {
                Result.failure(UnsupportedOperationException("Desktop not supported on this platform"))
            }
        } catch (e: Exception) {
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

                for (file in files) {
                    if (now - file.lastModified() > maxAge) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
    }
}
