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
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import tokyo.isseikuzumaki.puzzroom.domain.Furniture
import tokyo.isseikuzumaki.puzzroom.domain.Point
import tokyo.isseikuzumaki.puzzroom.domain.Polygon
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppCard
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.molecules.selectionBorder

/**
 * Shape selector molecule for selecting furniture shape templates
 */
@Composable
fun FurnitureSelector(
    items: List<Furniture>,
    selectedShape: Furniture?,
    onShapeSelected: (Furniture) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AppText(
            text = "Place Item",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { shape ->
                FurnitureTemplateCard(
                    item = shape,
                    isSelected = shape == selectedShape,
                    onClick = { onShapeSelected(shape) }
                )
            }
        }
    }
}

/**
 * Shape template card for individual shape selection
 */
@Composable
private fun FurnitureTemplateCard(
    item: Furniture,
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
                // TODO: Replace with actual SVG icon when available
                AppText(
                    text = item.name.take(1),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                AppText(
                    text = item.name,
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
private fun Preview() {
    val furnitureList = listOf(
        Furniture(name = "Desk", shape = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(120.cm(), 0.cm()),
                Point(120.cm(), 60.cm()),
                Point(0.cm(), 60.cm())
            ),
        )),
        Furniture(name = "Piano", shape = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(150.cm(), 0.cm()),
                Point(150.cm(), 70.cm()),
                Point(0.cm(), 70.cm())
            ),
        )),
        Furniture(name = "Wardrobe", shape = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(80.cm(), 0.cm()),
                Point(80.cm(), 200.cm()),
                Point(0.cm(), 200.cm())
            ),
        )),
        Furniture(name = "Bookshelf", shape = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(90.cm(), 0.cm()),
                Point(90.cm(), 180.cm()),
                Point(0.cm(), 180.cm())
            ),
        )),
        Furniture(name = "TV", shape = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(100.cm(), 0.cm()),
                Point(100.cm(), 10.cm()),
                Point(0.cm(), 10.cm())
            ),
        )),
        Furniture(name = "TV Stand", shape = Polygon(
            points = listOf(
                Point(0.cm(), 0.cm()),
                Point(120.cm(), 0.cm()),
                Point(120.cm(), 50.cm()),
                Point(0.cm(), 50.cm())
            ),
        )),
    )
    FurnitureSelector(
        items = furnitureList,
        selectedShape = furnitureList[2],
        onShapeSelected = {}
    )
}
