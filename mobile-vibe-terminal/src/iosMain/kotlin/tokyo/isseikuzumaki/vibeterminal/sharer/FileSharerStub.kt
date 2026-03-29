package tokyo.isseikuzumaki.vibeterminal.sharer

import okio.Path
import tokyo.isseikuzumaki.vibeterminal.domain.sharer.FileSharer

/**
 * iOS stub implementation of FileSharer.
 * File sharing is not yet supported on iOS.
 */
class FileSharerStub : FileSharer {
    override fun getShareCacheDirectory(): Path {
        throw NotImplementedError("File sharing not supported on iOS yet")
    }

    override suspend fun shareFile(file: Path, mimeType: String): Result<Unit> {
        return Result.failure(NotImplementedError("File sharing not supported on iOS yet"))
    }

    override suspend fun cleanupShareCache() {
        // No-op on iOS
    }
}
