package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for TerminalDisplayManager.
 *
 * Simplified behavior:
 * - When secondary display is connected → SECONDARY
 * - When secondary display is disconnected → MAIN
 */
class TerminalDisplayManagerTest {

    @BeforeTest
    fun setUp() {
        // Reset state before each test
        TerminalDisplayManager.reset()
    }

    /**
     * Test: isDisplayConnected=false -> MAIN
     */
    @Test
    fun terminalDisplayTarget_noDisplay_returnsMain() {
        TerminalDisplayManager.setDisplayConnected(false)

        assertEquals(
            DisplayTarget.MAIN,
            TerminalDisplayManager.terminalDisplayTarget.value,
            "Should be MAIN when display not connected"
        )
    }

    /**
     * Test: isDisplayConnected=true -> SECONDARY
     */
    @Test
    fun terminalDisplayTarget_displayConnected_returnsSecondary() {
        TerminalDisplayManager.setDisplayConnected(true)

        assertEquals(
            DisplayTarget.SECONDARY,
            TerminalDisplayManager.terminalDisplayTarget.value,
            "Should be SECONDARY when display connected"
        )
    }

    /**
     * Test state transition: MAIN -> SECONDARY (display connected)
     */
    @Test
    fun terminalDisplayTarget_transitionToSecondary() {
        // Initial state: no display
        TerminalDisplayManager.setDisplayConnected(false)
        assertEquals(DisplayTarget.MAIN, TerminalDisplayManager.terminalDisplayTarget.value)

        // Display connected
        TerminalDisplayManager.setDisplayConnected(true)
        assertEquals(
            DisplayTarget.SECONDARY,
            TerminalDisplayManager.terminalDisplayTarget.value,
            "Should transition to SECONDARY when display connected"
        )
    }

    /**
     * Test state transition: SECONDARY -> MAIN (display disconnected)
     */
    @Test
    fun terminalDisplayTarget_transitionToMainOnDisconnect() {
        // Initial state: display connected
        TerminalDisplayManager.setDisplayConnected(true)
        assertEquals(DisplayTarget.SECONDARY, TerminalDisplayManager.terminalDisplayTarget.value)

        // Display disconnected
        TerminalDisplayManager.setDisplayConnected(false)
        assertEquals(
            DisplayTarget.MAIN,
            TerminalDisplayManager.terminalDisplayTarget.value,
            "Should transition to MAIN when display disconnected"
        )
    }

    /**
     * Test that initial state is MAIN.
     */
    @Test
    fun terminalDisplayTarget_initialStateIsMain() {
        assertEquals(
            DisplayTarget.MAIN,
            TerminalDisplayManager.terminalDisplayTarget.value,
            "Initial state should be MAIN"
        )
    }

    /**
     * Test rapid state changes.
     */
    @Test
    fun terminalDisplayTarget_rapidStateChanges() {
        TerminalDisplayManager.setDisplayConnected(true)
        assertEquals(DisplayTarget.SECONDARY, TerminalDisplayManager.terminalDisplayTarget.value)

        TerminalDisplayManager.setDisplayConnected(false)
        assertEquals(DisplayTarget.MAIN, TerminalDisplayManager.terminalDisplayTarget.value)

        TerminalDisplayManager.setDisplayConnected(true)
        assertEquals(DisplayTarget.SECONDARY, TerminalDisplayManager.terminalDisplayTarget.value)

        TerminalDisplayManager.setDisplayConnected(false)
        assertEquals(DisplayTarget.MAIN, TerminalDisplayManager.terminalDisplayTarget.value)
    }
}
