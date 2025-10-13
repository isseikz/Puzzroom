package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppCard
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppOutlinedButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppSlider
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText

/**
 * Furniture placement toolbar organism
 * Provides controls for adjusting furniture size and rotation before placement
 */
@Composable
fun FurniturePlacementToolbar(
    furnitureName: String,
    width: Centimeter,
    depth: Centimeter,
    rotation: Float,
    onWidthChange: (Centimeter) -> Unit,
    onDepthChange: (Centimeter) -> Unit,
    onRotationChange: (Float) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Display furniture name
            AppText(
                text = "Placing: $furnitureName",
                style = MaterialTheme.typography.titleMedium
            )

            // Adjust size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SizeInputField(
                    label = "Width (cm)",
                    value = width.value,
                    onValueChange = { onWidthChange(Centimeter(it)) },
                    modifier = Modifier.weight(1f)
                )
                SizeInputField(
                    label = "Depth (cm)",
                    value = depth.value,
                    onValueChange = { onDepthChange(Centimeter(it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Adjust rotation
            RotationControl(
                rotation = rotation,
                onRotationChange = onRotationChange
            )

            // Confirm/Cancel buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppOutlinedButton(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                )
                AppButton(
                    text = "Confirm placement",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Size input field
 */
@Composable
private fun SizeInputField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            textValue = newValue
            newValue.toIntOrNull()?.let { intValue ->
                if (intValue in 1..1000) {
                    onValueChange(intValue)
                }
            }
        },
        label = { AppText(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

/**
 * Rotation control
 */
@Composable
private fun RotationControl(
    rotation: Float,
    onRotationChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Rotation angle display and slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppText(
                text = "Rotation angle:",
                style = MaterialTheme.typography.bodyMedium
            )
            AppText(
                text = "${rotation.toInt()}°",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(48.dp)
            )
            AppSlider(
                value = rotation,
                onValueChange = onRotationChange,
                valueRange = 0f..359f,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick rotation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0, 45, 90, 135, 180, 225, 270, 315).forEach { angle ->
                AppOutlinedButton(
                    text = "${angle}°",
                    onClick = { onRotationChange(angle.toFloat()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
