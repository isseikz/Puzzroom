package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.ui.atoms.*
import tokyo.isseikuzumaki.puzzroom.ui.molecules.*
import tokyo.isseikuzumaki.puzzroom.ui.organisms.*
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme

/**
 * Design system showcase page
 * Demonstrates all atoms, molecules, and organisms with the warm color theme
 */
@Preview
@Composable
fun DesignSystemShowcase() {
    PuzzroomTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            AppText(
                text = "Puzzroom Design System",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Color Palette Section
            SectionTitle("Color Palette - Warm Theme")
            ColorPaletteShowcase()
            
            HorizontalDivider()
            
            // Atoms Section
            SectionTitle("Atoms")
            AtomsShowcase()
            
            HorizontalDivider()
            
            // Molecules Section
            SectionTitle("Molecules")
            MoleculesShowcase()
            
            HorizontalDivider()
            
            // Organisms Section
            SectionTitle("Organisms")
            OrganismsShowcase()
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    AppText(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun ColorPaletteShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorSwatch("Primary", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        ColorSwatch("Secondary", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
        ColorSwatch("Tertiary", MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
        ColorSwatch("Background", MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground)
        ColorSwatch("Surface", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
        ColorSwatch("Error", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
    }
}

@Composable
private fun ColorSwatch(name: String, color: androidx.compose.ui.graphics.Color, onColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(color, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppText(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = onColor
        )
    }
}

@Composable
private fun AtomsShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Buttons
        AppText("Buttons:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(text = "Primary", onClick = {})
            AppOutlinedButton(text = "Outlined", onClick = {})
            AppTextButton(text = "Text", onClick = {})
        }
        
        // Icons
        AppText("Icons:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIcon(imageVector = Icons.Default.Add, contentDescription = "Add")
            AppIcon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
            AppIcon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
        
        // Icon Buttons
        AppText("Icon Buttons:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIconButton(imageVector = Icons.Default.Add, contentDescription = "Add", onClick = {})
            AppIconButton(imageVector = Icons.Default.Edit, contentDescription = "Edit", onClick = {})
            AppIconButton(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                onClick = {},
                tint = MaterialTheme.colorScheme.error
            )
        }
        
        // Icon Checkboxes
        AppText("Icon Checkboxes:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var checked1 by remember { mutableStateOf(false) }
            var checked2 by remember { mutableStateOf(true) }
            var checked3 by remember { mutableStateOf(false) }
            
            AppIconCheckbox(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                checked = checked1,
                onCheckedChange = { checked1 = it }
            )
            AppIconCheckbox(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                checked = checked2,
                onCheckedChange = { checked2 = it }
            )
            AppIconCheckbox(
                imageVector = Icons.Default.Lock,
                contentDescription = "Delete",
                checked = checked3,
                onCheckedChange = { checked3 = it }
            )
        }
        
        // Text
        AppText("Typography:", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppText("Display Large", style = MaterialTheme.typography.displayLarge)
            AppText("Headline Medium", style = MaterialTheme.typography.headlineMedium)
            AppText("Title Large", style = MaterialTheme.typography.titleLarge)
            AppText("Body Medium", style = MaterialTheme.typography.bodyMedium)
            AppText("Label Small", style = MaterialTheme.typography.labelSmall)
        }
        
        // Card
        AppText("Card:", style = MaterialTheme.typography.titleMedium)
        AppCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                AppText("This is a card with consistent styling")
            }
        }
    }
}

@Composable
private fun MoleculesShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Icon with Label
        AppText("Icon with Label:", style = MaterialTheme.typography.titleMedium)
        IconWithLabel(
            icon = Icons.Default.Add,
            label = "Create New",
            contentDescription = "Create"
        )
        
        // Title with Subtitle
        AppText("Title with Subtitle:", style = MaterialTheme.typography.titleMedium)
        TitleWithSubtitle(
            title = "Project Name",
            subtitle = "3 floor plans"
        )
        
        // Image with Fallback
        AppText("Image with Fallback:", style = MaterialTheme.typography.titleMedium)
        ImageWithFallback(
            imageUrl = null,
            fallbackText = "Sample Project"
        )
    }
}

@Composable
private fun OrganismsShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Empty State
        AppText("Empty State:", style = MaterialTheme.typography.titleMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        ) {
            EmptyState(
                title = "No Items",
                message = "Start by creating something new",
                actionText = "Create",
                onAction = {}
            )
        }
        
        // Loading Indicator
        AppText("Loading Indicator:", style = MaterialTheme.typography.titleMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        ) {
            LoadingIndicator()
        }
    }
}
