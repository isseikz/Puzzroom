package tokyo.isseikuzumaki.vibeterminal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific banner ad composable.
 * On Android, this displays an AdMob banner ad.
 * On other platforms, this is a no-op (empty composable).
 */
@Composable
expect fun PlatformAdBanner(modifier: Modifier = Modifier)
