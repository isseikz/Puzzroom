package tokyo.isseikuzumaki.puzzroom.ui.atoms

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.colors
import androidx.compose.material3.SliderState
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    orientation: Orientation = Orientation.Horizontal,
    steps: Int = 0,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
    inactiveTrackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
    thumbRadius: Float = 10f
) {
    if (orientation == Orientation.Horizontal) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = modifier,
            enabled = enabled,
            colors = colors(
                thumbColor = thumbColor,
                activeTrackColor = trackColor,
                inactiveTrackColor = inactiveTrackColor
            )
        )
    } else {
        VerticalSlider(
            modifier = modifier,
            state = AppSliderState(
                value = value,
                valueRange = valueRange,
                steps = steps,
                isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl,
                onValueChange = onValueChange
            ),
            enabled = enabled,
            interactionSource = MutableInteractionSource(),
            thumb = {
                VerticalThumb(
                    colors = colors(
                        thumbColor = thumbColor,
                        activeTrackColor = trackColor,
                        inactiveTrackColor = inactiveTrackColor
                    ),
                    interactionSource =  remember { MutableInteractionSource() },
                )
                    },
            track = {
                SliderDefaults.Track(
                    colors = colors(
                        thumbColor = thumbColor,
                        activeTrackColor = trackColor,
                        inactiveTrackColor = inactiveTrackColor
                    ),
                    sliderState = SliderState(
                        value = value,
                        valueRange = valueRange,
                        steps = steps,
                    )
                )
            }
        )
    }
}

@Stable
private class AppSliderState(
    val value: Float,
    val valueRange: ClosedFloatingPointRange<Float>,
    val steps: Int,
    val isRtl: Boolean,
    val onValueChange: (Float) -> Unit
): DraggableState {

    private var thumbWidth: Float = 0f
    private var totalWidth: Int = 0
    private var trackHeight: Float = 0f

    private val dragScope: DragScope = object : DragScope {
        override fun dragBy(pixels: Float) {
            val offsetDelta = if (isRtl) -pixels else pixels
            onValueChange(rawOffsetToValue(offsetDelta))
        }
    }

    fun updateDimensions(newTrackHeight: Float, newTotalWidth: Int) {
        trackHeight = newTrackHeight
        totalWidth = newTotalWidth
    }

    fun updateThumbWidth(width: Float) {
        thumbWidth = width
    }

    val coercedValueAsFraction: Float
        get() {
            val range = valueRange.endInclusive - valueRange.start
            return if (range == 0f) 0f
            else ((value - valueRange.start) / range).coerceIn(0f, 1f)
        }

    private fun rawOffsetToValue(offset: Float): Float {
        val newOffset = (trackHeight * coercedValueAsFraction + offset)
        val fraction = (newOffset / trackHeight).coerceIn(0f, 1f)
        return valueFromFraction(fraction)
    }

    private fun valueFromFraction(fraction: Float): Float {
        val range = valueRange.endInclusive - valueRange.start
        val value = valueRange.start + fraction * range

        return if (steps > 0) {
            val stepSize = range / (steps + 1)
            val roundedValue = ((value - valueRange.start) / stepSize).roundToInt() * stepSize + valueRange.start
            roundedValue.coerceIn(valueRange)
        } else {
            value.coerceIn(valueRange)
        }
    }

    fun onPress(position: androidx.compose.ui.geometry.Offset) {
        val newFraction = (position.y / trackHeight).coerceIn(0f, 1f)
        onValueChange(valueFromFraction(newFraction))
    }

    var isDragging: Boolean = false
        private set

    fun gestureEndAction() {
        isDragging = false
    }

    override fun dispatchRawDelta(delta: Float) {
        dragScope.dragBy(delta)
    }

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit,
    ) {
        isDragging = true
        dragScope.block()
    }
}

private enum class AppSliderId {
    THUMB, TRACK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerticalSlider(
    modifier: Modifier,
    state: AppSliderState,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    thumb: @Composable (AppSliderState) -> Unit,
    track: @Composable (AppSliderState) -> Unit
) {
    val press = Modifier.sliderTapModifier(state, interactionSource, enabled)
    val drag =
        Modifier.draggable(
            orientation = Orientation.Vertical,
            reverseDirection = !state.isRtl,
            enabled = enabled,
            interactionSource = interactionSource,
            onDragStopped = {
                state.gestureEndAction()
            },
            startDragImmediately = state.isDragging,
            state = state
        )

    Layout(
        {
            Box(
                modifier =
                    Modifier.layoutId(AppSliderId.THUMB).wrapContentHeight().onSizeChanged {
                        state.updateThumbWidth(it.height.toFloat())
                    }
            ) {
                thumb(state)
            }
            Box(modifier = Modifier.layoutId(AppSliderId.TRACK)) { track(state) }
        },
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .requiredSizeIn(minWidth = 16.dp, minHeight = 4.dp)
                .focusable(enabled, interactionSource)
                .then(press)
                .then(drag)
    ) { measurables, constraints ->
        val thumbPlaceable =
            measurables.fastFirst { it.layoutId == AppSliderId.THUMB }.measure(constraints)

        val trackPlaceable =
            measurables
                .fastFirst { it.layoutId == AppSliderId.TRACK }
                .measure(constraints.offset(horizontal = -thumbPlaceable.width).copy(minHeight = 0))

        val sliderWidth = thumbPlaceable.width + trackPlaceable.width
        val sliderHeight = max(trackPlaceable.height, thumbPlaceable.height)

        state.updateDimensions(trackPlaceable.height.toFloat(), sliderWidth)

        val trackOffsetX = thumbPlaceable.width / 2
        val thumbOffsetX = (trackPlaceable.width.toFloat() * state.coercedValueAsFraction).roundToInt()
        val trackOffsetY = (sliderHeight - trackPlaceable.height) / 2
        val thumbOffsetY = (sliderHeight - thumbPlaceable.height) / 2

        layout(sliderWidth, sliderHeight) {
            trackPlaceable.placeRelative(trackOffsetY, trackOffsetX)
            thumbPlaceable.placeRelative(thumbOffsetY, thumbOffsetX)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Stable
private fun Modifier.sliderTapModifier(
    state: AppSliderState,
    interactionSource: MutableInteractionSource,
    enabled: Boolean
) =
    if (enabled) {
        pointerInput(state, interactionSource) {
            detectTapGestures(
                onPress = {
                    state.onPress(it)
                },
                onTap = {
                    state.dispatchRawDelta(0f)
                    state.gestureEndAction()
                }
            )
        }
    } else {
        this
    }

@Composable
private fun VerticalThumb(
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    colors: SliderColors = colors(),
    enabled: Boolean = true,
    thumbSize: DpSize = DpSize(20.dp, 5.dp)
) {
    val interactions = remember { mutableStateListOf<Interaction>() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> interactions.add(interaction)
                is PressInteraction.Release -> interactions.remove(interaction.press)
                is PressInteraction.Cancel -> interactions.remove(interaction.press)
                is DragInteraction.Start -> interactions.add(interaction)
                is DragInteraction.Stop -> interactions.remove(interaction.start)
                is DragInteraction.Cancel -> interactions.remove(interaction.start)
            }
        }
    }

    val size =
        if (interactions.isNotEmpty()) {
            thumbSize.copy(width = thumbSize.width / 2)
        } else {
            thumbSize
        }
    Spacer(
        modifier
            .size(size)
            .hoverable(interactionSource = interactionSource)
            .background(colors.thumbColor)
    )
}


@Preview
@Composable
fun AppSliderPlayground() {
    MaterialTheme {
        AppSlider(
            orientation = Orientation.Horizontal,
            value = 30f,
            onValueChange = {},
            valueRange = 0f..100f,
            modifier = Modifier.width(100.dp).height(20.dp)
        )
    }
}

@Preview
@Composable
fun VerticalSliderPlayground() {
    MaterialTheme {
        AppSlider(
            orientation = Orientation.Vertical,
            value = 30f,
            onValueChange = {},
            valueRange = 0f..100f,
            modifier = Modifier.width(20.dp).height(100.dp)
        )
    }
}
