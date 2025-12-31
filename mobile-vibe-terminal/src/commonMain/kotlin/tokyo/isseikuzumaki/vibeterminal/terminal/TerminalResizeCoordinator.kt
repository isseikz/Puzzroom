package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Display metrics for terminal sizing.
 */
data class DisplayMetrics(
    val cols: Int,
    val rows: Int,
    val widthPx: Int,
    val heightPx: Int
)

/**
 * Centralizes terminal resize logic.
 *
 * Responsibilities:
 * - Receives display metrics from main and secondary displays
 * - Determines which metrics to use based on active DisplayTarget
 * - Emits target metrics when they change
 *
 * This class separates resize decision logic from UI, making it unit-testable.
 *
 * TODO: Implement the logic (TDD - tests written first)
 */
class TerminalResizeCoordinator {

    private val _targetMetrics = MutableStateFlow<DisplayMetrics?>(null)

    /**
     * The metrics that should be applied to the terminal.
     * Emits when the active display's metrics change.
     */
    val targetMetrics: StateFlow<DisplayMetrics?> = _targetMetrics.asStateFlow()

    /**
     * Pure function to determine which metrics to use.
     *
     * Truth table:
     * | DisplayTarget | Main Metrics | Secondary Metrics | Result          |
     * |---------------|--------------|-------------------|-----------------|
     * | MAIN          | present      | any               | main            |
     * | MAIN          | null         | any               | null            |
     * | SECONDARY     | any          | present           | secondary       |
     * | SECONDARY     | present      | null              | main (fallback) |
     * | SECONDARY     | null         | null              | null            |
     *
     * @param target The active display target
     * @param main Main display metrics (nullable)
     * @param secondary Secondary display metrics (nullable)
     * @return The metrics to apply, or null if unavailable
     */
    fun determineTargetMetrics(
        target: DisplayTarget,
        main: DisplayMetrics?,
        secondary: DisplayMetrics?
    ): DisplayMetrics? {
        // TODO: Implement - currently returns null to fail tests
        return null
    }

    /**
     * Set the active display target.
     */
    fun setDisplayTarget(target: DisplayTarget) {
        // TODO: Implement
    }

    /**
     * Set main display metrics.
     */
    fun setMainDisplayMetrics(metrics: DisplayMetrics) {
        // TODO: Implement
    }

    /**
     * Set secondary display metrics.
     */
    fun setSecondaryDisplayMetrics(metrics: DisplayMetrics) {
        // TODO: Implement
    }

    /**
     * Clear secondary display metrics (e.g., when secondary display disconnects).
     */
    fun clearSecondaryDisplayMetrics() {
        // TODO: Implement
    }
}
