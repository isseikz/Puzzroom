package tokyo.isseikuzumaki.puzzroom.ui.atoms

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 落ち着いた暖色系のカラーパレット
 * Warm color palette with calm tones
 */
object AppColors {
    // Primary colors - 暖かいオレンジ系
    val WarmOrange = Color(0xFFE07A5F)
    val WarmOrangeDark = Color(0xFFC85A3F)
    val WarmOrangeLight = Color(0xFFF2A889)
    
    // Secondary colors - 落ち着いたベージュ系
    val WarmBeige = Color(0xFFF4D3B5)
    val WarmBeigeDark = Color(0xFFE8C19E)
    val WarmBeigeLight = Color(0xFFF8E3CB)
    
    // Tertiary colors - アクセント用の暖色
    val WarmTerracotta = Color(0xFFD4A574)
    val WarmTerracottaDark = Color(0xFFB88A5D)
    val WarmTerracottaLight = Color(0xFFE4C09A)
    
    // Background colors
    val WarmBackground = Color(0xFFFFFBF7)
    val WarmSurface = Color(0xFFFFF8F2)
    val WarmSurfaceVariant = Color(0xFFFFF0E5)
    
    // Text colors
    val WarmTextPrimary = Color(0xFF3E2723)
    val WarmTextSecondary = Color(0xFF5D4037)
    val WarmTextTertiary = Color(0xFF795548)
    
    // Status colors
    val WarmSuccess = Color(0xFF81C784)
    val WarmWarning = Color(0xFFFFB74D)
    val WarmError = Color(0xFFE57373)
}

/**
 * Light color scheme with warm tones
 */
val WarmLightColorScheme = lightColorScheme(
    primary = AppColors.WarmOrange,
    onPrimary = Color.White,
    primaryContainer = AppColors.WarmOrangeLight,
    onPrimaryContainer = AppColors.WarmTextPrimary,
    
    secondary = AppColors.WarmBeige,
    onSecondary = AppColors.WarmTextPrimary,
    secondaryContainer = AppColors.WarmBeigeLight,
    onSecondaryContainer = AppColors.WarmTextPrimary,
    
    tertiary = AppColors.WarmTerracotta,
    onTertiary = Color.White,
    tertiaryContainer = AppColors.WarmTerracottaLight,
    onTertiaryContainer = AppColors.WarmTextPrimary,
    
    background = AppColors.WarmBackground,
    onBackground = AppColors.WarmTextPrimary,
    
    surface = AppColors.WarmSurface,
    onSurface = AppColors.WarmTextPrimary,
    surfaceVariant = AppColors.WarmSurfaceVariant,
    onSurfaceVariant = AppColors.WarmTextSecondary,
    
    error = AppColors.WarmError,
    onError = Color.White,
)

/**
 * Dark color scheme with warm tones
 */
val WarmDarkColorScheme = darkColorScheme(
    primary = AppColors.WarmOrangeLight,
    onPrimary = AppColors.WarmTextPrimary,
    primaryContainer = AppColors.WarmOrangeDark,
    onPrimaryContainer = AppColors.WarmBeigeLight,
    
    secondary = AppColors.WarmBeigeDark,
    onSecondary = AppColors.WarmTextPrimary,
    secondaryContainer = AppColors.WarmBeige,
    onSecondaryContainer = Color.White,
    
    tertiary = AppColors.WarmTerracottaLight,
    onTertiary = AppColors.WarmTextPrimary,
    tertiaryContainer = AppColors.WarmTerracottaDark,
    onTertiaryContainer = Color.White,
    
    background = Color(0xFF2C1810),
    onBackground = AppColors.WarmBeigeLight,
    
    surface = Color(0xFF3E2723),
    onSurface = AppColors.WarmBeigeLight,
    surfaceVariant = Color(0xFF4E342E),
    onSurfaceVariant = AppColors.WarmBeige,
    
    error = AppColors.WarmError,
    onError = Color.White,
)
