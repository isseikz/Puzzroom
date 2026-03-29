package tokyo.isseikuzumaki.vibeterminal.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.dsl.module
import tokyo.isseikuzumaki.vibeterminal.data.database.AppDatabase
import tokyo.isseikuzumaki.vibeterminal.data.database.getRoomDatabase
import tokyo.isseikuzumaki.vibeterminal.data.datastore.PreferencesHelper
import tokyo.isseikuzumaki.vibeterminal.data.datastore.createDataStore
import tokyo.isseikuzumaki.vibeterminal.data.repository.ConnectionRepositoryImpl
import tokyo.isseikuzumaki.vibeterminal.domain.downloader.FileDownloader
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.domain.sharer.FileSharer
import tokyo.isseikuzumaki.vibeterminal.downloader.FileDownloaderIos
import tokyo.isseikuzumaki.vibeterminal.installer.IosApkInstallerStub
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyProvider
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyProviderIos
import tokyo.isseikuzumaki.vibeterminal.sharer.FileSharerIos
import tokyo.isseikuzumaki.vibeterminal.ssh.SshRepositoryIos

actual fun platformModule() = module {
    single<AppDatabase> { getRoomDatabase() }

    single<DataStore<Preferences>> { createDataStore() }

    single { PreferencesHelper(get()) }

    single<SshKeyProvider> { SshKeyProviderIos() }

    single<SshRepository> { SshRepositoryIos() }

    factory<ApkInstaller> { IosApkInstallerStub() }

    factory<FileDownloader> { FileDownloaderIos() }

    factory<FileSharer> { FileSharerIos() }

    factory<ConnectionRepository> {
        ConnectionRepositoryImpl(
            dao = get<AppDatabase>().serverConnectionDao(),
            dataStore = get()
        )
    }
}
