package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for TerminalResizeCoordinator.
 *
 * TerminalResizeCoordinator centralizes resize logic:
 * - Receives display metrics from main and secondary displays
 * - Determines which metrics to use based on DisplayTarget
 * - Emits target metrics only when they actually change
 *
 * Truth table:
 * | DisplayTarget | Main Metrics | Secondary Metrics | Result |
 * |---------------|--------------|-------------------|--------|
 * | MAIN          | present      | any               | main   |
 * | MAIN          | null         | any               | null   |
 * | SECONDARY     | any          | present           | secondary |
 * | SECONDARY     | present      | null              | main (fallback) |
 * | SECONDARY     | null         | null              | null   |
 */
class TerminalResizeCoordinatorTest {

    private lateinit var coordinator: TerminalResizeCoordinator

    @BeforeTest
    fun setUp() {
        coordinator = TerminalResizeCoordinator()
    }

    // ========== Basic Target Selection Tests ==========

    /**
     * Test: MAIN target with main metrics available -> returns main metrics
     */
    @Test
    fun determineTargetMetrics_mainTarget_withMainMetrics_returnsMain() {
        val mainMetrics = DisplayMetrics(cols = 80, rows = 24, widthPx = 800, heightPx = 480)
        val secondaryMetrics = DisplayMetrics(cols = 120, rows = 40, widthPx = 1920, heightPx = 1080)

        val result = coordinator.determineTargetMetrics(
            target = DisplayTarget.MAIN,
            main = mainMetrics,
            secondary = secondaryMetrics
        )

        assertEquals(mainMetrics, result, "MAIN target should use main metrics")
    }

    /**
     * Test: MAIN target with no main metrics -> returns null
     */
    @Test
    fun determineTargetMetrics_mainTarget_withoutMainMetrics_returnsNull() {
        val secondaryMetrics = DisplayMetrics(cols = 120, rows = 40, widthPx = 1920, heightPx = 1080)

        val result = coordinator.determineTargetMetrics(
            target = DisplayTarget.MAIN,
            main = null,
            secondary = secondaryMetrics
        )

        assertNull(result, "MAIN target without main metrics should return null")
    }

    /**
     * Test: SECONDARY target with secondary metrics available -> returns secondary metrics
     */
    @Test
    fun determineTargetMetrics_secondaryTarget_withSecondaryMetrics_returnsSecondary() {
        val mainMetrics = DisplayMetrics(cols = 80, rows = 24, widthPx = 800, heightPx = 480)
        val secondaryMetrics = DisplayMetrics(cols = 120, rows = 40, widthPx = 1920, heightPx = 1080)

        val result = coordinator.determineTargetMetrics(
            target = DisplayTarget.SECONDARY,
            main = mainMetrics,
            secondary = secondaryMetrics
        )

        assertEquals(secondaryMetrics, result, "SECONDARY target should use secondary metrics")
    }

    /**
     * Test: SECONDARY target without secondary metrics but with main -> falls back to main
     */
    @Test
    fun determineTargetMetrics_secondaryTarget_withoutSecondaryMetrics_fallsBackToMain() {
        val mainMetrics = DisplayMetrics(cols = 80, rows = 24, widthPx = 800, heightPx = 480)

        val result = coordinator.determineTargetMetrics(
            target = DisplayTarget.SECONDARY,
            main = mainMetrics,
            secondary = null
        )

        assertEquals(mainMetrics, result, "SECONDARY target should fall back to main when secondary unavailable")
    }

    /**
     * Test: SECONDARY target with neither metrics available -> returns null
     */
    @Test
    fun determineTargetMetrics_secondaryTarget_withNoMetrics_returnsNull() {
        val result = coordinator.determineTargetMetrics(
            target = DisplayTarget.SECONDARY,
            main = null,
            secondary = null
        )

        assertNull(result, "Should return null when no metrics available")
    }

    // ========== State Flow Tests ==========

    /**
     * Test: Setting main metrics updates targetMetrics when target is MAIN
     */
    @Test
    fun setMainDisplayMetrics_whenTargetIsMain_updatesTargetMetrics() {
        // Set target to MAIN
        coordinator.setDisplayTarget(DisplayTarget.MAIN)

        val mainMetrics = DisplayMetrics(cols = 80, rows = 24, widthPx = 800, heightPx = 480)
        coordinator.setMainDisplayMetrics(mainMetrics)

        assertEquals(mainMetrics, coordinator.targetMetrics.value)
    }

    /**
     * Test: Setting secondary metrics updates targetMetrics when target is SECONDARY
     */
    @Test
    fun setSecondaryDisplayMetrics_whenTargetIsSecondary_updatesTargetMetrics() {
        // Set target to SECONDARY
        coordinator.setDisplayTarget(DisplayTarget.SECONDARY)

        val secondaryMetrics = DisplayMetrics(cols = 120, rows = 40, widthPx = 1920, heightPx = 1080)
        coordinator.setSecondaryDisplayMetrics(secondaryMetrics)

        assertEquals(secondaryMetrics, coordinator.targetMetrics.value)
    }

    /**
     * Test: Changing target from MAIN to SECONDARY switches to secondary metrics
     */
    @Test
    fun setDisplayTarget_fromMainToSecondary_switchesToSecondaryMetrics() {
        val mainMetrics = DisplayMetrics(cols = 80, rows = 24, widthPx = 800, heightPx = 480)
        val secondaryMetrics = DisplayMetrics(cols = 120, rows = 40, widthPx = 1920, heightPx = 1080)

        // Setup both metrics
        coordinator.setMainDisplayMetrics(mainMetrics)
        coordinator.setSecondaryDisplayMetrics(secondaryMetrics)

        // Initially MAIN
        coordinator.setDisplayTarget(DisplayTarget.MAIN)
        assertEquals(mainMetrics, coordinator.targetMetrics.value)

        // Switch to SECONDARY
        coordinator.setDisplayTarget(DisplayTarget.SECONDARY)
        assertEquals(secondaryMetrics, coordinator.targetMetrics.value)
    }

    /**
     * Test: Changing target from SECONDARY to MAIN switches to main metrics
     */
    @Test
    fun setDisplayTarget_fromSecondaryToMain_switchesToMainMetrics() {
        val mainMetrics = DisplayMetrics(cols = 80, rows = 24, widthPx = 800, heightPx = 480)
        val secondaryMetrics = DisplayMetrics(cols = 120, rows = 40, widthPx = 1920, heightPx = 1080)

        // Setup both metrics
        coordinator.setMainDisplayMetrics(mainMetrics)
        coordinator.setSecondaryDisplayMetrics(secondaryMetrics)

        // Initially SECONDARY
        coordinator.setDisplayTarget(DisplayTarget.SECONDARY)
        assertEquals(secondaryMetrics, coordinator.targetMetrics.value)

        // Switch to MAIN
        coordinator.setDisplayTarget(DisplayTarget.MAIN)
        assertEquals(mainMetrics, coordinator.targetMetrics.value)
    }

    // ========== Fallback Behavior Tests ==========

    /**
     * Test: SECONDARY target falls back to main when secondary cleared
     */
    @Test
    fun clearSecondaryDisplayMetrics_whenTargetIsSecondary_fallsBackToMain() {
        val mainMetrics = DisplayMetrics(cols = 80, rows = 24, widthPx = 800, heightPx = 480)
        val secondaryMetrics = DisplayMetrics(cols = 120, rows = 40, widthPx = 1920, heightPx = 1080)

        // Setup
        coordinator.setMainDisplayMetrics(mainMetrics)
        coordinator.setSecondaryDisplayMetrics(secondaryMetrics)
        coordinator.setDisplayTarget(DisplayTarget.SECONDARY)
        assertEquals(secondaryMetrics, coordinator.targetMetrics.value)

        // Clear secondary
        coordinator.clearSecondaryDisplayMetrics()
        assertEquals(mainMetrics, coordinator.targetMetrics.value, "Should fall back to main metrics")
    }

    /**
     * Test: Initial state has null targetMetrics
     */
    @Test
    fun initialState_targetMetricsIsNull() {
        assertNull(coordinator.targetMetrics.value, "Initial targetMetrics should be null")
    }

    // ========== No-op Tests (metrics unchanged) ==========

    /**
     * Test: Setting same main metrics doesn't trigger unnecessary update
     */
    @Test
    fun setMainDisplayMetrics_sameValue_noChange() {
        coordinator.setDisplayTarget(DisplayTarget.MAIN)

        val mainMetrics = DisplayMetrics(cols = 80, rows = 24, widthPx = 800, heightPx = 480)
        coordinator.setMainDisplayMetrics(mainMetrics)

        val initialValue = coordinator.targetMetrics.value

        // Set same value again
        coordinator.setMainDisplayMetrics(mainMetrics.copy())

        // Value should be equal (no unnecessary emission)
        assertEquals(initialValue, coordinator.targetMetrics.value)
    }

    /**
     * Test: Setting main metrics when target is SECONDARY doesn't change targetMetrics
     */
    @Test
    fun setMainDisplayMetrics_whenTargetIsSecondary_noChangeToTargetMetrics() {
        val secondaryMetrics = DisplayMetrics(cols = 120, rows = 40, widthPx = 1920, heightPx = 1080)

        coordinator.setSecondaryDisplayMetrics(secondaryMetrics)
        coordinator.setDisplayTarget(DisplayTarget.SECONDARY)

        val initialValue = coordinator.targetMetrics.value
        assertEquals(secondaryMetrics, initialValue)

        // Set main metrics (should not affect targetMetrics since target is SECONDARY)
        val mainMetrics = DisplayMetrics(cols = 80, rows = 24, widthPx = 800, heightPx = 480)
        coordinator.setMainDisplayMetrics(mainMetrics)

        assertEquals(secondaryMetrics, coordinator.targetMetrics.value,
            "Setting main metrics should not change targetMetrics when target is SECONDARY")
    }

    // ========== Edge Cases ==========

    /**
     * Test: Setting secondary metrics when target is MAIN and secondary becomes available later
     */
    @Test
    fun setSecondaryDisplayMetrics_whenTargetIsMain_preparesForLaterSwitch() {
        val mainMetrics = DisplayMetrics(cols = 80, rows = 24, widthPx = 800, heightPx = 480)
        val secondaryMetrics = DisplayMetrics(cols = 120, rows = 40, widthPx = 1920, heightPx = 1080)

        coordinator.setDisplayTarget(DisplayTarget.MAIN)
        coordinator.setMainDisplayMetrics(mainMetrics)
        assertEquals(mainMetrics, coordinator.targetMetrics.value)

        // Set secondary metrics while target is MAIN
        coordinator.setSecondaryDisplayMetrics(secondaryMetrics)
        assertEquals(mainMetrics, coordinator.targetMetrics.value,
            "targetMetrics should still be main")

        // Now switch target
        coordinator.setDisplayTarget(DisplayTarget.SECONDARY)
        assertEquals(secondaryMetrics, coordinator.targetMetrics.value,
            "Should immediately use secondary metrics after target switch")
    }
}
