package tokyo.isseikuzumaki.vibeterminal.installer

import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import java.io.File

class JvmApkInstallerStub : ApkInstaller {
    override fun installApk(apkFile: File): Result<Unit> {
        return Result.failure(NotImplementedError("APK installation not supported on JVM/Desktop"))
    }

    override fun getCacheDir(): File {
        return File(System.getProperty("java.io.tmpdir"))
    }
}
