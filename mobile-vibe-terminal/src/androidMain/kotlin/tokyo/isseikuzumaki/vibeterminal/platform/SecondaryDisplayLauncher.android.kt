package tokyo.isseikuzumaki.vibeterminal.platform

import tokyo.isseikuzumaki.vibeterminal.util.Logger

/**
 * Android implementation - now a no-op since secondary display is managed by MainActivity lifecycle.
 *
 * Previously, this started a ForegroundService to maintain secondary display presentation
 * even when the app was in background. The new implementation dismisses the presentation
 * when the app goes to background, eliminating the need for a foreground service.
 */
actual fun stopSecondaryDisplay(context: Any?) {
    Logger.d("stopSecondaryDisplay: no-op (service removed)")
}

/**
 * Android implementation - now a no-op since secondary display is managed by MainActivity lifecycle.
 *
 * Secondary display monitoring and presentation is now handled in MainActivity's
 * onResume/onPause lifecycle methods.
 */
actual fun launchSecondaryDisplay(context: Any?) {
    Logger.d("launchSecondaryDisplay: no-op (managed by MainActivity lifecycle)")
}
