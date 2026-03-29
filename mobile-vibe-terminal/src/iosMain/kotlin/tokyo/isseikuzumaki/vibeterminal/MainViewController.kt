package tokyo.isseikuzumaki.vibeterminal

import androidx.compose.ui.window.ComposeUIViewController
import cafe.adriel.voyager.navigator.Navigator
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools
import tokyo.isseikuzumaki.vibeterminal.di.appModule
import tokyo.isseikuzumaki.vibeterminal.di.dataModule
import tokyo.isseikuzumaki.vibeterminal.di.platformModule
import tokyo.isseikuzumaki.vibeterminal.ui.screens.ConnectionListScreen
import tokyo.isseikuzumaki.vibeterminal.ui.theme.VibeTerminalTheme

fun MainViewController() = ComposeUIViewController {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin { modules(appModule, dataModule, platformModule()) }
    }
    VibeTerminalTheme {
        Navigator(ConnectionListScreen())
    }
}
