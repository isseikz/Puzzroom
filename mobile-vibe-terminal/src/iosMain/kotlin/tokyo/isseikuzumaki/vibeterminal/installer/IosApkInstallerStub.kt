package tokyo.isseikuzumaki.vibeterminal.installer

import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import java.io.File

class IosApkInstallerStub : ApkInstaller {
    override fun installApk(apkFile: File): Result<Unit> {
        return Result.failure(NotImplementedError("APK installation not supported on iOS"))
    }

    override fun getCacheDir(): File {
        // iOS would use NSTemporaryDirectory() or similar
        return File(System.getProperty("java.io.tmpdir") ?: "/tmp")
    }
}
