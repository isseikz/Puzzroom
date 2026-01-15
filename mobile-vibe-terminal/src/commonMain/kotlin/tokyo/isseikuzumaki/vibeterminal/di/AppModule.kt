package tokyo.isseikuzumaki.vibeterminal.di

import org.koin.dsl.module
import tokyo.isseikuzumaki.vibeterminal.macro.data.local.MacroRepositoryImpl
import tokyo.isseikuzumaki.vibeterminal.macro.domain.repository.MacroRepository
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyProvider
import tokyo.isseikuzumaki.vibeterminal.viewmodel.ConnectionListScreenModel
import tokyo.isseikuzumaki.vibeterminal.viewmodel.FileExplorerScreenModel
import tokyo.isseikuzumaki.vibeterminal.viewmodel.TerminalScreenModel

val appModule = module {
    factory { ConnectionListScreenModel(connectionRepository = get(), sshKeyProvider = getOrNull<SshKeyProvider>()) }

    factory { params ->
        TerminalScreenModel(
            config = params.get(),
            sshRepository = get(),
            apkInstaller = get(),
            connectionRepository = get()
        )
    }

    factory { FileExplorerScreenModel(sshRepository = get()) }
}

val dataModule = module {
    // Repository instances
    single<MacroRepository> { MacroRepositoryImpl(dataStore = get()) }
}

val databaseModule = module {
    // Room database instance (platform-specific)
}
