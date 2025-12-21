package tokyo.isseikuzumaki.vibeterminal.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/**
 * ANSI color parser for terminal output
 * Supports basic 16 colors and 256-color palette
 */
object AnsiParser {

    private val ansiRegex = "\u001B\\[(\\d+)(;\\d+)*m".toRegex()

    // Standard ANSI colors (30-37 for foreground, 40-47 for background)
    private val ansiColors = mapOf(
        30 to Color(0xFF000000), // Black
        31 to Color(0xFFFF0000), // Red
        32 to Color(0xFF00FF00), // Green
        33 to Color(0xFFFFFF00), // Yellow
        34 to Color(0xFF0000FF), // Blue
        35 to Color(0xFFFF00FF), // Magenta
        36 to Color(0xFF00FFFF), // Cyan
        37 to Color(0xFFFFFFFF), // White

        // Bright colors (90-97)
        90 to Color(0xFF808080), // Bright Black (Gray)
        91 to Color(0xFFFF8080), // Bright Red
        92 to Color(0xFF80FF80), // Bright Green
        93 to Color(0xFFFFFF80), // Bright Yellow
        94 to Color(0xFF8080FF), // Bright Blue
        95 to Color(0xFFFF80FF), // Bright Magenta
        96 to Color(0xFF80FFFF), // Bright Cyan
        97 to Color(0xFFFFFFFF), // Bright White
    )

    private val defaultColor = Color(0xFF00FF00) // Neon green default

    /**
     * Parse ANSI escape codes and return styled text
     */
    fun parse(text: String): AnnotatedString = buildAnnotatedString {
        var currentColor = defaultColor
        var currentIndex = 0

        ansiRegex.findAll(text).forEach { match ->
            // Append text before the ANSI code
            val beforeText = text.substring(currentIndex, match.range.first)
            if (beforeText.isNotEmpty()) {
                pushStyle(SpanStyle(color = currentColor))
                append(beforeText)
                pop()
            }

            // Parse the ANSI code
            val codes = match.groupValues[1]
            val code = codes.toIntOrNull() ?: 0

            when (code) {
                0 -> currentColor = defaultColor // Reset
                in 30..37 -> currentColor = ansiColors[code] ?: defaultColor
                in 90..97 -> currentColor = ansiColors[code] ?: defaultColor
                // Add more codes as needed (bold, underline, etc.)
            }

            currentIndex = match.range.last + 1
        }

        // Append remaining text
        val remainingText = text.substring(currentIndex)
        if (remainingText.isNotEmpty()) {
            pushStyle(SpanStyle(color = currentColor))
            append(remainingText)
            pop()
        }
    }

    /**
     * Strip ANSI codes from text (returns plain text)
     */
    fun strip(text: String): String {
        return ansiRegex.replace(text, "")
    }
}
