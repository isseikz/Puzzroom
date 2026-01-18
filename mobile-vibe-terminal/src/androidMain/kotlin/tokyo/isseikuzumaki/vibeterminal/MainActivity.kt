package tokyo.isseikuzumaki.vibeterminal

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import tokyo.isseikuzumaki.vibeterminal.di.appModule
import tokyo.isseikuzumaki.vibeterminal.di.dataModule
import tokyo.isseikuzumaki.vibeterminal.di.platformModule
import tokyo.isseikuzumaki.vibeterminal.input.HardwareKeyboardHandler
import tokyo.isseikuzumaki.vibeterminal.service.TerminalPresentation
import tokyo.isseikuzumaki.vibeterminal.service.isValidSecondaryDisplay
import tokyo.isseikuzumaki.vibeterminal.terminal.DisplayTarget
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalDisplayManager
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalStateProvider
import tokyo.isseikuzumaki.vibeterminal.ui.components.TriggerEventHost
import tokyo.isseikuzumaki.vibeterminal.ui.screens.ConnectionListScreen
import tokyo.isseikuzumaki.vibeterminal.ui.theme.VibeTerminalTheme
import tokyo.isseikuzumaki.vibeterminal.util.Logger
import android.widget.Toast

class MainActivity : ComponentActivity() {

    private var wasHardwareKeyboardConnected = false

    private var presentation: TerminalPresentation? = null
    private lateinit var displayManager: DisplayManager
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isResumed = false

    // Display listener for monitoring secondary display connection/disconnection
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            Logger.d("Display added: $displayId")
            updateConnectionState()
        }

        override fun onDisplayRemoved(displayId: Int) {
            Logger.d("Display removed: $displayId")
            updateConnectionState()
        }

        override fun onDisplayChanged(displayId: Int) {
            Logger.d("Display changed: $displayId")
            updateConnectionState()
        }
    }

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

        // Initialize DisplayManager
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

        // Check initial hardware keyboard state
        updateHardwareKeyboardState(resources.configuration)

        // Observe terminalDisplayTarget to control presentation
        activityScope.launch {
            TerminalDisplayManager.terminalDisplayTarget.collect { target ->
                Logger.d("MainActivity: terminalDisplayTarget changed to $target")
                if (isResumed) {
                    when (target) {
                        DisplayTarget.SECONDARY -> updatePresentation()
                        DisplayTarget.MAIN -> dismissPresentation()
                    }
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            VibeTerminalTheme {
                TriggerEventHost {
                    Navigator(ConnectionListScreen())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        Logger.d("MainActivity: onResume - registering display listener")

        // Register display listener
        displayManager.registerDisplayListener(displayListener, null)

        // Check for existing valid secondary displays
        updateConnectionState()

        // Show presentation if target is secondary
        if (TerminalDisplayManager.terminalDisplayTarget.value == DisplayTarget.SECONDARY) {
            updatePresentation()
        }
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
        Logger.d("MainActivity: onPause - unregistering display listener and dismissing presentation")

        // Dismiss presentation when app goes to background
        dismissPresentation()

        // Unregister display listener
        displayManager.unregisterDisplayListener(displayListener)

        // Reset display connection state
        TerminalDisplayManager.setDisplayConnected(false)
        TerminalStateProvider.clearSecondaryDisplayMetrics()
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("MainActivity: onDestroy")
        activityScope.cancel()
    }

    private fun updateConnectionState() {
        val hasValidSecondary = displayManager.displays.any { it.isValidSecondaryDisplay() }
        Logger.d("updateConnectionState: hasValidSecondary=$hasValidSecondary")
        TerminalDisplayManager.setDisplayConnected(hasValidSecondary)

        if (!hasValidSecondary) {
            TerminalStateProvider.clearSecondaryDisplayMetrics()
        }
    }

    private fun updatePresentation() {
        if (!isResumed) {
            Logger.d("updatePresentation: Activity not resumed, skipping")
            return
        }

        if (presentation != null) {
            Logger.d("Presentation already exists")
            return
        }

        val displays = displayManager.displays
        Logger.d("Found ${displays.size} displays")

        val secondaryDisplay = displays.firstOrNull { it.isValidSecondaryDisplay() }

        if (secondaryDisplay != null) {
            Logger.d("Found valid secondary display: ${secondaryDisplay.displayId}, flags=${secondaryDisplay.flags}")
            showPresentation(secondaryDisplay)
        } else {
            Logger.w("No valid secondary display found")
        }
    }

    private fun showPresentation(display: Display) {
        try {
            Logger.d("Creating presentation for display ${display.displayId}")

            presentation = TerminalPresentation(
                outerContext = this,
                display = display,
                onDisplaySizeCalculated = { cols, rows, widthPx, heightPx ->
                    Logger.d("Secondary display size calculated: ${cols}x${rows} (${widthPx}x${heightPx}px)")
                    TerminalStateProvider.requestResize(cols, rows, widthPx, heightPx)
                }
            )

            presentation?.show()
            Logger.d("Presentation shown successfully")

            // Request focus restoration to main display after a short delay
            activityScope.launch {
                delay(200)
                Logger.d("Requesting focus restoration to main display")
                TerminalDisplayManager.requestFocusRestoration()
            }
        } catch (e: WindowManager.InvalidDisplayException) {
            Logger.e(e, "InvalidDisplayException when showing presentation")
            presentation = null
        } catch (e: Exception) {
            Logger.e(e, "Exception in showPresentation")
            presentation = null
        }
    }

    private fun dismissPresentation() {
        Logger.d("Dismissing presentation")
        presentation?.dismiss()
        presentation = null
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isConnected = TerminalStateProvider.state.value.isConnected

        // Process key events when terminal is connected
        if (isConnected) {
            val isCommandMode = TerminalStateProvider.isCommandMode.value
            val result = HardwareKeyboardHandler.processKeyEvent(event, isCommandMode)

            when (result) {
                is HardwareKeyboardHandler.KeyResult.ToggleIme -> {
                    Logger.d("MainActivity: Alt+i pressed, requesting IME toggle")
                    TerminalStateProvider.requestToggleIme()
                    return true
                }
                is HardwareKeyboardHandler.KeyResult.Handled -> {
                    if (isCommandMode) {
                        Logger.d("MainActivity: Sending key sequence to terminal: '${result.sequence}'")
                        TerminalStateProvider.sendHardwareKeyboardInput(result.sequence)
                        return true
                    }
                    // In IME mode, only special keys are handled, regular chars pass through
                    return super.dispatchKeyEvent(event)
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateHardwareKeyboardState(newConfig)
    }

    private fun updateHardwareKeyboardState(config: Configuration) {
        val hasHardwareKeyboard = config.keyboard != Configuration.KEYBOARD_NOKEYS
        Logger.d("MainActivity: Hardware keyboard connected: $hasHardwareKeyboard (keyboard type: ${config.keyboard})")

        // Show Toast when keyboard is disconnected
        if (wasHardwareKeyboardConnected && !hasHardwareKeyboard) {
            Toast.makeText(this, "キーボードが切断されました", Toast.LENGTH_SHORT).show()
        }

        wasHardwareKeyboardConnected = hasHardwareKeyboard
        TerminalStateProvider.setHardwareKeyboardConnected(hasHardwareKeyboard)
    }
}
