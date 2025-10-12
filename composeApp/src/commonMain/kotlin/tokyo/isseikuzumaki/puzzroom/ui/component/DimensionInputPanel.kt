package tokyo.isseikuzumaki.puzzroom.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.PolygonGeometry

/**
 * ポリゴンの寸法を入力・編集するパネル
 */
@Composable
fun DimensionInputPanel(
    polygon: Polygon?,
    onDimensionChange: (edgeIndex: Int, newLength: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (polygon == null) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Please select a room",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val edgeLengths = remember(polygon) {
        PolygonGeometry.calculateEdgeLengths(polygon)
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Dimensions",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(onClick = onClose) {
                    Text("Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(edgeLengths.size) { index ->
                    DimensionInput(
                        edgeNumber = index + 1,
                        currentLength = edgeLengths[index],
                        onLengthChange = { newLength ->
                            onDimensionChange(index, newLength)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "* You can change the length of each side individually.\nChanges are reflected immediately and saved automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 個別の寸法入力フィールド
 */
@Composable
private fun DimensionInput(
    edgeNumber: Int,
    currentLength: Int,
    onLengthChange: (Int) -> Unit
) {
    var inputValue by remember(currentLength) {
        mutableStateOf(currentLength.toString())
    }
    var isError by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Side ${edgeNumber}")

        OutlinedTextField(
            value = inputValue,
            onValueChange = { newValue ->
                inputValue = newValue
                val parsedValue = newValue.toIntOrNull()
                if (parsedValue != null && parsedValue > 0) {
                    isError = false
                    if (parsedValue != currentLength) {
                        onLengthChange(parsedValue)
                    }
                } else {
                    isError = newValue.isNotEmpty()
                }
            },
            label = { Text("Length (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isError,
            singleLine = true,
            modifier = Modifier.width(140.dp)
        )

        Text(
            text = "Current: ${currentLength}cm",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}