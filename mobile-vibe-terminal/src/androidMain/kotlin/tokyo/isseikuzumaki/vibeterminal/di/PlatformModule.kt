package tokyo.isseikuzumaki.vibeterminal.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.koin.dsl.module
import tokyo.isseikuzumaki.vibeterminal.data.database.AppDatabase
import tokyo.isseikuzumaki.vibeterminal.data.database.getRoomDatabase
import tokyo.isseikuzumaki.vibeterminal.data.datastore.PreferencesHelper
import tokyo.isseikuzumaki.vibeterminal.data.datastore.createDataStore
import tokyo.isseikuzumaki.vibeterminal.data.repository.ConnectionRepositoryImpl
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.installer.AndroidApkInstaller
import tokyo.isseikuzumaki.vibeterminal.installer.TriggerEventHandler
import tokyo.isseikuzumaki.vibeterminal.security.PasswordEncryptionHelper
import tokyo.isseikuzumaki.vibeterminal.ssh.MinaSshdRepository

actual fun platformModule() = module {
    single<AppDatabase> {
        val context = get<Context>()
        getRoomDatabase(context)
    }

    single<DataStore<Preferences>> {
        val context = get<Context>()
        createDataStore(context)
    }

    single { PasswordEncryptionHelper() }

    single { PreferencesHelper(get()) }

    single<SshRepository> { MinaSshdRepository() }

    factory<ApkInstaller> {
        val context = get<Context>()
        AndroidApkInstaller(context)
    }

    single {
        val context = get<Context>()
        val preferencesHelper = get<PreferencesHelper>()
        val apkInstaller = get<ApkInstaller>()
        val sshRepository = get<SshRepository>()
        TriggerEventHandler.getInstance(context, preferencesHelper, apkInstaller, sshRepository)
    }

    factory<ConnectionRepository> {
        val database = get<AppDatabase>()
        val dataStore = get<DataStore<Preferences>>()
        val passwordEncryption = get<PasswordEncryptionHelper>()
        ConnectionRepositoryImpl(database.serverConnectionDao(), dataStore, passwordEncryption)
    }
}
