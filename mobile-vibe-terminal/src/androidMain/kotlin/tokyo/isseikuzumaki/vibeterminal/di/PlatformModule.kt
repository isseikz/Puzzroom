package tokyo.isseikuzumaki.vibeterminal.di

import android.content.Context
import org.koin.dsl.module
import tokyo.isseikuzumaki.vibeterminal.data.database.AppDatabase
import tokyo.isseikuzumaki.vibeterminal.data.database.getRoomDatabase
import tokyo.isseikuzumaki.vibeterminal.data.repository.ConnectionRepositoryImpl
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.installer.AndroidApkInstaller
import tokyo.isseikuzumaki.vibeterminal.ssh.MinaSshdRepository

actual fun platformModule() = module {
    single<AppDatabase> {
        val context = get<Context>()
        getRoomDatabase(context)
    }

    factory<SshRepository> { MinaSshdRepository() }

    factory<ApkInstaller> {
        val context = get<Context>()
        AndroidApkInstaller(context)
    }

    factory<ConnectionRepository> {
        val database = get<AppDatabase>()
        ConnectionRepositoryImpl(database.serverConnectionDao())
    }
}
