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
import tokyo.isseikuzumaki.vibeterminal.R
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalStateProvider
import tokyo.isseikuzumaki.vibeterminal.util.Logger

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

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "secondary_display_channel"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_NAME = "Secondary Display"
    }

    // Display listener for monitoring secondary display connection/disconnection
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            Logger.d("Display added: $displayId")
            updatePresentation()

            // **New**: セカンダリディスプレイ接続を通知
            TerminalStateProvider.setSecondaryDisplayConnected(true)
        }

        override fun onDisplayRemoved(displayId: Int) {
            Logger.d("Display removed: $displayId")
            if (presentation?.display?.displayId == displayId) {
                dismissPresentation()

                // **New**: セカンダリディスプレイ切断を通知
                TerminalStateProvider.setSecondaryDisplayConnected(false)
                TerminalStateProvider.clearSecondaryDisplayMetrics()
            }
        }

        override fun onDisplayChanged(displayId: Int) {
            Logger.d("Display changed: $displayId")
            // Optionally handle display configuration changes
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
            Logger.d("TerminalService onCreate completed successfully")
        } catch (e: Exception) {
            Logger.e(e, "Exception in TerminalService onCreate")
            throw e
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            Logger.d("TerminalService onStartCommand")

            // Create notification channel (required for Android 8.0+)
            createNotificationChannel()
            Logger.d("Notification channel created")

            // Start as foreground service with notification
            val notification = createNotification()
            Logger.d("Notification created")

            startForeground(NOTIFICATION_ID, notification)
            Logger.d("Started foreground with notification")

            // Check for existing secondary displays and show presentation
            updatePresentation()
            Logger.d("Presentation updated")

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
            .setSmallIcon(android.R.drawable.ic_dialog_info)  // Use system icon for PoC
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

            // Find secondary displays
            val displays = displayManager.displays
            Logger.d("Found ${displays.size} displays")

            // Look for a secondary display (not the default display)
            val secondaryDisplay = displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }

            if (secondaryDisplay != null) {
                Logger.d("Found secondary display: ${secondaryDisplay.displayId}")
                showPresentation(secondaryDisplay)
            } else {
                Logger.w("No secondary display found")
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
        dismissPresentation()
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
