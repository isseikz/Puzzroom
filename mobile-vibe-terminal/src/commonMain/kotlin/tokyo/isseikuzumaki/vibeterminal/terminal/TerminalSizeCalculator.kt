package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlin.math.floor

/**
 * Utility class for calculating terminal dimensions based on display size and font metrics.
 *
 * This class ensures that the calculated terminal dimensions will always fit within
 * the display area, preventing any clipping or overflow.
 */
object TerminalSizeCalculator {

    /**
     * Result of terminal size calculation.
     *
     * @property cols Number of columns that fit in the display width
     * @property rows Number of rows that fit in the display height
     * @property actualCharWidth The character width used for calculation
     * @property actualCharHeight The character height used for calculation
     */
    data class TerminalDimensions(
        val cols: Int,
        val rows: Int,
        val actualCharWidth: Float,
        val actualCharHeight: Float
    ) {
        /**
         * Returns the total width required to display all columns.
         * This should always be <= the display width used for calculation.
         */
        val requiredWidth: Float get() = cols * actualCharWidth

        /**
         * Returns the total height required to display all rows.
         * This should always be <= the display height used for calculation.
         */
        val requiredHeight: Float get() = rows * actualCharHeight
    }

    /**
     * Calculate terminal dimensions based on display size and measured character dimensions.
     *
     * Uses floor() to ensure the calculated cols/rows will always fit within the display,
     * even if it means having some margin at the edges. This prevents clipping.
     *
     * @param displayWidthPx Display width in pixels
     * @param displayHeightPx Display height in pixels
     * @param charWidth Actual measured character width in pixels
     * @param charHeight Actual measured character height in pixels
     * @return TerminalDimensions containing the calculated cols and rows
     */
    fun calculate(
        displayWidthPx: Int,
        displayHeightPx: Int,
        charWidth: Float,
        charHeight: Float
    ): TerminalDimensions {
        require(displayWidthPx > 0) { "Display width must be positive" }
        require(displayHeightPx > 0) { "Display height must be positive" }
        require(charWidth > 0f) { "Character width must be positive" }
        require(charHeight > 0f) { "Character height must be positive" }

        // Use floor to ensure we never exceed the display bounds
        // This guarantees no clipping at the cost of potential margin
        val cols = floor(displayWidthPx / charWidth).toInt().coerceAtLeast(1)
        val rows = floor(displayHeightPx / charHeight).toInt().coerceAtLeast(1)

        return TerminalDimensions(
            cols = cols,
            rows = rows,
            actualCharWidth = charWidth,
            actualCharHeight = charHeight
        )
    }

    /**
     * Calculate terminal dimensions using estimated character dimensions.
     *
     * This method uses conservative estimates for character dimensions based on
     * font size and density. The estimates are intentionally larger than typical
     * monospace font metrics to ensure the terminal fits within the display.
     *
     * @param displayWidthPx Display width in pixels
     * @param displayHeightPx Display height in pixels
     * @param fontSizeSp Font size in scaled pixels (sp)
     * @param density Display density
     * @return TerminalDimensions containing the calculated cols and rows
     */
    fun calculateWithEstimatedCharSize(
        displayWidthPx: Int,
        displayHeightPx: Int,
        fontSizeSp: Float,
        density: Float
    ): TerminalDimensions {
        require(fontSizeSp > 0f) { "Font size must be positive" }
        require(density > 0f) { "Density must be positive" }

        val fontSizePx = fontSizeSp * density

        // Use conservative multipliers to avoid clipping
        // These values are intentionally larger than typical monospace ratios
        // Typical monospace: width = 0.5-0.6, height = 1.0-1.2
        // Conservative:      width = 0.65,    height = 1.3
        val charWidth = fontSizePx * CONSERVATIVE_WIDTH_RATIO
        val charHeight = fontSizePx * CONSERVATIVE_HEIGHT_RATIO

        return calculate(displayWidthPx, displayHeightPx, charWidth, charHeight)
    }

    /**
     * Validate that the given terminal dimensions will fit within the display.
     *
     * @param cols Number of terminal columns
     * @param rows Number of terminal rows
     * @param displayWidthPx Display width in pixels
     * @param displayHeightPx Display height in pixels
     * @param charWidth Character width in pixels
     * @param charHeight Character height in pixels
     * @return true if the terminal fits within the display, false otherwise
     */
    fun validateFit(
        cols: Int,
        rows: Int,
        displayWidthPx: Int,
        displayHeightPx: Int,
        charWidth: Float,
        charHeight: Float
    ): Boolean {
        val requiredWidth = cols * charWidth
        val requiredHeight = rows * charHeight
        return requiredWidth <= displayWidthPx && requiredHeight <= displayHeightPx
    }

    // Conservative ratios for estimating character dimensions
    // These ensure the terminal always fits, even with different fonts
    private const val CONSERVATIVE_WIDTH_RATIO = 0.65f
    private const val CONSERVATIVE_HEIGHT_RATIO = 1.3f
}
