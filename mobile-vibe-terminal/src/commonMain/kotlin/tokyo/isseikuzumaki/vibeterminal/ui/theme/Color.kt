package tokyo.isseikuzumaki.vibeterminal.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Vibe Terminal M3 Dark Color Palette
 *
 * Design Principles:
 * - Avoid pure black (#000000) for backgrounds to enable depth/elevation
 * - Use softer green instead of harsh neon lime (#00FF00)
 * - Consistent 8dp grid system for spacing
 */

// Background (deep charcoal, NOT pure black)
val VtBackground = Color(0xFF121212)

// Surface colors (slightly lighter for elevation hierarchy)
val VtSurface = Color(0xFF1E1E1E)
val VtSurfaceVariant = Color(0xFF252525)
val VtSurfaceContainer = Color(0xFF1A1A1A)
val VtSurfaceContainerHigh = Color(0xFF2A2A2A)

// Primary (adjusted terminal green - softer and easier on eyes)
val VtPrimary = Color(0xFF4CAF50)
val VtPrimaryContainer = Color(0xFF1B5E20)
val VtOnPrimary = Color(0xFF000000)
val VtOnPrimaryContainer = Color(0xFFA5D6A7)

// Secondary (muted teal for accents)
val VtSecondary = Color(0xFF80CBC4)
val VtSecondaryContainer = Color(0xFF00695C)
val VtOnSecondary = Color(0xFF000000)
val VtOnSecondaryContainer = Color(0xFFB2DFDB)

// Tertiary (amber for warnings/highlights)
val VtTertiary = Color(0xFFFFB74D)
val VtTertiaryContainer = Color(0xFFE65100)
val VtOnTertiary = Color(0xFF000000)
val VtOnTertiaryContainer = Color(0xFFFFE0B2)

// On Surface (text colors)
val VtOnSurface = Color(0xFFE0E0E0)
val VtOnSurfaceVariant = Color(0xFF9E9E9E)

// Error
val VtError = Color(0xFFCF6679)
val VtErrorContainer = Color(0xFFB00020)
val VtOnError = Color(0xFF000000)
val VtOnErrorContainer = Color(0xFFFFDAD6)

// Outline
val VtOutline = Color(0xFF5C5C5C)
val VtOutlineVariant = Color(0xFF3C3C3C)

// Inverse
val VtInverseSurface = Color(0xFFE0E0E0)
val VtInverseOnSurface = Color(0xFF121212)
val VtInversePrimary = Color(0xFF1B5E20)

// Scrim
val VtScrim = Color(0xFF000000)
