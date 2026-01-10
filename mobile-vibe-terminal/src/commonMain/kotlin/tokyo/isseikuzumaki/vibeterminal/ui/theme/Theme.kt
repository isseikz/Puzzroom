package tokyo.isseikuzumaki.vibeterminal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Vibe Terminal M3 Dark Color Scheme
 *
 * Designed for terminal applications with:
 * - Deep charcoal backgrounds (no pure black)
 * - Softer terminal green primary color
 * - Proper elevation hierarchy via surface variants
 */
private val VibeTerminalDarkColorScheme = darkColorScheme(
    // Primary
    primary = VtPrimary,
    onPrimary = VtOnPrimary,
    primaryContainer = VtPrimaryContainer,
    onPrimaryContainer = VtOnPrimaryContainer,

    // Secondary
    secondary = VtSecondary,
    onSecondary = VtOnSecondary,
    secondaryContainer = VtSecondaryContainer,
    onSecondaryContainer = VtOnSecondaryContainer,

    // Tertiary
    tertiary = VtTertiary,
    onTertiary = VtOnTertiary,
    tertiaryContainer = VtTertiaryContainer,
    onTertiaryContainer = VtOnTertiaryContainer,

    // Background & Surface
    background = VtBackground,
    onBackground = VtOnSurface,
    surface = VtSurface,
    onSurface = VtOnSurface,
    surfaceVariant = VtSurfaceVariant,
    onSurfaceVariant = VtOnSurfaceVariant,
    surfaceContainerLowest = VtBackground,
    surfaceContainerLow = VtSurfaceContainer,
    surfaceContainer = VtSurface,
    surfaceContainerHigh = VtSurfaceContainerHigh,
    surfaceContainerHighest = VtSurfaceContainerHigh,

    // Error
    error = VtError,
    onError = VtOnError,
    errorContainer = VtErrorContainer,
    onErrorContainer = VtOnErrorContainer,

    // Outline
    outline = VtOutline,
    outlineVariant = VtOutlineVariant,

    // Inverse
    inverseSurface = VtInverseSurface,
    inverseOnSurface = VtInverseOnSurface,
    inversePrimary = VtInversePrimary,

    // Scrim
    scrim = VtScrim
)

/**
 * Vibe Terminal Theme
 *
 * Wraps the app content with M3 MaterialTheme using the Vibe Terminal color scheme.
 * Apply this at the root of the app to enable consistent theming throughout.
 */
@Composable
fun VibeTerminalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VibeTerminalDarkColorScheme,
        shapes = VibeTerminalShapes,
        content = content
    )
}
