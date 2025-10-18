package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppCard
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText

/**
 * Shape template for furniture
 */
enum class ShapeTemplate {
    RECTANGLE,
    CIRCLE,
    L_SHAPE,
    CUSTOM;

    fun displayName(): String = when (this) {
        RECTANGLE -> "Rectangle"
        CIRCLE -> "Circle"
        L_SHAPE -> "L-Shape"
        CUSTOM -> "Custom"
    }
}

/**
 * Shape selector molecule for selecting furniture shape templates
 */
@Composable
fun ShapeSelector(
    selectedShape: ShapeTemplate,
    onShapeSelected: (ShapeTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AppText(
            text = "Select Shape",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ShapeTemplate.entries) { shape ->
                ShapeTemplateCard(
                    shape = shape,
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
private fun ShapeTemplateCard(
    shape: ShapeTemplate,
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
