package tokyo.isseikuzumaki.vibeterminal.sharer

import tokyo.isseikuzumaki.vibeterminal.domain.sharer.FileSharer
import java.io.File

/**
 * iOS stub implementation of FileSharer.
 * File sharing is not yet supported on iOS.
 */
class FileSharerStub : FileSharer {
    override fun getShareCacheDirectory(): File {
        throw NotImplementedError("File sharing not supported on iOS yet")
    }

    override suspend fun shareFile(file: File, mimeType: String): Result<Unit> {
        return Result.failure(NotImplementedError("File sharing not supported on iOS yet"))
    }

    override suspend fun cleanupShareCache() {
        // No-op on iOS
    }
}
