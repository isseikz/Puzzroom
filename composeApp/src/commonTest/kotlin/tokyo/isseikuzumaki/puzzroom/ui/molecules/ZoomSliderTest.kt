package tokyo.isseikuzumaki.puzzroom.ui.molecules

import tokyo.isseikuzumaki.puzzroom.ui.state.SliderState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for ZoomSlider molecule
 */
class ZoomSliderTest {

    @Test
    fun testSliderStateInitialization() {
        // Given
        val sliderState = SliderState(
            initialValue = 1000f,
            valueRange = 500f..1500f
        )

        // Then
        assertEquals(1000f, sliderState.value, "Initial value should be 1000")
    }

    @Test
    fun testSliderStateValueChange() {
        // Given
        var changedValue = 0f
        val sliderState = SliderState(
            initialValue = 1000f,
            valueRange = 500f..1500f,
            onValueChange = { newValue -> changedValue = newValue }
        )

        // When
        sliderState.updateValue(800f)

        // Then
        assertEquals(800f, sliderState.value, "Value should be updated to 800")
        assertEquals(800f, changedValue, "Callback should be called with new value")
    }

    @Test
    fun testSliderStateValueCoercion() {
        // Given
        val sliderState = SliderState(
            initialValue = 1000f,
            valueRange = 500f..1500f
        )

        // When - try to set value below minimum
        sliderState.updateValue(300f)

        // Then
        assertEquals(500f, sliderState.value, "Value should be coerced to minimum")

        // When - try to set value above maximum
        sliderState.updateValue(2000f)

        // Then
        assertEquals(1500f, sliderState.value, "Value should be coerced to maximum")
    }

    @Test
    fun testSliderStateFraction() {
        // Given
        val sliderState = SliderState(
            initialValue = 1000f,
            valueRange = 500f..1500f
        )

        // When value is at middle
        sliderState.updateValue(1000f)

        // Then
        assertEquals(0.5f, sliderState.coercedValueAsFraction, "Fraction should be 0.5 for middle value")

        // When value is at minimum
        sliderState.updateValue(500f)

        // Then
        assertEquals(0f, sliderState.coercedValueAsFraction, "Fraction should be 0 for minimum value")

        // When value is at maximum
        sliderState.updateValue(1500f)

        // Then
        assertEquals(1f, sliderState.coercedValueAsFraction, "Fraction should be 1 for maximum value")
    }
}
