package tokyo.isseikuzumaki.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WarmColorScheme = lightColorScheme(
    primary = WarmPrimary,
    onPrimary = WarmOnPrimary,
    primaryContainer = WarmPrimaryVariant,
    onPrimaryContainer = WarmOnPrimary,
    
    secondary = WarmSecondary,
    onSecondary = WarmOnSecondary,
    secondaryContainer = WarmSecondaryVariant,
    onSecondaryContainer = WarmOnSecondary,
    
    tertiary = WarmTertiary,
    onTertiary = WarmOnTertiary,
    tertiaryContainer = WarmTertiaryVariant,
    onTertiaryContainer = WarmOnTertiary,
    
    background = WarmBackground,
    onBackground = WarmOnBackground,
    
    surface = WarmSurface,
    onSurface = WarmOnSurface,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = WarmOnSurfaceVariant,
    
    error = WarmError,
    onError = WarmOnError,
    
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WarmColorScheme,
        typography = Typography,
        content = content
    )
}
