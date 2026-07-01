package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Unit tests for TerminalSizeCalculator.
 *
 * These tests ensure that the terminal dimensions are calculated correctly
 * for various screen sizes and that the terminal content never exceeds
 * the display boundaries (no clipping).
 */
class TerminalSizeCalculatorTest {

    // Common screen resolutions to test
    private val screenSizes = listOf(
        // width, height, description
        Triple(1920, 1080, "Full HD"),
        Triple(1280, 720, "HD"),
        Triple(1024, 768, "XGA"),
        Triple(800, 600, "SVGA"),
        Triple(640, 480, "VGA"),
        Triple(2560, 1440, "QHD"),
        Triple(3840, 2160, "4K UHD"),
        Triple(1366, 768, "Common laptop"),
        Triple(1536, 864, "Common laptop HD+"),
        Triple(2048, 1536, "iPad Retina"),
        Triple(1334, 750, "iPhone landscape"),
        Triple(2208, 1242, "iPhone Plus landscape")
    )

    // Common density values
    private val densities = listOf(1.0f, 1.5f, 2.0f, 2.5f, 3.0f)

    // Common font sizes
    private val fontSizes = listOf(10f, 12f, 14f, 16f, 18f, 20f)

    /**
     * Test that calculate() never produces dimensions that exceed the display.
     * This is the critical test to prevent clipping.
     */
    @Test
    fun calculate_neverExceedsDisplayBounds() {
        // Test with various character sizes
        val charSizes = listOf(
            Pair(8f, 16f),    // Small
            Pair(10f, 20f),   // Medium
            Pair(12f, 24f),   // Large
            Pair(14f, 28f),   // Extra large
            Pair(7.5f, 15f),  // Non-integer
            Pair(9.3f, 18.6f) // Non-integer
        )

        for ((width, height, desc) in screenSizes) {
            for ((charWidth, charHeight) in charSizes) {
                val result = TerminalSizeCalculator.calculate(
                    displayWidthPx = width,
                    displayHeightPx = height,
                    charWidth = charWidth,
                    charHeight = charHeight
                )

                // Critical assertion: terminal content must fit within display
                assertTrue(
                    result.requiredWidth <= width,
                    "Terminal width (${result.requiredWidth}) exceeds display width ($width) " +
                            "for $desc screen with char size ${charWidth}x${charHeight}"
                )
                assertTrue(
                    result.requiredHeight <= height,
                    "Terminal height (${result.requiredHeight}) exceeds display height ($height) " +
                            "for $desc screen with char size ${charWidth}x${charHeight}"
                )

                // Validate using the validateFit function
                assertTrue(
                    TerminalSizeCalculator.validateFit(
                        cols = result.cols,
                        rows = result.rows,
                        displayWidthPx = width,
                        displayHeightPx = height,
                        charWidth = charWidth,
                        charHeight = charHeight
                    ),
                    "validateFit should return true for calculated dimensions"
                )
            }
        }
    }

    /**
     * Test that calculateWithEstimatedCharSize() never produces dimensions that exceed the display.
     * Uses conservative estimates for character size.
     */
    @Test
    fun calculateWithEstimatedCharSize_neverExceedsDisplayBounds() {
        for ((width, height, desc) in screenSizes) {
            for (density in densities) {
                for (fontSize in fontSizes) {
                    val result = TerminalSizeCalculator.calculateWithEstimatedCharSize(
                        displayWidthPx = width,
                        displayHeightPx = height,
                        fontSizeSp = fontSize,
                        density = density
                    )

                    // Critical assertion: terminal content must fit within display
                    assertTrue(
                        result.requiredWidth <= width,
                        "Terminal width (${result.requiredWidth}) exceeds display width ($width) " +
                                "for $desc screen, density=$density, fontSize=$fontSize"
                    )
                    assertTrue(
                        result.requiredHeight <= height,
                        "Terminal height (${result.requiredHeight}) exceeds display height ($height) " +
                                "for $desc screen, density=$density, fontSize=$fontSize"
                    )
                }
            }
        }
    }

    /**
     * Test specific case: 1280x720 screen which was reported as having issues.
     */
    @Test
    fun calculate_1280x720_doesNotClip() {
        val width = 1280
        val height = 720

        // Test with various realistic character sizes
        val charSizes = listOf(
            Pair(8.4f, 16.8f),   // 14sp at density 1.0
            Pair(12.6f, 25.2f), // 14sp at density 1.5
            Pair(16.8f, 33.6f), // 14sp at density 2.0
            Pair(9.1f, 18.2f),  // Realistic measured size
            Pair(10.0f, 20.0f)  // Round numbers
        )

        for ((charWidth, charHeight) in charSizes) {
            val result = TerminalSizeCalculator.calculate(
                displayWidthPx = width,
                displayHeightPx = height,
                charWidth = charWidth,
                charHeight = charHeight
            )

            assertTrue(
                result.requiredWidth <= width,
                "1280x720: Width overflow with char ${charWidth}x${charHeight}. " +
                        "Required: ${result.requiredWidth}, Available: $width"
            )
            assertTrue(
                result.requiredHeight <= height,
                "1280x720: Height overflow with char ${charWidth}x${charHeight}. " +
                        "Required: ${result.requiredHeight}, Available: $height"
            )
        }
    }

    /**
     * Test that dimensions are always at least 1x1.
     */
    @Test
    fun calculate_alwaysReturnsAtLeastOneColAndRow() {
        // Even with very large character sizes
        val result = TerminalSizeCalculator.calculate(
            displayWidthPx = 100,
            displayHeightPx = 100,
            charWidth = 200f,  // Larger than display
            charHeight = 200f  // Larger than display
        )

        assertTrue(result.cols >= 1, "Should have at least 1 column")
        assertTrue(result.rows >= 1, "Should have at least 1 row")
    }

    /**
     * Test basic calculation correctness.
     */
    @Test
    fun calculate_producesCorrectDimensions() {
        // Simple case: 100x100 display with 10x10 characters
        val result = TerminalSizeCalculator.calculate(
            displayWidthPx = 100,
            displayHeightPx = 100,
            charWidth = 10f,
            charHeight = 10f
        )

        assertEquals(10, result.cols, "Should have 10 columns")
        assertEquals(10, result.rows, "Should have 10 rows")
    }

    /**
     * Test that floor() is used correctly for non-integer divisions.
     */
    @Test
    fun calculate_usesFloorForNonIntegerDivision() {
        // 100 / 30 = 3.33... should floor to 3
        val result = TerminalSizeCalculator.calculate(
            displayWidthPx = 100,
            displayHeightPx = 100,
            charWidth = 30f,
            charHeight = 30f
        )

        assertEquals(3, result.cols, "Should floor to 3 columns")
        assertEquals(3, result.rows, "Should floor to 3 rows")

        // Verify no clipping
        assertTrue(result.requiredWidth <= 100, "Should not exceed display width")
        assertTrue(result.requiredHeight <= 100, "Should not exceed display height")
    }

    /**
     * Test that validateFit correctly identifies when terminal fits.
     */
    @Test
    fun validateFit_returnsTrueWhenFits() {
        assertTrue(
            TerminalSizeCalculator.validateFit(
                cols = 10,
                rows = 10,
                displayWidthPx = 100,
                displayHeightPx = 100,
                charWidth = 10f,
                charHeight = 10f
            )
        )

        // With margin
        assertTrue(
            TerminalSizeCalculator.validateFit(
                cols = 9,
                rows = 9,
                displayWidthPx = 100,
                displayHeightPx = 100,
                charWidth = 10f,
                charHeight = 10f
            )
        )
    }

    /**
     * Test that validateFit correctly identifies when terminal doesn't fit.
     */
    @Test
    fun validateFit_returnsFalseWhenDoesNotFit() {
        // Width overflow
        assertTrue(
            !TerminalSizeCalculator.validateFit(
                cols = 11,
                rows = 10,
                displayWidthPx = 100,
                displayHeightPx = 100,
                charWidth = 10f,
                charHeight = 10f
            )
        )

        // Height overflow
        assertTrue(
            !TerminalSizeCalculator.validateFit(
                cols = 10,
                rows = 11,
                displayWidthPx = 100,
                displayHeightPx = 100,
                charWidth = 10f,
                charHeight = 10f
            )
        )
    }

    /**
     * Test error handling for invalid inputs.
     */
    @Test
    fun calculate_throwsOnInvalidInput() {
        assertFailsWith<IllegalArgumentException> {
            TerminalSizeCalculator.calculate(0, 100, 10f, 10f)
        }
        assertFailsWith<IllegalArgumentException> {
            TerminalSizeCalculator.calculate(100, 0, 10f, 10f)
        }
        assertFailsWith<IllegalArgumentException> {
            TerminalSizeCalculator.calculate(100, 100, 0f, 10f)
        }
        assertFailsWith<IllegalArgumentException> {
            TerminalSizeCalculator.calculate(100, 100, 10f, 0f)
        }
        assertFailsWith<IllegalArgumentException> {
            TerminalSizeCalculator.calculate(-100, 100, 10f, 10f)
        }
    }

    /**
     * Test that estimated character size calculation is conservative enough.
     * The estimated size should be larger than or equal to typical actual sizes.
     */
    @Test
    fun calculateWithEstimatedCharSize_isConservative() {
        val fontSize = 14f
        val density = 1.5f

        val result = TerminalSizeCalculator.calculateWithEstimatedCharSize(
            displayWidthPx = 1920,
            displayHeightPx = 1080,
            fontSizeSp = fontSize,
            density = density
        )

        // The estimated character size should be conservative (larger)
        // Typical monospace font: width = fontSize * 0.5-0.6, height = fontSize * 1.0-1.2
        // Conservative estimate: width = fontSize * 0.65, height = fontSize * 1.3
        val fontSizePx = fontSize * density
        val expectedMinCharWidth = fontSizePx * 0.5f
        val expectedMinCharHeight = fontSizePx * 1.0f

        assertTrue(
            result.actualCharWidth >= expectedMinCharWidth,
            "Estimated char width should be at least $expectedMinCharWidth, got ${result.actualCharWidth}"
        )
        assertTrue(
            result.actualCharHeight >= expectedMinCharHeight,
            "Estimated char height should be at least $expectedMinCharHeight, got ${result.actualCharHeight}"
        )
    }

    /**
     * Stress test with many random combinations.
     * This helps catch edge cases that might not be covered by specific tests.
     */
    @Test
    fun calculate_stressTestWithVariousCombinations() {
        val widths = listOf(320, 480, 640, 800, 1024, 1280, 1366, 1920, 2560, 3840)
        val heights = listOf(240, 320, 480, 600, 768, 720, 768, 1080, 1440, 2160)
        val charWidths = listOf(6f, 8f, 10f, 12f, 14f, 16f, 18f, 20f)
        val charHeights = listOf(12f, 16f, 20f, 24f, 28f, 32f, 36f, 40f)

        var testCount = 0
        for (w in widths) {
            for (h in heights) {
                for (cw in charWidths) {
                    for (ch in charHeights) {
                        val result = TerminalSizeCalculator.calculate(w, h, cw, ch)

                        assertTrue(
                            result.requiredWidth <= w,
                            "Test $testCount: Width overflow. Display: ${w}x${h}, Char: ${cw}x${ch}, " +
                                    "Required: ${result.requiredWidth}"
                        )
                        assertTrue(
                            result.requiredHeight <= h,
                            "Test $testCount: Height overflow. Display: ${w}x${h}, Char: ${cw}x${ch}, " +
                                    "Required: ${result.requiredHeight}"
                        )

                        testCount++
                    }
                }
            }
        }

        println("Stress test completed: $testCount combinations tested")
    }

    /**
     * Test that the margin (unused space) is minimal.
     * While we prioritize no clipping, we also don't want excessive margin.
     */
    @Test
    fun calculate_marginIsMinimal() {
        val result = TerminalSizeCalculator.calculate(
            displayWidthPx = 1920,
            displayHeightPx = 1080,
            charWidth = 10f,
            charHeight = 20f
        )

        val widthMargin = 1920 - result.requiredWidth
        val heightMargin = 1080 - result.requiredHeight

        // Margin should be less than one character size
        assertTrue(
            widthMargin < 10f,
            "Width margin ($widthMargin) should be less than char width"
        )
        assertTrue(
            heightMargin < 20f,
            "Height margin ($heightMargin) should be less than char height"
        )
    }
}
