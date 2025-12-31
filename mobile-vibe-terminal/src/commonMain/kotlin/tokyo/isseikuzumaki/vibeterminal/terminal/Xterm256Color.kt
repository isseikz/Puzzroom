package tokyo.isseikuzumaki.vibeterminal.terminal

import androidx.compose.ui.graphics.Color

/**
 * Utility to convert Xterm 256 color index to Color.
 */
object Xterm256Color {
    
    // Cache for calculated colors
    private val colorCache =  HashMap<Int, Color>()

    fun getColor(index: Int): Color {
        if (index in colorCache) return colorCache[index]!!

        val color = when (index) {
            // 0-15: Standard Colors
            in 0..15 -> getStandardColor(index)
            // 16-231: 6x6x6 Color Cube
            in 16..231 -> getColorCube(index)
            // 232-255: Grayscale Ramp
            in 232..255 -> getGrayscale(index)
            else -> Color.White // Fallback
        }
        
        colorCache[index] = color
        return color
    }

    private fun getStandardColor(index: Int): Color {
        return when (index) {
            // Standard (ANSI)
            0 -> Color.Black
            1 -> Color(0xFFFF5555) // Red
            2 -> Color(0xFF50FA7B) // Green
            3 -> Color(0xFFF1FA8C) // Yellow
            4 -> Color(0xFFBD93F9) // Blue
            5 -> Color(0xFFFF79C6) // Magenta
            6 -> Color(0xFF8BE9FD) // Cyan
            7 -> Color(0xFFBFBFBF) // Light Gray
            // Bright (AIXTerm)
            8 -> Color(0xFF4D4D4D) // Dark Gray
            9 -> Color(0xFFFF6E6E) // Bright Red
            10 -> Color(0xFF69FF94) // Bright Green
            11 -> Color(0xFFFFFFA5) // Bright Yellow
            12 -> Color(0xFFD6ACFF) // Bright Blue
            13 -> Color(0xFFFF92DF) // Bright Magenta
            14 -> Color(0xFFA4FFFF) // Bright Cyan
            15 -> Color.White      // White
            else -> Color.White
        }
    }

    private fun getColorCube(index: Int): Color {
        // Index 16..231
        // Formula: 16 + (36 * r) + (6 * g) + b
        val baseIndex = index - 16
        val r = baseIndex / 36
        val g = (baseIndex % 36) / 6
        val b = baseIndex % 6

        // Map 0..5 to 0..255
        // Steps: 0, 95, 135, 175, 215, 255
        fun mapValue(v: Int): Int {
            return if (v == 0) 0 else (v * 40 + 55)
        }

        return Color(mapValue(r), mapValue(g), mapValue(b))
    }

    private fun getGrayscale(index: Int): Color {
        // Index 232..255
        // Map to 24 shades of gray from 8 to 238
        val baseIndex = index - 232
        val value = baseIndex * 10 + 8
        return Color(value, value, value)
    }
}
