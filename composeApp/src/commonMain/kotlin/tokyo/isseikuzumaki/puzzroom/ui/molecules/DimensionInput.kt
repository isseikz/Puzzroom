package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppTextField

/**
 * Dimension input molecule for width and depth
 */
@Composable
fun DimensionInput(
    widthValue: String,
    depthValue: String,
    onWidthChange: (String) -> Unit,
    onDepthChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppTextField(
            value = widthValue,
            onValueChange = onWidthChange,
            label = "Width (cm)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        AppTextField(
            value = depthValue,
            onValueChange = onDepthChange,
            label = "Depth (cm)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }
}
