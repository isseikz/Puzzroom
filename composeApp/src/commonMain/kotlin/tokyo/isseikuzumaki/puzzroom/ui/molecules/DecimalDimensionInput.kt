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
 * Decimal dimension input molecule for width and height with decimal support (1 decimal place)
 * Displays dimensions in centimeters (cm)
 */
@Composable
fun DecimalDimensionInput(
    widthValue: String,
    heightValue: String,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        AppTextField(
            value = heightValue,
            onValueChange = onHeightChange,
            label = "Height (cm)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
    }
}
