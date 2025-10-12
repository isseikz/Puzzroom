package tokyo.isseikuzumaki.puzzroom.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter

/**
 * Toolbar for furniture placement
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
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Display furniture name
            Text(
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
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirm placement")
                }
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
        label = { Text(label) },
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
            Text(
                text = "Rotation angle:",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${rotation.toInt()}°",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(48.dp)
            )
            Slider(
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
                OutlinedButton(
                    onClick = { onRotationChange(angle.toFloat()) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("${angle}°", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
