package tokyo.isseikuzumaki.puzzroom.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.PolygonGeometry
import kotlin.math.roundToInt

/**
 * ポリゴンの角度を入力・編集するパネル
 */
@Composable
fun AngleInputPanel(
    polygon: Polygon?,
    onAngleChange: (vertexIndex: Int, newAngleDegrees: Double) -> Unit,
    onAutoClose: () -> Unit,
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

    val interiorAngles = remember(polygon) {
        PolygonGeometry.calculateAllInteriorAngles(polygon)
    }

    val isClosed = remember(polygon) {
        PolygonGeometry.isPolygonClosed(polygon)
    }

    val gapDistance = remember(polygon) {
        if (isClosed) 0.0 else PolygonGeometry.calculateGapDistance(polygon)
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
                    text = "Edit Angles",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(onClick = onClose) {
                    Text("Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 開いたポリゴンの警告
            if (!isClosed) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ Polygon is open",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Gap: ${gapDistance.roundToInt()} cm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onAutoClose,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Auto Close")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(count = interiorAngles.size) { index ->
                    AngleInput(
                        vertexNumber = index + 1,
                        currentAngle = interiorAngles[index],
                        onAngleChange = { newAngle ->
                            onAngleChange(index, newAngle)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "* You can change the interior angle of each vertex individually.\nAdjusting the angle may open the polygon.\nChanges are reflected immediately and saved automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 個別の角度入力フィールド
 */
@Composable
private fun AngleInput(
    vertexNumber: Int,
    currentAngle: Double,
    onAngleChange: (Double) -> Unit
) {
    var inputValue by remember(currentAngle) {
        mutableStateOf(currentAngle.roundToInt().toString())
    }
    var isError by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Vertex ${vertexNumber}")

        OutlinedTextField(
            value = inputValue,
            onValueChange = { newValue ->
                inputValue = newValue
                val parsedValue = newValue.toDoubleOrNull()
                if (parsedValue != null && parsedValue > 0 && parsedValue < 360) {
                    isError = false
                    if (parsedValue != currentAngle) {
                        onAngleChange(parsedValue)
                    }
                } else {
                    isError = newValue.isNotEmpty()
                }
            },
            label = { Text("Angle (°)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isError,
            singleLine = true,
            modifier = Modifier.width(140.dp)
        )

        Text(
            text = "Current: ${currentAngle.roundToInt()}°",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}