package tokyo.isseikuzumaki.vibeterminal.service

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalStateProvider
import tokyo.isseikuzumaki.vibeterminal.ui.components.TerminalCanvas
import tokyo.isseikuzumaki.vibeterminal.util.Logger

/**
 * Presentation for secondary display.
 *
 * Displays the terminal output on the secondary display,
 * synchronized with the main terminal session.
 *
 * Uses Jetpack Compose to render the terminal UI using the same
 * TerminalCanvas component as the main display.
 *
 * Implements LifecycleOwner and SavedStateRegistryOwner to support
 * ComposeView which requires these for proper Compose lifecycle management.
 *
 * @param onDisplaySizeCalculated Callback invoked when display size is calculated
 */
class TerminalPresentation(
    outerContext: Context,
    display: Display,
    private val onDisplaySizeCalculated: (cols: Int, rows: Int, widthPx: Int, heightPx: Int) -> Unit
) : Presentation(outerContext, display), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Logger.d("TerminalPresentation onCreate")

            // Initialize SavedStateRegistry
            savedStateRegistryController.performRestore(savedInstanceState)

            // Calculate display size for terminal
            calculateAndNotifyDisplaySize()

            // Create ComposeView for Compose UI
            val composeView = ComposeView(context).apply {
                // Set lifecycle and saved state owners BEFORE setContent
                setViewTreeLifecycleOwner(this@TerminalPresentation)
                setViewTreeSavedStateRegistryOwner(this@TerminalPresentation)

                // Set composition strategy to dispose on detach
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

                setContent {
                    SecondaryTerminalDisplay()
                }
                Logger.d("ComposeView content set successfully")
            }

            setContentView(composeView)

            // Move lifecycle to CREATED state
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            Logger.d("Presentation content view set (Compose), lifecycle: CREATED")
        } catch (e: Exception) {
            Logger.e(e, "Exception in TerminalPresentation onCreate")
            throw e
        }
    }

    /**
     * Calculate terminal size based on secondary display dimensions.
     * Uses the same font size as TerminalCanvas (14sp).
     */
    private fun calculateAndNotifyDisplaySize() {
        try {
            // Get display metrics
            val displayMetrics = context.resources.displayMetrics
            val widthPx = displayMetrics.widthPixels
            val heightPx = displayMetrics.heightPixels

            Logger.d("Secondary display size: ${widthPx}x${heightPx}px")

            // Calculate character size using same logic as TerminalCanvas
            // Font size: 14sp
            val fontSizeSp = 14f
            val density = displayMetrics.density
            val fontSizePx = fontSizeSp * density

            // Approximate char dimensions (monospace font)
            // charWidth ≈ fontSizePx * 0.6 (typical monospace ratio)
            // charHeight ≈ fontSizePx * 1.2 (line height)
            val charWidth = (fontSizePx * 0.6f).toInt()
            val charHeight = (fontSizePx * 1.2f).toInt()

            Logger.d("Character size: ${charWidth}x${charHeight}px (font: ${fontSizePx}px)")

            // Calculate terminal dimensions
            val cols = (widthPx / charWidth).coerceAtLeast(1)
            val rows = (heightPx / charHeight).coerceAtLeast(1)

            Logger.d("Calculated terminal size: ${cols} cols x ${rows} rows")

            // **New**: TerminalStateProvider にセカンダリディスプレイのサイズを保存
            TerminalStateProvider.setSecondaryDisplayMetrics(cols, rows, widthPx, heightPx)

            // Notify callback
            onDisplaySizeCalculated(cols, rows, widthPx, heightPx)
        } catch (e: Exception) {
            Logger.e(e, "Failed to calculate display size")
        }
    }

    @Composable
    private fun SecondaryTerminalDisplay() {
        val terminalState by TerminalStateProvider.state.collectAsState()

        // Log state changes
        LaunchedEffect(terminalState.isConnected, terminalState.buffer.size) {
            try {
                Logger.d("Terminal state: isConnected=${terminalState.isConnected}, bufferSize=${terminalState.buffer.size}")
                if (terminalState.buffer.isNotEmpty()) {
                    Logger.d("Buffer dimensions: ${terminalState.buffer.size}x${terminalState.buffer.firstOrNull()?.size ?: 0}")
                }
            } catch (e: Exception) {
                Logger.e(e, "Error logging terminal state")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (terminalState.isConnected && terminalState.buffer.isNotEmpty()) {
                // Force recomposition when buffer updates using key()
                key(terminalState.bufferUpdateCounter) {
                    // Render terminal output
                    TerminalCanvas(
                        buffer = terminalState.buffer,
                        cursorRow = terminalState.cursorRow,
                        cursorCol = terminalState.cursorCol,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Show waiting message when not connected
                Text(
                    text = "Waiting for terminal connection...\n\nConnect to SSH server on the main display\nto see terminal output here.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            Logger.d("TerminalPresentation onStart, lifecycle: STARTED")
        } catch (e: Exception) {
            Logger.e(e, "Exception in TerminalPresentation onStart")
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            Logger.d("TerminalPresentation onStop, lifecycle: CREATED")
        } catch (e: Exception) {
            Logger.e(e, "Exception in TerminalPresentation onStop")
        }
    }

    override fun dismiss() {
        try {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            Logger.d("TerminalPresentation dismiss, lifecycle: DESTROYED")
        } catch (e: Exception) {
            Logger.e(e, "Exception in TerminalPresentation dismiss")
        }
        super.dismiss()
    }
}
