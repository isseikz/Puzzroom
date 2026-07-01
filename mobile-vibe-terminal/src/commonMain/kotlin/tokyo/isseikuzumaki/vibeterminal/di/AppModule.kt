package tokyo.isseikuzumaki.vibeterminal.di

import org.koin.dsl.module
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

    factory {
        FileExplorerScreenModel(
            sshRepository = get(),
            fileDownloader = get(),
            fileSharer = get()
        )
    }
}

val dataModule = module {
    // Repository instances
}

val databaseModule = module {
    // Room database instance (platform-specific)
}
