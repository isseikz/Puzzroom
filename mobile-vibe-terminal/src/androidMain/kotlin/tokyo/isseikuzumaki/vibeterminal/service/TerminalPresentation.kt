package tokyo.isseikuzumaki.vibeterminal.service

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.focus.focusProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.isseikz.kmpinput.TerminalInputContainer
import io.github.isseikz.kmpinput.rememberTerminalInputContainerState
import tokyo.isseikuzumaki.vibeterminal.terminal.DisplayTarget
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalDisplayManager
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalSizeCalculator
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

            // Allow focus on secondary display to capture keyboard input
            // Input will be forwarded to main terminal via TerminalStateProvider
            Logger.d("Window is focusable for keyboard input capture")

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
     * Uses conservative estimates to ensure terminal fits within display.
     * Actual precise calculation is done in Composable using TextMeasurer.
     */
    private fun calculateAndNotifyDisplaySize() {
        try {
            // Get display metrics
            val displayMetrics = context.resources.displayMetrics
            val widthPx = displayMetrics.widthPixels
            val heightPx = displayMetrics.heightPixels

            Logger.d("Secondary display size: ${widthPx}x${heightPx}px")

            // Use conservative estimates for initial calculation
            // This ensures terminal fits even before TextMeasurer provides exact values
            val fontSizeSp = TERMINAL_FONT_SIZE_SP
            val density = displayMetrics.density

            val dimensions = TerminalSizeCalculator.calculateWithEstimatedCharSize(
                displayWidthPx = widthPx,
                displayHeightPx = heightPx,
                fontSizeSp = fontSizeSp,
                density = density
            )

            Logger.d("Estimated character size: ${dimensions.actualCharWidth}x${dimensions.actualCharHeight}px")
            Logger.d("Calculated terminal size: ${dimensions.cols} cols x ${dimensions.rows} rows")

            // Store secondary display metrics
            TerminalStateProvider.setSecondaryDisplayMetrics(
                dimensions.cols,
                dimensions.rows,
                widthPx,
                heightPx
            )

            // Notify callback
            onDisplaySizeCalculated(dimensions.cols, dimensions.rows, widthPx, heightPx)
        } catch (e: Exception) {
            Logger.e(e, "Failed to calculate display size")
        }
    }

    companion object {
        const val TERMINAL_FONT_SIZE_SP = 14f
    }

    @Composable
    private fun SecondaryTerminalDisplay() {
        val terminalState by TerminalStateProvider.state.collectAsState()
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()

        // TerminalInputContainer state for capturing keyboard input
        val terminalInputState = rememberTerminalInputContainerState()

        // Collect input from TerminalInputContainer and forward to main terminal
        LaunchedEffect(terminalInputState.isReady) {
            if (terminalInputState.isReady) {
                Logger.d("SecondaryDisplay: TerminalInputContainer is ready, collecting ptyInputStream")
                terminalInputState.ptyInputStream.collect { bytes ->
                    val text = bytes.decodeToString()
                    Logger.d("SecondaryDisplay: Received input: '$text'")
                    TerminalStateProvider.sendInputFromSecondaryDisplay(text)
                }
            }
        }

        // Measure actual character dimensions using TextMeasurer
        val textStyle = remember {
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = TERMINAL_FONT_SIZE_SP.sp
            )
        }

        val charDimensions = remember(textMeasurer, textStyle) {
            val sampleLayout = textMeasurer.measure(text = "W", style = textStyle)
            Pair(sampleLayout.size.width.toFloat(), sampleLayout.size.height.toFloat())
        }

        // Get display metrics for accurate size calculation
        val displayMetrics = context.resources.displayMetrics
        val displayWidthPx = displayMetrics.widthPixels
        val displayHeightPx = displayMetrics.heightPixels

        // Calculate accurate terminal dimensions based on measured character size
        val accurateDimensions = remember(charDimensions, displayWidthPx, displayHeightPx) {
            TerminalSizeCalculator.calculate(
                displayWidthPx = displayWidthPx,
                displayHeightPx = displayHeightPx,
                charWidth = charDimensions.first,
                charHeight = charDimensions.second
            )
        }

        // Watch display target to only request resize when secondary display is active
        val displayTarget by TerminalDisplayManager.terminalDisplayTarget.collectAsState()

        // Request resize if dimensions differ from current buffer
        // Only when secondary display is the active target
        LaunchedEffect(accurateDimensions, terminalState.buffer.size, displayTarget) {
            // Only request resize when secondary display is the active display target
            if (displayTarget != DisplayTarget.SECONDARY) {
                Logger.d("SecondaryDisplay: Skipping resize request because displayTarget=$displayTarget (not SECONDARY)")
                return@LaunchedEffect
            }

            val currentRows = terminalState.buffer.size
            val currentCols = terminalState.buffer.firstOrNull()?.size ?: 0

            if (currentRows > 0 && currentCols > 0) {
                // Only resize if current buffer exceeds accurate dimensions (would clip)
                if (currentCols > accurateDimensions.cols || currentRows > accurateDimensions.rows) {
                    Logger.d("Buffer size ($currentCols x $currentRows) exceeds accurate dimensions (${accurateDimensions.cols} x ${accurateDimensions.rows}), requesting resize")
                    TerminalStateProvider.setSecondaryDisplayMetrics(
                        accurateDimensions.cols,
                        accurateDimensions.rows,
                        displayWidthPx,
                        displayHeightPx
                    )
                    TerminalStateProvider.requestResize(
                        accurateDimensions.cols,
                        accurateDimensions.rows,
                        displayWidthPx,
                        displayHeightPx
                    )
                }
            }

            Logger.d("Terminal state: isConnected=${terminalState.isConnected}, bufferSize=${terminalState.buffer.size}")
            Logger.d("Accurate dimensions: ${accurateDimensions.cols} x ${accurateDimensions.rows} (charSize: ${charDimensions.first} x ${charDimensions.second})")
        }

        // Wrap entire display in TerminalInputContainer to capture keyboard input
        TerminalInputContainer(
            state = terminalInputState,
            modifier = Modifier.fillMaxSize()
        ) {
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
                        text = "Waiting for terminal connection...\n\nConnect to SSH server on the main display\nto see terminal output here.\n\nTap here to enable keyboard input.",
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
