package tokyo.isseikuzumaki.vibeterminal.sharer

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import tokyo.isseikuzumaki.vibeterminal.domain.sharer.FileSharer
import kotlin.coroutines.resume

/**
 * iOS implementation of FileSharer.
 * Presents UIActivityViewController for sharing a file via the system share sheet.
 */
@OptIn(ExperimentalForeignApi::class)
class FileSharerIos : FileSharer {

    override fun getShareCacheDirectory(): Path {
        val cachesUrl = NSFileManager.defaultManager.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        val cachePath = requireNotNull(cachesUrl?.path) { "Could not find Caches directory" }
        val sharePath = "$cachePath/share"

        val fm = NSFileManager.defaultManager
        if (!fm.fileExistsAtPath(sharePath)) {
            fm.createDirectoryAtPath(
                path = sharePath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }

        return sharePath.toPath()
    }

    override suspend fun shareFile(file: Path, mimeType: String): Result<Unit> {
        return try {
            val fileUrl = NSURL.fileURLWithPath(file.toString())
            val activityVC = UIActivityViewController(
                activityItems = listOf(fileUrl),
                applicationActivities = null
            )

            val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: return Result.failure(IllegalStateException("No root view controller available"))

            // iPad popover configuration handled by iOS automatically

            suspendCancellableCoroutine { continuation ->
                activityVC.setCompletionWithItemsHandler { _, _, _, _ ->
                    continuation.resume(Result.success(Unit))
                }
                rootVC.presentViewController(activityVC, animated = true, completion = null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cleanupShareCache() {
        val shareDirPath = getShareCacheDirectory().toString()
        val fm = NSFileManager.defaultManager
        val contents = fm.contentsOfDirectoryAtPath(shareDirPath, error = null) ?: return
        val nowMs = (NSDate().timeIntervalSince1970 * 1000).toLong()
        val maxAgeMs = 60 * 60 * 1000L // 1 hour

        @Suppress("UNCHECKED_CAST")
        for (name in (contents as List<String>)) {
            val filePath = "$shareDirPath/$name"
            val attributes = fm.attributesOfItemAtPath(filePath, error = null) ?: continue
            val modDate = attributes["NSFileModificationDate"] as? NSDate ?: continue
            val fileMs = (modDate.timeIntervalSince1970 * 1000).toLong()
            if (nowMs - fileMs > maxAgeMs) {
                fm.removeItemAtPath(filePath, error = null)
            }
        }
    }
}
