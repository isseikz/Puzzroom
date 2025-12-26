package tokyo.isseikuzumaki.vibeterminal.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import tokyo.isseikuzumaki.vibeterminal.service.TerminalService
import tokyo.isseikuzumaki.vibeterminal.util.Logger

/**
 * Android implementation of secondary display launcher.
 *
 * This function:
 * 1. Checks for SYSTEM_ALERT_WINDOW permission
 * 2. Requests permission if not granted
 * 3. Starts TerminalService if permission is granted
 */
actual fun launchSecondaryDisplay(context: Any?) {
    if (context !is Context) {
        Logger.e(null, "launchSecondaryDisplay: context is not Android Context")
        return
    }

    Logger.d("launchSecondaryDisplay: checking permission")

    // Check if SYSTEM_ALERT_WINDOW permission is granted
    if (!Settings.canDrawOverlays(context)) {
        Logger.w("SYSTEM_ALERT_WINDOW permission not granted, requesting...")

        // Open settings to request permission
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
            Logger.d("Opened overlay permission settings")
        } catch (e: Exception) {
            Logger.e(e, "Failed to open overlay permission settings")
        }

        return
    }

    Logger.d("Permission granted, starting TerminalService")

    // Start TerminalService as foreground service
    val serviceIntent = Intent(context, TerminalService::class.java)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.startForegroundService(context, serviceIntent)
    } else {
        context.startService(serviceIntent)
    }

    Logger.d("TerminalService started")
}
