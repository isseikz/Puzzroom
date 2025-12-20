package tokyo.isseikuzumaki.vibeterminal.di

import android.content.Context
import org.koin.dsl.module
import tokyo.isseikuzumaki.vibeterminal.data.database.AppDatabase
import tokyo.isseikuzumaki.vibeterminal.data.database.getRoomDatabase
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.ssh.MinaSshdRepository

actual fun platformModule() = module {
    single<AppDatabase> {
        val context = get<Context>()
        getRoomDatabase(context)
    }

    factory<SshRepository> { MinaSshdRepository() }
}
