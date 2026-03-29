package tokyo.isseikuzumaki.vibeterminal.installer

import okio.Path
import okio.Path.Companion.toPath
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller

class JvmApkInstallerStub : ApkInstaller {
    override fun installApk(apkFile: Path): Result<Unit> {
        return Result.failure(NotImplementedError("APK installation not supported on JVM/Desktop"))
    }

    override fun getCacheDir(): Path {
        return System.getProperty("java.io.tmpdir").toPath()
    }
}
