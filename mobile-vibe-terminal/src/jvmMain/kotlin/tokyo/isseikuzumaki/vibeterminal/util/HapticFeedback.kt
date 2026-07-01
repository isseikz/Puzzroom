package tokyo.isseikuzumaki.vibeterminal.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * No-op HapticFeedback implementation for Desktop.
 */
private object NoOpHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        // No-op: Desktop does not support haptic feedback
    }
}

/**
 * Desktop (JVM) implementation of HapticFeedbackProvider.
 * Returns a no-op implementation as desktop platforms don't support haptic feedback.
 */
private object JvmHapticFeedbackProvider : HapticFeedbackProvider {
    @Composable
    override fun getHapticFeedback(): HapticFeedback {
        return NoOpHapticFeedback
    }
}

actual fun getHapticFeedbackProvider(): HapticFeedbackProvider = JvmHapticFeedbackProvider
