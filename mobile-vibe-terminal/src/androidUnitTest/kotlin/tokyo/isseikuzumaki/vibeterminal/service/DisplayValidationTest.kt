package tokyo.isseikuzumaki.vibeterminal.service

import android.view.Display
import io.mockk.every
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for Display.isValidSecondaryDisplay() extension function.
 *
 * These tests verify that the display detection logic correctly distinguishes between:
 * - Valid secondary displays (HDMI, Miracast) with FLAG_PRESENTATION
 * - Invalid displays (screen recording virtual displays) without FLAG_PRESENTATION
 */
class DisplayValidationTest {

    /**
     * Test: Screen recording virtual display should be ignored.
     *
     * Condition: displayId != DEFAULT_DISPLAY, flags = 0 (no FLAG_PRESENTATION)
     * Expected: isValidSecondaryDisplay() returns false
     */
    @Test
    fun isValidSecondaryDisplay_screenRecordingDisplay_returnsFalse() {
        val mockDisplay = mockk<Display>()
        every { mockDisplay.displayId } returns 1 // Not DEFAULT_DISPLAY (0)
        every { mockDisplay.flags } returns 0 // No FLAG_PRESENTATION

        assertFalse(
            mockDisplay.isValidSecondaryDisplay(),
            "Screen recording virtual display (no FLAG_PRESENTATION) should return false"
        )
    }

    /**
     * Test: Screen recording with FLAG_PRIVATE should be ignored.
     *
     * Condition: displayId != DEFAULT_DISPLAY, flags = FLAG_PRIVATE only
     * Expected: isValidSecondaryDisplay() returns false
     */
    @Test
    fun isValidSecondaryDisplay_privateDisplay_returnsFalse() {
        val mockDisplay = mockk<Display>()
        every { mockDisplay.displayId } returns 2
        every { mockDisplay.flags } returns Display.FLAG_PRIVATE

        assertFalse(
            mockDisplay.isValidSecondaryDisplay(),
            "Private display (FLAG_PRIVATE only) should return false"
        )
    }

    /**
     * Test: HDMI connected display should be detected.
     *
     * Condition: displayId != DEFAULT_DISPLAY, flags = FLAG_PRESENTATION
     * Expected: isValidSecondaryDisplay() returns true
     */
    @Test
    fun isValidSecondaryDisplay_hdmiDisplay_returnsTrue() {
        val mockDisplay = mockk<Display>()
        every { mockDisplay.displayId } returns 1
        every { mockDisplay.flags } returns Display.FLAG_PRESENTATION

        assertTrue(
            mockDisplay.isValidSecondaryDisplay(),
            "HDMI display (with FLAG_PRESENTATION) should return true"
        )
    }

    /**
     * Test: Miracast/wireless display should be detected.
     *
     * Condition: displayId != DEFAULT_DISPLAY, flags = FLAG_PRESENTATION | FLAG_SECURE
     * Expected: isValidSecondaryDisplay() returns true
     */
    @Test
    fun isValidSecondaryDisplay_miracastDisplay_returnsTrue() {
        val mockDisplay = mockk<Display>()
        every { mockDisplay.displayId } returns 3
        every { mockDisplay.flags } returns (Display.FLAG_PRESENTATION or Display.FLAG_SECURE)

        assertTrue(
            mockDisplay.isValidSecondaryDisplay(),
            "Miracast display (with FLAG_PRESENTATION) should return true"
        )
    }

    /**
     * Test: Default display should always be ignored.
     *
     * Condition: displayId == DEFAULT_DISPLAY
     * Expected: isValidSecondaryDisplay() returns false (regardless of flags)
     */
    @Test
    fun isValidSecondaryDisplay_defaultDisplay_returnsFalse() {
        val mockDisplay = mockk<Display>()
        every { mockDisplay.displayId } returns Display.DEFAULT_DISPLAY
        every { mockDisplay.flags } returns Display.FLAG_PRESENTATION

        assertFalse(
            mockDisplay.isValidSecondaryDisplay(),
            "Default display should return false even with FLAG_PRESENTATION"
        )
    }

    /**
     * Test: Multiple displays scenario - only FLAG_PRESENTATION display is valid.
     *
     * This simulates the case where both screen recording and HDMI are active.
     */
    @Test
    fun isValidSecondaryDisplay_mixedDisplays_onlyPresentationIsValid() {
        val screenRecordingDisplay = mockk<Display>()
        every { screenRecordingDisplay.displayId } returns 1
        every { screenRecordingDisplay.flags } returns Display.FLAG_PRIVATE

        val hdmiDisplay = mockk<Display>()
        every { hdmiDisplay.displayId } returns 2
        every { hdmiDisplay.flags } returns Display.FLAG_PRESENTATION

        val displays = listOf(screenRecordingDisplay, hdmiDisplay)

        // Screen recording display should be ignored
        assertFalse(
            screenRecordingDisplay.isValidSecondaryDisplay(),
            "Screen recording display should be ignored"
        )

        // HDMI display should be detected
        assertTrue(
            hdmiDisplay.isValidSecondaryDisplay(),
            "HDMI display should be detected"
        )

        // When iterating with any{}, should find valid display
        assertTrue(
            displays.any { it.isValidSecondaryDisplay() },
            "Should find at least one valid secondary display"
        )
    }

    /**
     * Test: HDMI disconnection while recording continues.
     *
     * After HDMI disconnection, only recording display remains.
     * Expected: No valid secondary display should be found.
     */
    @Test
    fun isValidSecondaryDisplay_hdmiDisconnectedRecordingContinues_noValidDisplay() {
        val screenRecordingDisplay = mockk<Display>()
        every { screenRecordingDisplay.displayId } returns 1
        every { screenRecordingDisplay.flags } returns Display.FLAG_PRIVATE

        val displays = listOf(screenRecordingDisplay)

        assertFalse(
            displays.any { it.isValidSecondaryDisplay() },
            "No valid secondary display when only recording display exists"
        )
    }
}
