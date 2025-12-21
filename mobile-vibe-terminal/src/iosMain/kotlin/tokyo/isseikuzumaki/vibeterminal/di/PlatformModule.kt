package tokyo.isseikuzumaki.vibeterminal.di

import org.koin.dsl.module
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.installer.IosApkInstallerStub
import tokyo.isseikuzumaki.vibeterminal.ssh.SshRepositoryStub

actual fun platformModule() = module {
    factory<SshRepository> { SshRepositoryStub() }
    factory<ApkInstaller> { IosApkInstallerStub() }
}
