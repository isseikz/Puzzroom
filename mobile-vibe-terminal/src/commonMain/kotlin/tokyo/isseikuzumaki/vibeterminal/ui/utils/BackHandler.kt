package tokyo.isseikuzumaki.vibeterminal.ui.utils

import androidx.compose.runtime.Composable

/**
 * Common BackHandler for KMP.
 * On Android, it uses [androidx.activity.compose.BackHandler].
 * On other platforms, it's currently a placeholder or platform-specific implementation.
 */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
