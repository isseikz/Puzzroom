package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for TerminalDisplayManager.
 *
 * These tests verify that the state transition matrix for secondary display control
 * is correctly implemented as specified in DesignDocument.md Section 4.4.
 *
 * State Transition Matrix:
 * | isDisplayConnected | useExternalDisplay | terminalDisplayTarget |
 * |:------------------:|:------------------:|:---------------------:|
 * | false              | false              | MAIN                  |
 * | false              | true               | MAIN                  |
 * | true               | false              | MAIN                  |
 * | true               | true               | SECONDARY             |
 *
 * The derived state (terminalDisplayTarget) should be SECONDARY only when
 * both conditions are true: display is connected AND user wants to use it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TerminalDisplayManagerTest {

    /**
     * Test: isDisplayConnected=false, useExternalDisplay=false -> MAIN
     *
     * When no external display is connected and user doesn't want to use it,
     * terminal should be displayed on main display.
     */
    @Test
    fun terminalDisplayTarget_noDisplayNotEnabled_returnsMain() = runTest {
        val manager = TerminalDisplayManager(scope = backgroundScope)

        manager.setDisplayConnected(false)
        manager.setUseExternalDisplay(false)
        advanceUntilIdle()

        assertEquals(
            DisplayTarget.MAIN,
            manager.terminalDisplayTarget.value,
            "Should be MAIN when display not connected and not enabled"
        )
    }

    /**
     * Test: isDisplayConnected=false, useExternalDisplay=true -> MAIN
     *
     * When no external display is connected but user wants to use it,
     * terminal should still be displayed on main display (can't use what's not there).
     */
    @Test
    fun terminalDisplayTarget_noDisplayButEnabled_returnsMain() = runTest {
        val manager = TerminalDisplayManager(scope = backgroundScope)

        manager.setDisplayConnected(false)
        manager.setUseExternalDisplay(true)
        advanceUntilIdle()

        assertEquals(
            DisplayTarget.MAIN,
            manager.terminalDisplayTarget.value,
            "Should be MAIN when display not connected even if enabled"
        )
    }

    /**
     * Test: isDisplayConnected=true, useExternalDisplay=false -> MAIN
     *
     * When external display is connected but user doesn't want to use it,
     * terminal should be displayed on main display (respect user preference).
     */
    @Test
    fun terminalDisplayTarget_displayConnectedButNotEnabled_returnsMain() = runTest {
        val manager = TerminalDisplayManager(scope = backgroundScope)

        manager.setDisplayConnected(true)
        manager.setUseExternalDisplay(false)
        advanceUntilIdle()

        assertEquals(
            DisplayTarget.MAIN,
            manager.terminalDisplayTarget.value,
            "Should be MAIN when display connected but not enabled"
        )
    }

    /**
     * Test: isDisplayConnected=true, useExternalDisplay=true -> SECONDARY
     *
     * When external display is connected AND user wants to use it,
     * terminal should be displayed on secondary display.
     * This is the ONLY case where SECONDARY should be returned.
     */
    @Test
    fun terminalDisplayTarget_displayConnectedAndEnabled_returnsSecondary() = runTest {
        val manager = TerminalDisplayManager(scope = backgroundScope)

        manager.setDisplayConnected(true)
        manager.setUseExternalDisplay(true)
        advanceUntilIdle()

        assertEquals(
            DisplayTarget.SECONDARY,
            manager.terminalDisplayTarget.value,
            "Should be SECONDARY when display connected and enabled"
        )
    }

    /**
     * Test state transition: MAIN -> SECONDARY
     *
     * When user enables external display while it's connected,
     * the target should transition from MAIN to SECONDARY.
     */
    @Test
    fun terminalDisplayTarget_transitionToSecondary() = runTest {
        val manager = TerminalDisplayManager(scope = backgroundScope)

        // Initial state: display connected but not enabled
        manager.setDisplayConnected(true)
        manager.setUseExternalDisplay(false)
        advanceUntilIdle()
        assertEquals(DisplayTarget.MAIN, manager.terminalDisplayTarget.value)

        // User enables external display
        manager.setUseExternalDisplay(true)
        advanceUntilIdle()
        assertEquals(
            DisplayTarget.SECONDARY,
            manager.terminalDisplayTarget.value,
            "Should transition to SECONDARY when user enables"
        )
    }

    /**
     * Test state transition: SECONDARY -> MAIN (display disconnected)
     *
     * When external display is physically disconnected while in use,
     * the target should transition from SECONDARY to MAIN.
     */
    @Test
    fun terminalDisplayTarget_transitionToMainOnDisconnect() = runTest {
        val manager = TerminalDisplayManager(scope = backgroundScope)

        // Initial state: display connected and enabled
        manager.setDisplayConnected(true)
        manager.setUseExternalDisplay(true)
        advanceUntilIdle()
        assertEquals(DisplayTarget.SECONDARY, manager.terminalDisplayTarget.value)

        // Display disconnected
        manager.setDisplayConnected(false)
        advanceUntilIdle()
        assertEquals(
            DisplayTarget.MAIN,
            manager.terminalDisplayTarget.value,
            "Should transition to MAIN when display disconnected"
        )
    }

    /**
     * Test state transition: SECONDARY -> MAIN (user disabled)
     *
     * When user disables external display while it's in use,
     * the target should transition from SECONDARY to MAIN.
     */
    @Test
    fun terminalDisplayTarget_transitionToMainOnUserDisable() = runTest {
        val manager = TerminalDisplayManager(scope = backgroundScope)

        // Initial state: display connected and enabled
        manager.setDisplayConnected(true)
        manager.setUseExternalDisplay(true)
        advanceUntilIdle()
        assertEquals(DisplayTarget.SECONDARY, manager.terminalDisplayTarget.value)

        // User disables external display
        manager.setUseExternalDisplay(false)
        advanceUntilIdle()
        assertEquals(
            DisplayTarget.MAIN,
            manager.terminalDisplayTarget.value,
            "Should transition to MAIN when user disables"
        )
    }

    /**
     * Test that initial state is MAIN.
     *
     * When TerminalDisplayManager is created, the default target should be MAIN.
     */
    @Test
    fun terminalDisplayTarget_initialStateIsMain() = runTest {
        val manager = TerminalDisplayManager(scope = backgroundScope)
        advanceUntilIdle()

        assertEquals(
            DisplayTarget.MAIN,
            manager.terminalDisplayTarget.value,
            "Initial state should be MAIN"
        )
    }

    /**
     * Test rapid state changes.
     *
     * Verify that the derived state correctly handles rapid changes
     * to input states.
     */
    @Test
    fun terminalDisplayTarget_rapidStateChanges() = runTest {
        val manager = TerminalDisplayManager(scope = backgroundScope)

        // Rapid toggle sequence
        manager.setDisplayConnected(true)
        manager.setUseExternalDisplay(true)
        advanceUntilIdle()
        assertEquals(DisplayTarget.SECONDARY, manager.terminalDisplayTarget.value)

        manager.setUseExternalDisplay(false)
        advanceUntilIdle()
        assertEquals(DisplayTarget.MAIN, manager.terminalDisplayTarget.value)

        manager.setUseExternalDisplay(true)
        advanceUntilIdle()
        assertEquals(DisplayTarget.SECONDARY, manager.terminalDisplayTarget.value)

        manager.setDisplayConnected(false)
        advanceUntilIdle()
        assertEquals(DisplayTarget.MAIN, manager.terminalDisplayTarget.value)

        manager.setDisplayConnected(true)
        advanceUntilIdle()
        assertEquals(DisplayTarget.SECONDARY, manager.terminalDisplayTarget.value)
    }

    /**
     * Test all state combinations exhaustively.
     *
     * This test verifies the complete state transition matrix.
     */
    @Test
    fun terminalDisplayTarget_exhaustiveStateMatrix() = runTest {
        val manager = TerminalDisplayManager(scope = backgroundScope)

        data class TestCase(
            val isDisplayConnected: Boolean,
            val useExternalDisplay: Boolean,
            val expectedTarget: DisplayTarget,
            val description: String
        )

        val testCases = listOf(
            TestCase(false, false, DisplayTarget.MAIN, "Row 1: No display, not enabled"),
            TestCase(false, true, DisplayTarget.MAIN, "Row 2: No display, enabled"),
            TestCase(true, false, DisplayTarget.MAIN, "Row 3: Display connected, not enabled"),
            TestCase(true, true, DisplayTarget.SECONDARY, "Row 4: Display connected and enabled")
        )

        for (testCase in testCases) {
            manager.setDisplayConnected(testCase.isDisplayConnected)
            manager.setUseExternalDisplay(testCase.useExternalDisplay)
            advanceUntilIdle()

            assertEquals(
                testCase.expectedTarget,
                manager.terminalDisplayTarget.value,
                "Failed: ${testCase.description}"
            )
        }
    }

    /**
     * Test that input states are independent.
     *
     * Changing one input state should not affect the other.
     */
    @Test
    fun inputStates_areIndependent() = runTest {
        val manager = TerminalDisplayManager(scope = backgroundScope)

        // Set display connected
        manager.setDisplayConnected(true)
        advanceUntilIdle()
        assertEquals(true, manager.isDisplayConnected.value)
        assertEquals(false, manager.useExternalDisplay.value, "useExternalDisplay should remain false")

        // Set use external display
        manager.setUseExternalDisplay(true)
        advanceUntilIdle()
        assertEquals(true, manager.useExternalDisplay.value)
        assertEquals(true, manager.isDisplayConnected.value, "isDisplayConnected should remain true")

        // Change display connected
        manager.setDisplayConnected(false)
        advanceUntilIdle()
        assertEquals(false, manager.isDisplayConnected.value)
        assertEquals(true, manager.useExternalDisplay.value, "useExternalDisplay should remain true")
    }
}
