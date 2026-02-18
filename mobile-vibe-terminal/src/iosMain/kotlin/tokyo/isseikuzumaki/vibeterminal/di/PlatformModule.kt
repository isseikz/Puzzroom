package tokyo.isseikuzumaki.vibeterminal.di

import org.koin.dsl.module
import tokyo.isseikuzumaki.vibeterminal.data.repository.ConnectionRepositoryStub
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.installer.IosApkInstallerStub
import tokyo.isseikuzumaki.vibeterminal.ssh.SshRepositoryStub
import tokyo.isseikuzumaki.vibeterminal.domain.downloader.FileDownloader
import tokyo.isseikuzumaki.vibeterminal.domain.sharer.FileSharer
import tokyo.isseikuzumaki.vibeterminal.downloader.FileDownloaderStub
import tokyo.isseikuzumaki.vibeterminal.sharer.FileSharerStub

actual fun platformModule() = module {
    factory<SshRepository> { SshRepositoryStub() }
    factory<ApkInstaller> { IosApkInstallerStub() }
    factory<ConnectionRepository> { ConnectionRepositoryStub() }
    factory<FileDownloader> { FileDownloaderStub() }
    factory<FileSharer> { FileSharerStub() }
}
