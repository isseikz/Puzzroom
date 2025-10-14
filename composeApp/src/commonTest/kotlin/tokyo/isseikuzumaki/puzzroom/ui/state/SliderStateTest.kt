package tokyo.isseikuzumaki.puzzroom.ui.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for SliderState
 */
class SliderStateTest {

    @Test
    fun `initial value is coerced to value range`() {
        val state = SliderState(
            initialValue = 1.5f,
            valueRange = 0f..1f
        )
        assertEquals(1.0f, state.value)
    }

    @Test
    fun `initial value below range is coerced to minimum`() {
        val state = SliderState(
            initialValue = -0.5f,
            valueRange = 0f..1f
        )
        assertEquals(0.0f, state.value)
    }

    @Test
    fun `updateValue sets value within range`() {
        val state = SliderState(
            initialValue = 0.5f,
            valueRange = 0f..1f
        )
        state.updateValue(0.7f)
        assertEquals(0.7f, state.value)
    }

    @Test
    fun `updateValue coerces value to maximum`() {
        val state = SliderState(
            initialValue = 0.5f,
            valueRange = 0f..1f
        )
        state.updateValue(1.5f)
        assertEquals(1.0f, state.value)
    }

    @Test
    fun `updateValue coerces value to minimum`() {
        val state = SliderState(
            initialValue = 0.5f,
            valueRange = 0f..1f
        )
        state.updateValue(-0.5f)
        assertEquals(0.0f, state.value)
    }

    @Test
    fun `updateValueFromFraction updates value correctly`() {
        val state = SliderState(
            initialValue = 0f,
            valueRange = 0f..100f
        )
        state.updateValueFromFraction(0.5f)
        assertEquals(50f, state.value)
    }

    @Test
    fun `updateValueFromFraction at minimum`() {
        val state = SliderState(
            initialValue = 50f,
            valueRange = 0f..100f
        )
        state.updateValueFromFraction(0f)
        assertEquals(0f, state.value)
    }

    @Test
    fun `updateValueFromFraction at maximum`() {
        val state = SliderState(
            initialValue = 50f,
            valueRange = 0f..100f
        )
        state.updateValueFromFraction(1f)
        assertEquals(100f, state.value)
    }

    @Test
    fun `updateValueFromFraction coerces fraction above 1`() {
        val state = SliderState(
            initialValue = 0f,
            valueRange = 0f..100f
        )
        state.updateValueFromFraction(1.5f)
        assertEquals(100f, state.value)
    }

    @Test
    fun `updateValueFromFraction coerces fraction below 0`() {
        val state = SliderState(
            initialValue = 50f,
            valueRange = 0f..100f
        )
        state.updateValueFromFraction(-0.5f)
        assertEquals(0f, state.value)
    }

    @Test
    fun `coercedValueAsFraction returns correct fraction`() {
        val state = SliderState(
            initialValue = 25f,
            valueRange = 0f..100f
        )
        assertEquals(0.25f, state.coercedValueAsFraction)
    }

    @Test
    fun `coercedValueAsFraction at minimum`() {
        val state = SliderState(
            initialValue = 0f,
            valueRange = 0f..100f
        )
        assertEquals(0f, state.coercedValueAsFraction)
    }

    @Test
    fun `coercedValueAsFraction at maximum`() {
        val state = SliderState(
            initialValue = 100f,
            valueRange = 0f..100f
        )
        assertEquals(1f, state.coercedValueAsFraction)
    }

    @Test
    fun `onValueChange callback is invoked when value changes`() {
        var callbackValue = 0f
        val state = SliderState(
            initialValue = 0f,
            valueRange = 0f..1f,
            onValueChange = { callbackValue = it }
        )
        
        state.updateValue(0.5f)
        assertEquals(0.5f, callbackValue)
    }

    @Test
    fun `onValueChange callback is not invoked when value does not change`() {
        var callbackCount = 0
        val state = SliderState(
            initialValue = 0.5f,
            valueRange = 0f..1f,
            onValueChange = { callbackCount++ }
        )
        
        state.updateValue(0.5f)
        assertEquals(0, callbackCount)
    }

    @Test
    fun `custom value range works correctly`() {
        val state = SliderState(
            initialValue = 150f,
            valueRange = 100f..200f
        )
        
        assertEquals(150f, state.value)
        assertEquals(0.5f, state.coercedValueAsFraction)
        
        state.updateValueFromFraction(0.25f)
        assertEquals(125f, state.value)
    }

    @Test
    fun `zero range returns zero fraction`() {
        val state = SliderState(
            initialValue = 5f,
            valueRange = 5f..5f
        )
        assertEquals(0f, state.coercedValueAsFraction)
    }
}
