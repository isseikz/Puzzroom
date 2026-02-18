package tokyo.isseikuzumaki.vibeterminal.di

import org.koin.dsl.module
import tokyo.isseikuzumaki.vibeterminal.data.repository.ConnectionRepositoryStub
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.installer.JvmApkInstallerStub
import tokyo.isseikuzumaki.vibeterminal.ssh.MinaSshdRepository
import tokyo.isseikuzumaki.vibeterminal.domain.downloader.FileDownloader
import tokyo.isseikuzumaki.vibeterminal.domain.sharer.FileSharer
import tokyo.isseikuzumaki.vibeterminal.downloader.JvmFileDownloader
import tokyo.isseikuzumaki.vibeterminal.sharer.JvmFileSharer

actual fun platformModule() = module {
    factory<SshRepository> { MinaSshdRepository() }
    factory<ApkInstaller> { JvmApkInstallerStub() }
    factory<ConnectionRepository> { ConnectionRepositoryStub() }
    factory<FileDownloader> { JvmFileDownloader() }
    factory<FileSharer> { JvmFileSharer() }
}
