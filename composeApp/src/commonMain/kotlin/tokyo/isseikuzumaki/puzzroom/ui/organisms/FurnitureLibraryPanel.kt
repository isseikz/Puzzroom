package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureCategory
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppCard
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.atoms.VerticalSpacer

/**
 * Get category name in English
 */
fun FurnitureCategory.displayName(): String = when (this) {
    FurnitureCategory.LIVING -> "Living"
    FurnitureCategory.BEDROOM -> "Bedroom"
    FurnitureCategory.KITCHEN -> "Kitchen"
    FurnitureCategory.DINING -> "Dining"
    FurnitureCategory.BATHROOM -> "Bathroom"
    FurnitureCategory.OFFICE -> "Office"
    FurnitureCategory.CUSTOM -> "Custom"
}

/**
 * Furniture library panel organism
 * Displays a categorized list of furniture templates for selection
 */
@Composable
fun FurnitureLibraryPanel(
    templates: List<FurnitureTemplate>,
    selectedTemplate: FurnitureTemplate?,
    onTemplateSelected: (FurnitureTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<FurnitureCategory?>(null) }

    Column(modifier = modifier) {
        AppText(
            "Select furniture",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        // Category filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { AppText("All") }
            )
            FurnitureCategory.entries.forEach { category ->
                val count = templates.count { it.category == category }
                if (count > 0) {
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { AppText("${category.displayName()} ($count)") }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Furniture list
        val filteredTemplates = if (selectedCategory != null) {
            templates.filter { it.category == selectedCategory }
        } else {
            templates
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredTemplates) { template ->
                FurnitureTemplateCard(
                    template = template,
                    isSelected = template == selectedTemplate,
                    onClick = { onTemplateSelected(template) }
                )
            }
        }
    }
}

/**
 * Furniture template card
 */
@Composable
fun FurnitureTemplateCard(
    template: FurnitureTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppText(
                    template.name,
                    style = MaterialTheme.typography.titleSmall
                )
                AppText(
                    template.category.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            VerticalSpacer(height = 4.dp)
            AppText(
                "Size: ${template.width.value}cm × ${template.depth.value}cm",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
