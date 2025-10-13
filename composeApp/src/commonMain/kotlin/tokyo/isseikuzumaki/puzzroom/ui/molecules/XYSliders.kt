package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppSlider


@Composable
fun XYSliders(
    value: Pair<Float, Float>,
    onValueChange: (Pair<Float, Float>) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = {
            AppSlider(
                orientation = Orientation.Vertical,
                value = value.second,
                onValueChange = { onValueChange(Pair(value.first, it)) },
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier
            )
            AppSlider(
                orientation = Orientation.Vertical,
                value = value.second,
                onValueChange = { onValueChange(Pair(value.first, it)) },
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier
            )
            AppSlider(
                value = value.first,
                onValueChange = { onValueChange(Pair(it, value.second)) },
                valueRange = valueRange,
                steps = steps
            )
            AppSlider(
                value = value.first,
                onValueChange = { onValueChange(Pair(it, value.second)) },
                valueRange = valueRange,
                steps = steps
            )
            Box { content() }
        }
    ) { measurables, constraints ->
        val contentPlaceable = measurables[4].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val contentWidth = contentPlaceable.width
        val contentHeight = contentPlaceable.height

        // Measure all sliders once and store the placeables
        val leftSliderPlaceable = measurables[0].measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxWidth = contentWidth, maxHeight = contentHeight)
        )
        val rightSliderPlaceable = measurables[1].measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxWidth = contentWidth, maxHeight = contentHeight)
        )
        val topSliderPlaceable = measurables[2].measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxWidth = contentWidth, maxHeight = contentHeight)
        )
        val bottomSliderPlaceable = measurables[3].measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxWidth = contentWidth, maxHeight = contentHeight)
        )

        val sliderWidth = topSliderPlaceable.width
        val sliderHeight = topSliderPlaceable.height

        val layoutWidth = sliderWidth + leftSliderPlaceable.width * 2
        val layoutHeight = contentHeight + sliderHeight * 2

        layout(layoutWidth, layoutHeight) {
            leftSliderPlaceable.placeRelative(0, topSliderPlaceable.height)
            rightSliderPlaceable.placeRelative(leftSliderPlaceable.width + contentWidth, topSliderPlaceable.height)
            topSliderPlaceable.placeRelative(leftSliderPlaceable.width, 0)
            bottomSliderPlaceable.placeRelative(leftSliderPlaceable.width, contentHeight + topSliderPlaceable.height)
            contentPlaceable.placeRelative(leftSliderPlaceable.width, topSliderPlaceable.height)
        }
    }
}

@Preview
@Composable
fun XYSlidersPreview() {
    MaterialTheme {
        XYSliders(
            value = Pair(10f, 100f),
            onValueChange = {},
            valueRange = 0f..100f,
            modifier = Modifier,
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(200.dp)
                    .background(color = Color.Yellow)
                    .then(Modifier)
            )
        }
    }
}
