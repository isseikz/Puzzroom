package tokyo.isseikuzumaki.vibeterminal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Vibe Terminal M3 Shape Definitions
 *
 * Following M3 guidelines:
 * - Small: 8dp (buttons, chips)
 * - Medium: 16dp (cards)
 * - Large: 28dp (dialogs, FABs)
 */
val VibeTerminalShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
