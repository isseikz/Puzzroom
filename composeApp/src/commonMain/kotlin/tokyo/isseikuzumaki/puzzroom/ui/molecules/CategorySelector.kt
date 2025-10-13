package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureCategory
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.atoms.VerticalSpacer

/**
 * Category selector molecule for furniture categories
 */
@Composable
fun CategorySelector(
    selectedCategory: FurnitureCategory,
    onCategorySelected: (FurnitureCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AppText(
            text = "Category",
            style = MaterialTheme.typography.labelMedium
        )
        VerticalSpacer(height = 4.dp)
        
        // First row of categories
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FurnitureCategory.entries.take(3).forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { AppText(text = category.name) }
                )
            }
        }
        
        VerticalSpacer(height = 8.dp)
        
        // Second row of categories
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FurnitureCategory.entries.drop(3).forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { AppText(text = category.name) }
                )
            }
        }
    }
}
