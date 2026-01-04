package tokyo.isseikuzumaki.vibeterminal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.view.Display
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.vibeterminal.R
import tokyo.isseikuzumaki.vibeterminal.terminal.DisplayTarget
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalDisplayManager
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalStateProvider
import tokyo.isseikuzumaki.vibeterminal.util.Logger

/**
 * Extension function to determine if a Display is a valid secondary display for app UI.
 *
 * This filters out virtual displays created for screen recording (MediaProjection API)
 * or mirroring, which do not have FLAG_PRESENTATION set.
 *
 * @return true if the display is suitable for showing app UI as a secondary display
 */
fun Display.isValidSecondaryDisplay(): Boolean {
    // 1. Exclude the default (built-in) display
    if (displayId == Display.DEFAULT_DISPLAY) return false

    // 2. Check for presentation flag (core logic to exclude screen recording displays)
    // FLAG_PRESENTATION is set on displays intended for external output (HDMI, Miracast, etc.)
    // Virtual displays for screen recording typically have FLAG_PRIVATE instead
    val isPresentation = (flags and Display.FLAG_PRESENTATION) != 0

    return isPresentation
}

/**
 * Foreground Service for maintaining secondary display presentation.
 *
 * This service:
 * 1. Runs as a foreground service to maintain process priority
 * 2. Monitors DisplayManager for secondary display connections
 * 3. Shows TerminalPresentation on detected secondary displays
 * 4. Uses TYPE_APPLICATION_OVERLAY to persist across app switches
 */
class TerminalService : Service() {

    private var presentation: TerminalPresentation? = null
    private lateinit var displayManager: DisplayManager
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "secondary_display_channel"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_NAME = "Secondary Display"
        const val ACTION_STOP = "tokyo.isseikuzumaki.vibeterminal.action.STOP_SERVICE"
    }

    // Display listener for monitoring secondary display connection/disconnection
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            Logger.d("Display added: $displayId")
            // Re-evaluate all displays to determine if a valid secondary display exists
            updateConnectionState()
        }

        override fun onDisplayRemoved(displayId: Int) {
            Logger.d("Display removed: $displayId")
            // Re-evaluate all displays to determine if a valid secondary display still exists
            updateConnectionState()
        }

        override fun onDisplayChanged(displayId: Int) {
            Logger.d("Display changed: $displayId")
            // Re-evaluate in case display flags changed
            updateConnectionState()
        }
    }

    /**
     * Re-evaluate all connected displays and update the connection state.
     *
     * This method checks if any valid secondary display (with FLAG_PRESENTATION) exists,
     * filtering out virtual displays created for screen recording.
     */
    private fun updateConnectionState() {
        val hasValidSecondary = displayManager.displays.any { it.isValidSecondaryDisplay() }
        Logger.d("updateConnectionState: hasValidSecondary=$hasValidSecondary")
        TerminalDisplayManager.setDisplayConnected(hasValidSecondary)

        if (!hasValidSecondary) {
            TerminalStateProvider.clearSecondaryDisplayMetrics()
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Logger.d("TerminalService onCreate")

            displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Register display listener
            displayManager.registerDisplayListener(displayListener, null)

            // Check for existing valid secondary displays (excludes screen recording virtual displays)
            val hasValidSecondary = displayManager.displays.any { it.isValidSecondaryDisplay() }
            TerminalDisplayManager.setDisplayConnected(hasValidSecondary)
            Logger.d("TerminalService onCreate: hasValidSecondary=$hasValidSecondary")

            // Observe terminalDisplayTarget to control presentation
            serviceScope.launch {
                TerminalDisplayManager.terminalDisplayTarget.collect { target ->
                    Logger.d("TerminalService: terminalDisplayTarget changed to $target")
                    when (target) {
                        DisplayTarget.SECONDARY -> {
                            // Show presentation on secondary display
                            updatePresentation()
                        }
                        DisplayTarget.MAIN -> {
                            // Dismiss presentation
                            dismissPresentation()
                        }
                    }
                }
            }

            Logger.d("TerminalService onCreate completed successfully")
        } catch (e: Exception) {
            Logger.e(e, "Exception in TerminalService onCreate")
            throw e
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            Logger.d("TerminalService onStartCommand: action=${intent?.action}")

            // Handle stop action
            if (intent?.action == ACTION_STOP) {
                Logger.d("TerminalService: Received STOP action, stopping service")
                stopSelf()
                return START_NOT_STICKY
            }

            // Create notification channel (required for Android 8.0+)
            createNotificationChannel()
            Logger.d("Notification channel created")

            // Start as foreground service with notification
            val notification = createNotification()
            Logger.d("Notification created")

            startForeground(NOTIFICATION_ID, notification)
            Logger.d("Started foreground with notification")

            // Presentation is now controlled by terminalDisplayTarget observer
            // Initial check is done when observer starts collecting
            Logger.d("Service started, presentation controlled by terminalDisplayTarget")

            return START_STICKY
        } catch (e: Exception) {
            Logger.e(e, "Exception in TerminalService onStartCommand")
            throw e
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Secondary display terminal service"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Secondary Display Active")
            .setContentText("Terminal is displayed on external screen")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updatePresentation() {
        try {
            // If presentation already exists, don't create another
            if (presentation != null) {
                Logger.d("Presentation already exists")
                return
            }

            // Find valid secondary displays (with FLAG_PRESENTATION, excludes screen recording)
            val displays = displayManager.displays
            Logger.d("Found ${displays.size} displays")

            // Look for a valid secondary display (has FLAG_PRESENTATION)
            val secondaryDisplay = displays.firstOrNull { it.isValidSecondaryDisplay() }

            if (secondaryDisplay != null) {
                Logger.d("Found valid secondary display: ${secondaryDisplay.displayId}, flags=${secondaryDisplay.flags}")
                showPresentation(secondaryDisplay)
            } else {
                Logger.w("No valid secondary display found (all displays may be virtual/screen recording)")
            }
        } catch (e: Exception) {
            Logger.e(e, "Exception in updatePresentation")
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
                    // Request terminal resize via TerminalStateProvider
                    TerminalStateProvider.requestResize(cols, rows, widthPx, heightPx)
                }
            )
            Logger.d("TerminalPresentation instance created")

            // Note: Do NOT set window type manually. Presentation API handles this automatically.
            // Setting TYPE_APPLICATION_OVERLAY causes "Window type mismatch" error on Android 12+

            Logger.d("Calling presentation.show()")
            presentation?.show()
            Logger.d("Presentation shown successfully")

            // Request focus restoration to main display after a short delay
            // This ensures the presentation is fully shown before reclaiming focus
            serviceScope.launch {
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

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("TerminalService onDestroy")
        serviceScope.cancel()
        dismissPresentation()
        displayManager.unregisterDisplayListener(displayListener)
        // Reset display connection state when service is destroyed
        TerminalDisplayManager.setDisplayConnected(false)
        TerminalStateProvider.clearSecondaryDisplayMetrics()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
