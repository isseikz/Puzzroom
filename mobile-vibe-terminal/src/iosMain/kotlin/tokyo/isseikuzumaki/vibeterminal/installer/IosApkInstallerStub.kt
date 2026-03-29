package tokyo.isseikuzumaki.vibeterminal.installer

import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller

@OptIn(ExperimentalForeignApi::class)
class IosApkInstallerStub : ApkInstaller {
    override fun installApk(apkFile: Path): Result<Unit> {
        return Result.failure(NotImplementedError("APK installation not supported on iOS"))
    }

    override fun getCacheDir(): Path {
        return NSTemporaryDirectory().toPath()
    }
}
