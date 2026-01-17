package tokyo.isseikuzumaki.vibeterminal.terminal

/**
 * Mouse tracking modes (WHAT events to report)
 *
 * These modes determine which mouse events the terminal should report back to the server.
 * See: https://www.xfree86.org/current/ctlseqs.html
 */
enum class MouseTrackingMode {
    /** No mouse reporting - default state */
    NONE,

    /**
     * X10 compatibility mode (DECSET 1000)
     * Reports: Button press and release
     * Does NOT report: Mouse movement
     */
    NORMAL,

    /**
     * Button event tracking (DECSET 1002)
     * Reports: Button press, release, and motion while button is held
     * Does NOT report: Motion without button pressed
     */
    BUTTON_EVENT,

    /**
     * Any event tracking (DECSET 1003)
     * Reports: All mouse events including motion without button
     * Warning: Generates heavy traffic, typically used for debugging
     */
    ANY_EVENT
}

/**
 * Mouse encoding modes (HOW events are formatted)
 *
 * These modes determine the format used to encode mouse events.
 * Encoding mode is independent of tracking mode.
 */
enum class MouseEncodingMode {
    /**
     * Legacy X10/VT200 encoding
     * Format: ESC [ M Cb Cx Cy
     * Limitation: Coordinates limited to 223 (255-32)
     */
    NORMAL,

    /**
     * SGR Extended mode (DECSET 1006)
     * Format: ESC [ < Cb ; Cx ; Cy M/m
     * Advantages:
     * - No coordinate limit
     * - UTF-8 safe
     * - Explicit press (M) vs release (m)
     */
    SGR
}

/**
 * Scroll direction for mouse wheel events
 */
enum class ScrollDirection {
    UP,
    DOWN
}
