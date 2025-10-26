package tokyo.isseikuzumaki.puzzroom.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/**
 * State holder for slider component
 * スライダーコンポーネントの状態を管理するクラス
 * 
 * @param initialValue Initial value of the slider (スライダーの初期値)
 * @param valueRange Range of values the slider can take (スライダーが取りうる値の範囲)
 * @param onValueChange Callback when the value changes (値が変更されたときのコールバック)
 */
class SliderState(
    initialValue: Float = 0f,
    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    val onValueChange: (Float) -> Unit = {},
    val onSliderReleased: () -> Unit = {}
) {
    /**
     * Current value of the slider (スライダーの現在値)
     */
    var value by mutableFloatStateOf(initialValue.coerceIn(valueRange))
        private set

    /**
     * Value as a fraction between 0f and 1f (0fから1fの間の割合としての値)
     */
    val coercedValueAsFraction: Float
        get() {
            val range = valueRange.endInclusive - valueRange.start
            return if (range > 0f) {
                ((value - valueRange.start) / range).coerceIn(0f, 1f)
            } else {
                0f
            }
        }

    /**
     * Update the slider value (スライダーの値を更新)
     */
    fun updateValue(newValue: Float) {
        val coercedValue = newValue.coerceIn(valueRange)
        if (value != coercedValue) {
            value = coercedValue
            onValueChange(coercedValue)
        }
    }

    /**
     * Update the slider value from a fraction (0f..1f) (割合(0f..1f)から値を更新)
     */
    fun updateValueFromFraction(fraction: Float) {
        val range = valueRange.endInclusive - valueRange.start
        val newValue = valueRange.start + (fraction.coerceIn(0f, 1f) * range)
        updateValue(newValue)
    }
}
