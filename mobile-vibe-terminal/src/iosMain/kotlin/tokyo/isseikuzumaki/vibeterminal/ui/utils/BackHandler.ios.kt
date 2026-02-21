package tokyo.isseikuzumaki.vibeterminal.ui.utils

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS swipe back usually handled by navigator
}
