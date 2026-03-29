package tokyo.isseikuzumaki.vibeterminal.domain.installer

import okio.Path

/**
 * Platform-specific APK installer interface
 */
interface ApkInstaller {
    /**
     * Install an APK file
     * @param apkFile The APK file to install
     * @return Result indicating success or failure
     */
    fun installApk(apkFile: Path): Result<Unit>

    /**
     * Get the cache directory for temporary APK storage
     */
    fun getCacheDir(): Path
}
