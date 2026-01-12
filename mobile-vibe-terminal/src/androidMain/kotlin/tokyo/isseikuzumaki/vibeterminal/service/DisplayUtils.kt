package tokyo.isseikuzumaki.vibeterminal.service

import android.view.Display

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
