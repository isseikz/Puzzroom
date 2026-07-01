package tokyo.isseikuzumaki.vibeterminal.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Android implementation of HapticFeedbackProvider.
 * Uses LocalHapticFeedback for native haptic feedback.
 */
private object AndroidHapticFeedbackProvider : HapticFeedbackProvider {
    @Composable
    override fun getHapticFeedback(): HapticFeedback {
        return LocalHapticFeedback.current
    }
}

actual fun getHapticFeedbackProvider(): HapticFeedbackProvider = AndroidHapticFeedbackProvider
