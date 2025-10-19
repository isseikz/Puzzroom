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

/**
 * 辺の寸法を編集するModalBottomSheet
 *
 * ユーザーが辺を選択した際に表示され、辺の長さを調整できる。
 * ロック機能により、調整した辺の長さを固定できる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeDimensionSheet(
    selectedEdgeIndex: Int?,
    polygon: Polygon?,
    editState: PolygonEditState,
    onDimensionChange: (edgeIndex: Int, newLength: Int, useSimilarity: Boolean) -> Unit,
    onToggleLock: (edgeIndex: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    if (selectedEdgeIndex != null && polygon != null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier
        ) {
            EdgeDimensionContent(
                edgeIndex = selectedEdgeIndex,
                polygon = polygon,
                editState = editState,
                onDimensionChange = onDimensionChange,
                onToggleLock = onToggleLock,
                onClose = onDismiss
            )
        }
    }
}

@Composable
private fun EdgeDimensionContent(
    edgeIndex: Int,
    polygon: Polygon,
    editState: PolygonEditState,
    onDimensionChange: (edgeIndex: Int, newLength: Int, useSimilarity: Boolean) -> Unit,
    onToggleLock: (edgeIndex: Int) -> Unit,
    onClose: () -> Unit
) {
    val edgeLengths = remember(polygon) {
        PolygonGeometry.calculateEdgeLengths(polygon)
    }
    
    val currentLength = edgeLengths.getOrNull(edgeIndex) ?: 0
    val isLocked = editState.isEdgeLocked(edgeIndex)
    val useSimilarity = !editState.hasAppliedSimilarity

    var inputValue by remember(currentLength) {
        mutableStateOf(currentLength.toString())
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
                text = "辺 ${edgeIndex + 1} の長さを編集",
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

        // Current length display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "現在の長さ",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${currentLength} cm",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Length input
        OutlinedTextField(
            value = inputValue,
            onValueChange = { newValue ->
                inputValue = newValue
                val parsedValue = newValue.toIntOrNull()
                if (parsedValue != null && parsedValue > 0) {
                    isError = false
                    if (parsedValue != currentLength) {
                        onDimensionChange(edgeIndex, parsedValue, useSimilarity)
                    }
                } else {
                    isError = newValue.isNotEmpty()
                }
            },
            label = { Text("新しい長さ (cm)") },
            isError = isError,
            supportingText = if (isError) {
                { Text("正の整数を入力してください") }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Similarity mode info (only for first adjustment)
        if (useSimilarity) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📐 相似変換モード",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "初回の寸法指定により、図形全体が相似となるように他の辺も調整されます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📏 個別調整モード",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "この辺のみが調整され、他の辺の長さは維持されます。角度が調整される場合があります。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
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
                    text = "寸法をロック",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (isLocked) "ロック中 - 変更不可" else "ロック解除 - 変更可能",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onToggleLock(edgeIndex) },
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}
