package tokyo.isseikuzumaki.vibeterminal.domain.installer

import java.io.File

/**
 * Platform-specific APK installer interface
 */
interface ApkInstaller {
    /**
     * Install an APK file
     * @param apkFile The APK file to install
     * @return Result indicating success or failure
     */
    fun installApk(apkFile: File): Result<Unit>

    /**
     * Get the cache directory for temporary APK storage
     */
    fun getCacheDir(): File
}
