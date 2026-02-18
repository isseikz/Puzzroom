package tokyo.isseikuzumaki.vibeterminal.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback

/**
 * Platform-agnostic interface for retrieving haptic feedback.
 *
 * Provides access to haptic feedback on platforms that support it (Android),
 * with graceful no-op fallback on other platforms (Desktop, iOS).
 */
interface HapticFeedbackProvider {
    /**
     * Returns the platform-specific HapticFeedback instance.
     * On Android, returns LocalHapticFeedback.current.
     * On other platforms, returns a no-op implementation.
     */
    @Composable
    fun getHapticFeedback(): HapticFeedback
}

/**
 * Expect declaration for platform-specific HapticFeedbackProvider.
 */
expect fun getHapticFeedbackProvider(): HapticFeedbackProvider
