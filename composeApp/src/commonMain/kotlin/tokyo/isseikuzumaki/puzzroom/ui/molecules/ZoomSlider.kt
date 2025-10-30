package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppSlider
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.atoms.SliderOrientation
import tokyo.isseikuzumaki.puzzroom.ui.state.SliderState
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme

/**
 * Zoom slider molecule for adjusting canvas scale
 * キャンバスのスケールを調整するズームスライダー
 * 
 * @param sliderState State holder for the zoom slider
 * @param modifier Modifier to be applied to the slider container
 */
@Composable
fun ZoomSlider(
    sliderState: SliderState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppText(
            text = "縮尺",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        
        AppSlider(
            state = sliderState,
            orientation = SliderOrientation.Horizontal,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
        )
        
        AppText(
            text = "${sliderState.value.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Preview
@Composable
private fun ZoomSliderPreview() {
    PuzzroomTheme {
        val sliderState = SliderState(
            initialValue = 1000f,
            valueRange = 500f..1500f
        )
        ZoomSlider(sliderState = sliderState)
    }
}
