package tokyo.isseikuzumaki.vibeterminal.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * No-op HapticFeedback implementation for iOS.
 * Future: Can integrate UIImpactFeedbackGenerator.
 */
private object NoOpHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        // No-op: iOS haptic feedback not yet implemented
        // TODO: Integrate UIImpactFeedbackGenerator in future version
    }
}

/**
 * iOS implementation of HapticFeedbackProvider.
 * Returns a no-op implementation for initial release.
 */
private object IosHapticFeedbackProvider : HapticFeedbackProvider {
    @Composable
    override fun getHapticFeedback(): HapticFeedback {
        return NoOpHapticFeedback
    }
}

actual fun getHapticFeedbackProvider(): HapticFeedbackProvider = IosHapticFeedbackProvider
