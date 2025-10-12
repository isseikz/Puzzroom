package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.ui.atoms.*
import tokyo.isseikuzumaki.puzzroom.ui.molecules.TitleWithSubtitle

/**
 * Polygon list panel organism
 * Displays a list of polygons/rooms with selection, editing, and deletion actions
 */
@Composable
fun PolygonListPanel(
    polygons: List<Polygon>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            AppText(
                text = "Room List",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (polygons.isEmpty()) {
                AppText(
                    text = "No rooms are registered.\nPlease click on the canvas to create a room.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(polygons) { index, polygon ->
                        PolygonCard(
                            roomNumber = index + 1,
                            polygon = polygon,
                            isSelected = index == selectedIndex,
                            onSelect = { onSelect(index) },
                            onEdit = { onEdit(index) },
                            onDelete = { onDelete(index) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual polygon card
 */
@Composable
private fun PolygonCard(
    roomNumber: Int,
    polygon: Polygon,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TitleWithSubtitle(
                title = "Room $roomNumber",
                subtitle = "Number of vertices: ${polygon.points.size}",
                modifier = Modifier.weight(1f)
            )

            Row {
                AppIconButton(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                )
                AppIconButton(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
