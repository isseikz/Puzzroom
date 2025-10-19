package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.domain.PolygonGeometry
import tokyo.isseikuzumaki.puzzroom.ui.state.PolygonEditState
import kotlin.math.roundToInt

/**
 * 角度を編集するModalBottomSheet
 *
 * ユーザーが頂点を選択した際に表示され、内角を調整できる。
 * 90度拘束（ロック）機能をサポート。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AngleEditSheet(
    selectedVertexIndex: Int?,
    polygon: Polygon?,
    editState: PolygonEditState,
    onAngleChange: (vertexIndex: Int, newAngleDegrees: Double) -> Unit,
    onToggleLock: (vertexIndex: Int) -> Unit,
    onLockTo90: (vertexIndex: Int) -> Unit,
    onAutoClose: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    if (selectedVertexIndex != null && polygon != null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier
        ) {
            AngleEditContent(
                vertexIndex = selectedVertexIndex,
                polygon = polygon,
                editState = editState,
                onAngleChange = onAngleChange,
                onToggleLock = onToggleLock,
                onLockTo90 = onLockTo90,
                onAutoClose = onAutoClose,
                onClose = onDismiss
            )
        }
    }
}

@Composable
private fun AngleEditContent(
    vertexIndex: Int,
    polygon: Polygon,
    editState: PolygonEditState,
    onAngleChange: (vertexIndex: Int, newAngleDegrees: Double) -> Unit,
    onToggleLock: (vertexIndex: Int) -> Unit,
    onLockTo90: (vertexIndex: Int) -> Unit,
    onAutoClose: () -> Unit,
    onClose: () -> Unit
) {
    val interiorAngles = remember(polygon) {
        PolygonGeometry.calculateAllInteriorAngles(polygon)
    }
    
    val currentAngle = interiorAngles.getOrNull(vertexIndex) ?: 0.0
    val isLocked = editState.isAngleLocked(vertexIndex)
    val isNear90 = (currentAngle - 90.0).let { it >= -5.0 && it <= 5.0 }

    var inputValue by remember(currentAngle) {
        mutableStateOf(currentAngle.roundToInt().toString())
    }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "頂点 ${vertexIndex + 1} の角度を編集",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = "閉じる"
                )
            }
        }

        Divider()

        // Current angle display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "現在の内角",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${currentAngle.roundToInt()}°",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Angle input
        OutlinedTextField(
            value = inputValue,
            onValueChange = { newValue ->
                inputValue = newValue
                val parsedValue = newValue.toDoubleOrNull()
                if (parsedValue != null && parsedValue > 0 && parsedValue < 360) {
                    isError = false
                    if (parsedValue != currentAngle) {
                        onAngleChange(vertexIndex, parsedValue)
                    }
                } else {
                    isError = newValue.isNotEmpty()
                }
            },
            label = { Text("新しい角度 (度)") },
            isError = isError,
            supportingText = if (isError) {
                { Text("0より大きく360未満の値を入力してください") }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // 90-degree lock button
        if (isNear90 || isLocked) {
            Button(
                onClick = { 
                    onLockTo90(vertexIndex)
                    if (!isLocked) {
                        onAngleChange(vertexIndex, 90.0)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLocked && currentAngle.roundToInt() == 90) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            ) {
                Icon(
                    imageVector = if (isLocked && currentAngle.roundToInt() == 90) {
                        Icons.Default.Lock
                    } else {
                        Icons.Default.LockOpen
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("90°に拘束")
            }
        } else {
            OutlinedButton(
                onClick = { 
                    onAngleChange(vertexIndex, 90.0)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("90°に設定")
            }
        }

        Divider()

        // Warning about opening polygon and auto-close button
        val isClosed = remember(polygon) {
            PolygonGeometry.isPolygonClosed(polygon, tolerance = 1.0)
        }
        val gapDistance = remember(polygon) {
            if (!isClosed) PolygonGeometry.calculateGapDistance(polygon) else 0.0
        }
        
        if (!isClosed) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ ポリゴンが開いています",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "ギャップ: ${gapDistance.roundToInt()} cm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = onAutoClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("自動で閉じる")
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "⚠️ 注意",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "角度を調整すると、多角形が開く可能性があります。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        Divider()

        // Lock toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "角度をロック",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (isLocked) "ロック中 - 変更不可" else "ロック解除 - 変更可能",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onToggleLock(vertexIndex) },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isLocked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isLocked) "ロックを解除" else "ロック",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Complete constraint info
        val isFullyConstrained = editState.isFullyConstrained(polygon.points.size)
        if (isFullyConstrained) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✅",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "完全拘束条件を満たしています",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
