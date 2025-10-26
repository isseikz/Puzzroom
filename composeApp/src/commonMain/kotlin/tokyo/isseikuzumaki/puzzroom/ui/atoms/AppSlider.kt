package tokyo.isseikuzumaki.puzzroom.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.ui.PreviewTemplate
import tokyo.isseikuzumaki.puzzroom.ui.organisms.AdaptiveNineGrid
import tokyo.isseikuzumaki.puzzroom.ui.state.SliderState

/**
 * Slider orientation enum
 * スライダーの向きを表す列挙型
 */
enum class SliderOrientation {
    Horizontal,
    Vertical
}

/**
 * Slot identifiers for slider components
 * スライダーコンポーネントのスロット識別子
 */
private enum class SliderSlot {
    Track,
    Thumb
}

/**
 * Generic slider atom component that supports both horizontal and vertical orientations
 * 水平・垂直両方向に対応した汎用スライダーコンポーネント
 * 
 * This component follows the design principles outlined in the vertical slider specification:
 * - Uses Layout composable for custom measurement and placement
 * - Supports dynamic orientation via SliderOrientation parameter
 * - Implements Slot API for thumb and track customization
 * - Manages state through SliderState class
 * 
 * @param state Slider state holder (スライダーの状態ホルダー)
 * @param modifier Modifier for the slider container (スライダーコンテナのModifier)
 * @param orientation Orientation of the slider (スライダーの向き)
 * @param thumb Custom composable for the thumb (つまみのカスタムComposable)
 * @param track Custom composable for the track (軌道のカスタムComposable)
 */
@Composable
fun AppSlider(
    state: SliderState,
    modifier: Modifier = Modifier,
    orientation: SliderOrientation = SliderOrientation.Horizontal,
    thumb: @Composable () -> Unit = { DefaultThumb() },
    track: @Composable () -> Unit = { DefaultTrack(orientation) }
) {

    Layout(
        content = {
            // Track (軌道)
            Box(modifier = Modifier.layoutId(SliderSlot.Track)) {
                track()
            }
            // Thumb (つまみ)
            Box(modifier = Modifier.layoutId(SliderSlot.Thumb)) {
                thumb()
            }
        },
        modifier = modifier.pointerInput(orientation) {
            detectDragGestures(
                onDragStart = { /* No-op */ },
                onDragEnd = {
                    state.onSliderReleased()
                },
                onDragCancel = { /* No-op */ },
                onDrag = { change, dragAmount ->
                    change.consume()
                    when (orientation) {
                        SliderOrientation.Horizontal -> {
                            // Horizontal: use X coordinate
                            val trackWidth = size.width.toFloat()
                            if (trackWidth > 0) {
                                val delta = dragAmount.x / trackWidth
                                val newFraction = (state.coercedValueAsFraction + delta).coerceIn(0f, 1f)
                                state.updateValueFromFraction(newFraction)
                            }
                        }
                        SliderOrientation.Vertical -> {
                            // Vertical: use Y coordinate (inverted - top is max, bottom is min)
                            val trackHeight = size.height.toFloat()
                            if (trackHeight > 0) {
                                val delta = -dragAmount.y / trackHeight // Inverted for natural feel
                                val newFraction = (state.coercedValueAsFraction + delta).coerceIn(0f, 1f)
                                state.updateValueFromFraction(newFraction)
                            }
                        }
                    }
                }
            )
        }
    ) { measurables, constraints ->
        // Find track and thumb measurables
        val trackMeasurable = measurables.first { it.layoutId == SliderSlot.Track }
        val thumbMeasurable = measurables.first { it.layoutId == SliderSlot.Thumb }

        // Measure thumb first to know its size
        val thumbPlaceable = thumbMeasurable.measure(Constraints())

        // Measure track based on orientation
        val trackPlaceable = when (orientation) {
            SliderOrientation.Horizontal -> {
                // Horizontal: track width is constrained, height wraps content
                val trackWidth = maxOf(0, constraints.maxWidth - thumbPlaceable.width)
                trackMeasurable.measure(
                    Constraints(
                        minWidth = trackWidth,
                        maxWidth = trackWidth,
                        minHeight = 0,
                        maxHeight = constraints.maxHeight
                    )
                )
            }
            SliderOrientation.Vertical -> {
                // Vertical: track height is constrained, width wraps content
                val trackHeight = maxOf(0, constraints.maxHeight - thumbPlaceable.height)
                trackMeasurable.measure(
                    Constraints(
                        minWidth = 0,
                        maxWidth = constraints.maxWidth,
                        minHeight = trackHeight,
                        maxHeight = trackHeight
                    )
                )
            }
        }

        // Calculate total size based on orientation
        val (totalWidth, totalHeight) = when (orientation) {
            SliderOrientation.Horizontal -> {
                val width = thumbPlaceable.width + trackPlaceable.width
                val height = maxOf(trackPlaceable.height, thumbPlaceable.height)
                Pair(width, height)
            }
            SliderOrientation.Vertical -> {
                val width = maxOf(trackPlaceable.width, thumbPlaceable.width)
                val height = thumbPlaceable.height + trackPlaceable.height
                Pair(width, height)
            }
        }

        // Coerce to constraints
        val layoutWidth = totalWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(layoutWidth, layoutHeight) {
            when (orientation) {
                SliderOrientation.Horizontal -> {
                    // Horizontal layout
                    val trackOffsetX = thumbPlaceable.width / 2
                    val trackOffsetY = (layoutHeight - trackPlaceable.height) / 2

                    // Place track
                    trackPlaceable.placeRelative(trackOffsetX, trackOffsetY)

                    // Calculate thumb position based on value
                    val thumbOffsetX = (trackPlaceable.width * state.coercedValueAsFraction).toInt()
                    val thumbOffsetY = (layoutHeight - thumbPlaceable.height) / 2

                    // Place thumb
                    thumbPlaceable.placeRelative(thumbOffsetX, thumbOffsetY)
                }
                SliderOrientation.Vertical -> {
                    // Vertical layout
                    val trackOffsetX = (layoutWidth - trackPlaceable.width) / 2
                    val trackOffsetY = thumbPlaceable.height / 2

                    // Place track
                    trackPlaceable.placeRelative(trackOffsetX, trackOffsetY)

                    // Calculate thumb position based on value (inverted - top is max)
                    val thumbOffsetX = (layoutWidth - thumbPlaceable.width) / 2
                    val thumbOffsetY = (trackPlaceable.height * (1f - state.coercedValueAsFraction)).toInt()

                    // Place thumb
                    thumbPlaceable.placeRelative(thumbOffsetX, thumbOffsetY)
                }
            }
        }
    }
}

/**
 * Default thumb implementation
 * デフォルトのつまみ実装
 */
@Composable
private fun DefaultThumb(
    size: Dp = 20.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape)
    )
}

/**
 * Default track implementation
 * デフォルトの軌道実装
 */
@Composable
private fun DefaultTrack(
    orientation: SliderOrientation,
    thickness: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.secondary
) {
    when (orientation) {
        SliderOrientation.Horizontal -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(thickness)
                    .background(color)
            )
        }
        SliderOrientation.Vertical -> {
            Box(
                modifier = Modifier
                    .width(thickness)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

/**
 * Preview: Horizontal slider
 * プレビュー: 水平スライダー
 */
@Preview
@Composable
fun AppSliderPreview_Horizontal() {
    PreviewTemplate {
        val sliderState = remember { 
            SliderState(
                initialValue = 0.5f,
                valueRange = 0f..1f
            )
        }
        
        Box(modifier = Modifier.size(300.dp, 100.dp)) {
            AppSlider(
                state = sliderState,
                orientation = SliderOrientation.Horizontal,
                modifier = Modifier.width(250.dp).height(40.dp)
            )
        }
    }
}

/**
 * Preview: Vertical slider
 * プレビュー: 垂直スライダー
 */
@Preview
@Composable
fun AppSliderPreview_Vertical() {
    PreviewTemplate {
        val sliderState = remember { 
            SliderState(
                initialValue = 0.7f,
                valueRange = 0f..1f
            )
        }
        
        Box(modifier = Modifier.size(100.dp, 300.dp)) {
            AppSlider(
                state = sliderState,
                orientation = SliderOrientation.Vertical,
                modifier = Modifier.width(40.dp).height(250.dp)
            )
        }
    }
}

/**
 * Preview: Custom styled horizontal slider
 * プレビュー: カスタムスタイルの水平スライダー
 */
@Preview
@Composable
fun AppSliderPreview_CustomHorizontal() {
    PreviewTemplate {
        val sliderState = remember { 
            SliderState(
                initialValue = 0.3f,
                valueRange = 0f..1f
            )
        }
        
        Box(modifier = Modifier.size(300.dp, 100.dp)) {
            AppSlider(
                state = sliderState,
                orientation = SliderOrientation.Horizontal,
                modifier = Modifier.width(250.dp).height(40.dp),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                    )
                },
                track = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            )
        }
    }
}

/**
 * Preview: Custom styled vertical slider
 * プレビュー: カスタムスタイルの垂直スライダー
 */
@Preview
@Composable
fun AppSliderPreview_CustomVertical() {
    PreviewTemplate {
        val sliderState = remember { 
            SliderState(
                initialValue = 0.6f,
                valueRange = 0f..1f
            )
        }
        
        Box(modifier = Modifier.size(100.dp, 300.dp)) {
            AppSlider(
                state = sliderState,
                orientation = SliderOrientation.Vertical,
                modifier = Modifier.width(40.dp).height(250.dp),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                    )
                },
                track = {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            )
        }
    }
}


/**
 * Preview: Vertical sliders in left and right panels
 * プレビュー: 左右のパネルに垂直スライダーを配置
 */
@Preview
@Composable
fun AdaptiveNineGridPreview_VerticalSliders() {
    PreviewTemplate {
        Box(modifier = Modifier.size(450.dp).padding(16.dp)) {
            val leftSliderState = remember {
                SliderState(
                    initialValue = 0.7f,
                    valueRange = 0f..1f
                )
            }
            val rightSliderState = remember {
                SliderState(
                    initialValue = 0.3f,
                    valueRange = 0f..1f
                )
            }
            val topBottomSliderState = remember {
                SliderState(
                    initialValue = 0.5f,
                    valueRange = 0f..1f
                )
            }

            AdaptiveNineGrid(
                commonSize = 56.dp,
                topLeftContent = {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(Icons.Default.Settings, "Settings", tint = Color.White)
                    }
                },
                topContent = {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        AppSlider(
                            state = topBottomSliderState,
                            orientation = SliderOrientation.Horizontal,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                topRightContent = {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(Icons.Default.Info, "Info", tint = Color.White)
                    }
                },
                leftContent = {
                    // Left vertical slider
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.tertiary)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppSlider(
                            state = leftSliderState,
                            orientation = SliderOrientation.Vertical,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                centerContent = {
                    Box(
                        Modifier.fillMaxSize().background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            "Main Content\n\nLeft: ${(leftSliderState.value * 100).toInt()}%\nRight: ${(rightSliderState.value * 100).toInt()}%\nTop/Bottom: ${(topBottomSliderState.value * 100).toInt()}%",
                            color = Color.DarkGray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                },
                rightContent = {
                    // Right vertical slider
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.tertiary)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppSlider(
                            state = rightSliderState,
                            orientation = SliderOrientation.Vertical,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                bottomLeftContent = {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(Icons.Default.Delete, "Delete", tint = Color.White)
                    }
                },
                bottomContent = {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        AppSlider(
                            state = topBottomSliderState,
                            orientation = SliderOrientation.Horizontal,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                bottomRightContent = {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(Icons.Default.Favorite, "Favorite", tint = Color.White)
                    }
                }
            )
        }
    }
}
