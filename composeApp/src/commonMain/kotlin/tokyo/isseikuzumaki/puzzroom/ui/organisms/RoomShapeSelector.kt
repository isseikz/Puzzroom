package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.domain.RoomShapeType
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppCard
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.molecules.selectionBorder

/**
 * Room shape selector organism for selecting room shape types (walls, doors)
 */
@Composable
fun RoomShapeSelector(
    items: List<RoomShapeType>,
    selectedShape: RoomShapeType?,
    onShapeSelected: (RoomShapeType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AppText(
            text = "Place Room Element",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { shape ->
                RoomShapeCard(
                    shape = shape,
                    isSelected = shape == selectedShape,
                    onClick = { onShapeSelected(shape) }
                )
            }
        }
    }
}

/**
 * Room shape card for individual shape selection
 */
@Composable
private fun RoomShapeCard(
    shape: RoomShapeType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        onClick = onClick,
        modifier = modifier
            .width(80.dp)
            .height(80.dp)
            .selectionBorder(isSelected),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // TODO: Replace with actual icon when available
                AppText(
                    text = shape.name.take(1),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                AppText(
                    text = shape.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun RoomShapeSelectorPreview() {
    val shapeTypes = listOf(
        RoomShapeType.WALL,
        RoomShapeType.DOOR,
        RoomShapeType.WINDOW
    )
    RoomShapeSelector(
        items = shapeTypes,
        selectedShape = RoomShapeType.WALL,
        onShapeSelected = {}
    )
}
