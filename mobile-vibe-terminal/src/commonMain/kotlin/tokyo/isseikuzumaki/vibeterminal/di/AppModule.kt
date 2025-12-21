package tokyo.isseikuzumaki.vibeterminal.di

import org.koin.dsl.module
import tokyo.isseikuzumaki.vibeterminal.viewmodel.ConnectionListScreenModel
import tokyo.isseikuzumaki.vibeterminal.viewmodel.TerminalScreenModel

val appModule = module {
    factory { ConnectionListScreenModel(connectionRepository = get()) }

    factory { params ->
        TerminalScreenModel(
            config = params.get(),
            sshRepository = get(),
            apkInstaller = get()
        )
    }
}

val dataModule = module {
    // Repository instances
}

val databaseModule = module {
    // Room database instance (platform-specific)
}
