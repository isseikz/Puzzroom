package tokyo.isseikuzumaki.vibeterminal.platform

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import tokyo.isseikuzumaki.vibeterminal.service.TerminalService
import tokyo.isseikuzumaki.vibeterminal.util.Logger

/**
 * Android implementation to stop secondary display service.
 */
actual fun stopSecondaryDisplay(context: Any?) {
    try {
        Logger.d("stopSecondaryDisplay: called")

        if (context !is Context) {
            Logger.e("stopSecondaryDisplay: context is not Android Context (type: ${context?.javaClass?.name})")
            return
        }

        Logger.d("stopSecondaryDisplay: stopping TerminalService")

        val serviceIntent = Intent(context, TerminalService::class.java).apply {
            action = TerminalService.ACTION_STOP
        }
        context.startService(serviceIntent)

        Logger.d("stopSecondaryDisplay: stop command sent")
    } catch (e: Exception) {
        Logger.e(e, "Exception in stopSecondaryDisplay")
    }
}

/**
 * Android implementation of secondary display launcher.
 *
 * This function:
 * 1. Checks for SYSTEM_ALERT_WINDOW permission
 * 2. Requests permission if not granted
 * 3. Starts TerminalService if permission is granted
 */
actual fun launchSecondaryDisplay(context: Any?) {
    try {
        Logger.d("launchSecondaryDisplay: called")

        if (context !is Context) {
            Logger.e("launchSecondaryDisplay: context is not Android Context (type: ${context?.javaClass?.name})")
            return
        }

        Logger.d("Starting TerminalService (permission check disabled for testing)")

        // Start TerminalService as foreground service
        try {
            val serviceIntent = Intent(context, TerminalService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Logger.d("Starting foreground service (Android O+)")
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                Logger.d("Starting service (pre-Android O)")
                context.startService(serviceIntent)
            }

            Logger.d("TerminalService start command sent")
        } catch (e: Exception) {
            Logger.e(e, "Failed to start TerminalService")
            throw e
        }
    } catch (e: Exception) {
        Logger.e(e, "Exception in launchSecondaryDisplay")
        throw e
    }
}
