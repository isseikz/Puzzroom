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
 */
class TerminalResizeCoordinator {

    // ========== Internal State ==========

    private var currentTarget: DisplayTarget = DisplayTarget.MAIN
    private var mainMetrics: DisplayMetrics? = null
    private var secondaryMetrics: DisplayMetrics? = null

    // ========== Output ==========

    private val _targetMetrics = MutableStateFlow<DisplayMetrics?>(null)

    /**
     * The metrics that should be applied to the terminal.
     * Emits when the active display's metrics change.
     */
    val targetMetrics: StateFlow<DisplayMetrics?> = _targetMetrics.asStateFlow()

    // ========== Pure Function (Testable Logic) ==========

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
        return when (target) {
            DisplayTarget.MAIN -> main
            DisplayTarget.SECONDARY -> secondary ?: main  // Fallback to main if secondary unavailable
        }
    }

    // ========== State Mutators ==========

    /**
     * Set the active display target.
     * Updates targetMetrics if the resulting metrics change.
     */
    fun setDisplayTarget(target: DisplayTarget) {
        if (currentTarget != target) {
            currentTarget = target
            updateTargetMetrics()
        }
    }

    /**
     * Set main display metrics.
     * Updates targetMetrics if target is MAIN or if it's used as fallback.
     */
    fun setMainDisplayMetrics(metrics: DisplayMetrics) {
        if (mainMetrics != metrics) {
            mainMetrics = metrics
            updateTargetMetrics()
        }
    }

    /**
     * Set secondary display metrics.
     * Updates targetMetrics if target is SECONDARY.
     */
    fun setSecondaryDisplayMetrics(metrics: DisplayMetrics) {
        if (secondaryMetrics != metrics) {
            secondaryMetrics = metrics
            updateTargetMetrics()
        }
    }

    /**
     * Clear secondary display metrics (e.g., when secondary display disconnects).
     * If target is SECONDARY, will fall back to main metrics.
     */
    fun clearSecondaryDisplayMetrics() {
        if (secondaryMetrics != null) {
            secondaryMetrics = null
            updateTargetMetrics()
        }
    }

    // ========== Internal ==========

    /**
     * Recalculate and emit targetMetrics based on current state.
     */
    private fun updateTargetMetrics() {
        val newMetrics = determineTargetMetrics(currentTarget, mainMetrics, secondaryMetrics)
        if (_targetMetrics.value != newMetrics) {
            _targetMetrics.value = newMetrics
        }
    }
}
