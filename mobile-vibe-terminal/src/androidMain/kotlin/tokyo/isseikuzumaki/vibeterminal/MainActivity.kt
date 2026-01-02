package tokyo.isseikuzumaki.vibeterminal

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cafe.adriel.voyager.navigator.Navigator
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import tokyo.isseikuzumaki.vibeterminal.di.appModule
import tokyo.isseikuzumaki.vibeterminal.di.dataModule
import tokyo.isseikuzumaki.vibeterminal.di.platformModule
import tokyo.isseikuzumaki.vibeterminal.input.HardwareKeyboardHandler
import tokyo.isseikuzumaki.vibeterminal.platform.launchSecondaryDisplay
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalStateProvider
import tokyo.isseikuzumaki.vibeterminal.ui.components.TriggerEventHost
import tokyo.isseikuzumaki.vibeterminal.ui.screens.ConnectionListScreen
import tokyo.isseikuzumaki.vibeterminal.util.Logger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Apache SSHD for Android (must be done before any SSHD classes are loaded)
        org.apache.sshd.common.util.io.PathUtils.setUserHomeFolderResolver {
            // Use app's files directory as fake home folder
            filesDir.toPath()
        }

        // Initialize Koin only if not already started
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@MainActivity)
                modules(appModule, dataModule, platformModule())
            }
        }

        // Start TerminalService to monitor secondary display connections
        try {
            launchSecondaryDisplay(this)
            Logger.d("MainActivity: TerminalService started for display monitoring")
        } catch (e: Exception) {
            Logger.e(e, "MainActivity: Failed to start TerminalService")
        }

        enableEdgeToEdge()
        setContent {
            TriggerEventHost {
                Navigator(ConnectionListScreen())
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Only intercept in command mode when terminal is connected
        val isCommandMode = TerminalStateProvider.isCommandMode.value
        val isConnected = TerminalStateProvider.state.value.isConnected

        if (isCommandMode && isConnected) {
            val result = HardwareKeyboardHandler.processKeyEvent(event, isCommandMode)
            when (result) {
                is HardwareKeyboardHandler.KeyResult.Handled -> {
                    Logger.d("MainActivity: Sending key sequence to terminal: '${result.sequence}'")
                    TerminalStateProvider.sendHardwareKeyboardInput(result.sequence)
                    return true
                }
                is HardwareKeyboardHandler.KeyResult.Ignored -> {
                    // Modifier key only, let system handle
                    return super.dispatchKeyEvent(event)
                }
                is HardwareKeyboardHandler.KeyResult.PassThrough -> {
                    // Unknown key, let system handle
                    return super.dispatchKeyEvent(event)
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }
}
