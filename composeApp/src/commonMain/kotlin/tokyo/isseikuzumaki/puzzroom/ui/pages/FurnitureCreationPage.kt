package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.AppState
import tokyo.isseikuzumaki.puzzroom.domain.*
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppTextField
import tokyo.isseikuzumaki.puzzroom.ui.component.EditMode
import tokyo.isseikuzumaki.puzzroom.ui.component.EditablePolygonCanvas
import tokyo.isseikuzumaki.puzzroom.ui.molecules.ActionButtons
import tokyo.isseikuzumaki.puzzroom.ui.molecules.CategorySelector
import tokyo.isseikuzumaki.puzzroom.ui.molecules.DimensionInput
import tokyo.isseikuzumaki.puzzroom.ui.organisms.FurnitureTemplateCard
import tokyo.isseikuzumaki.puzzroom.ui.viewmodel.FurnitureTemplateViewModel

/**
 * Creation mode for furniture
 */
enum class FurnitureCreationMode {
    PRESET,    // Select from presets
    SIMPLE,    // Simple rectangle editor
    DETAILED   // Detailed polygon editor
}

/**
 * Furniture creation page - allows users to create new furniture templates
 */
@Composable
fun FurnitureCreationPage(
    appState: AppState,
    furnitureTemplateViewModel: FurnitureTemplateViewModel,
    onFurnitureCreated: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var creationMode by remember { mutableStateOf(FurnitureCreationMode.PRESET) }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Mode selection tabs
        TabRow(selectedTabIndex = creationMode.ordinal) {
            Tab(
                selected = creationMode == FurnitureCreationMode.PRESET,
                onClick = { creationMode = FurnitureCreationMode.PRESET },
                text = { AppText("Presets") }
            )
            Tab(
                selected = creationMode == FurnitureCreationMode.SIMPLE,
                onClick = { creationMode = FurnitureCreationMode.SIMPLE },
                text = { AppText("Simple Editor") }
            )
            Tab(
                selected = creationMode == FurnitureCreationMode.DETAILED,
                onClick = { creationMode = FurnitureCreationMode.DETAILED },
                text = { AppText("Detailed Editor") }
            )
        }
        
        // Content based on selected mode
        when (creationMode) {
            FurnitureCreationMode.PRESET -> {
                PresetSelectionContent(
                    appState = appState,
                    furnitureTemplateViewModel = furnitureTemplateViewModel,
                    onFurnitureCreated = onFurnitureCreated,
                    onCancel = onCancel
                )
            }
            FurnitureCreationMode.SIMPLE -> {
                SimpleEditorContent(
                    appState = appState,
                    furnitureTemplateViewModel = furnitureTemplateViewModel,
                    onFurnitureCreated = onFurnitureCreated,
                    onCancel = onCancel
                )
            }
            FurnitureCreationMode.DETAILED -> {
                DetailedEditorContent(
                    appState = appState,
                    furnitureTemplateViewModel = furnitureTemplateViewModel,
                    onFurnitureCreated = onFurnitureCreated,
                    onCancel = onCancel
                )
            }
        }
    }
}

/**
 * Preset selection content
 */
@Composable
private fun PresetSelectionContent(
    appState: AppState,
    furnitureTemplateViewModel: FurnitureTemplateViewModel,
    onFurnitureCreated: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPreset by remember { mutableStateOf<FurnitureTemplate?>(null) }
    
    Column(modifier = modifier.fillMaxSize()) {
        AppText(
            "Select a preset furniture template",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FurnitureTemplate.PRESETS) { preset ->
                FurnitureTemplateCard(
                    template = preset,
                    isSelected = selectedPreset == preset,
                    onClick = { selectedPreset = preset }
                )
            }
        }
        
        // Action buttons
        ActionButtons(
            onCancel = onCancel,
            onConfirm = {
                selectedPreset?.let { preset ->
                    furnitureTemplateViewModel.saveCustomTemplate(preset)
                    onFurnitureCreated()
                }
            },
            confirmText = "Add to Library",
            confirmEnabled = selectedPreset != null,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Simple rectangle editor content
 */
@Composable
private fun SimpleEditorContent(
    appState: AppState,
    furnitureTemplateViewModel: FurnitureTemplateViewModel,
    onFurnitureCreated: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var widthStr by remember { mutableStateOf("") }
    var depthStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FurnitureCategory.CUSTOM) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppText(
            "Create a rectangular furniture",
            style = MaterialTheme.typography.titleMedium
        )
        
        AppTextField(
            value = name,
            onValueChange = { name = it },
            label = "Furniture Name",
            modifier = Modifier.fillMaxWidth()
        )
        
        CategorySelector(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )
        
        DimensionInput(
            widthValue = widthStr,
            depthValue = depthStr,
            onWidthChange = { widthStr = it },
            onDepthChange = { depthStr = it }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action buttons
        ActionButtons(
            onCancel = onCancel,
            onConfirm = {
                val width = widthStr.toIntOrNull()
                val depth = depthStr.toIntOrNull()
                if (name.isNotBlank() && width != null && depth != null && width > 0 && depth > 0) {
                    val template = FurnitureTemplate(
                        name = name,
                        category = selectedCategory,
                        width = Length(width.cm),
                        depth = Length(depth.cm)
                    )
                    furnitureTemplateViewModel.saveCustomTemplate(template)
                    onFurnitureCreated()
                }
            },
            confirmEnabled = name.isNotBlank() && 
                         widthStr.toIntOrNull()?.let { it > 0 } == true &&
                         depthStr.toIntOrNull()?.let { it > 0 } == true
        )
    }
}

/**
 * Detailed polygon editor content
 */
@Composable
private fun DetailedEditorContent(
    appState: AppState,
    furnitureTemplateViewModel: FurnitureTemplateViewModel,
    onFurnitureCreated: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FurnitureCategory.CUSTOM) }
    var polygons by remember { mutableStateOf<List<Polygon>>(emptyList()) }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Input fields
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppText(
                "Create a custom shape furniture",
                style = MaterialTheme.typography.titleMedium
            )
            
            AppTextField(
                value = name,
                onValueChange = { name = it },
                label = "Furniture Name",
                modifier = Modifier.fillMaxWidth()
            )
            
            // Category selection (compact version for detailed editor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FurnitureCategory.entries.take(4).forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { AppText(category.name.take(3)) }
                    )
                }
            }
            
            AppText(
                "Click on the canvas to add vertices. Click on the first vertex to close the shape.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            EditablePolygonCanvas(
                polygons = polygons,
                selectedPolygonIndex = null,
                backgroundImageUrl = null,
                editMode = EditMode.Creation,
                onNewVertex = { offset ->
                    // Canvas handles vertex tracking internally
                },
                onCompletePolygon = { polygon ->
                    if (polygon.points.size >= 3) {
                        polygons = polygons + polygon
                    }
                },
                onVertexMove = { _, _, _ ->
                    // Not used in creation mode
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Action buttons
        ActionButtons(
            onCancel = onCancel,
            onConfirm = {
                polygons.firstOrNull()?.takeIf { name.isNotBlank() }?.let { finalPolygon ->
                    // Calculate bounding box for width and depth
                    val xs = finalPolygon.points.map { it.x.value }
                    val ys = finalPolygon.points.map { it.y.value }
                    val width = (xs.maxOrNull() ?: 0) - (xs.minOrNull() ?: 0)
                    val depth = (ys.maxOrNull() ?: 0) - (ys.minOrNull() ?: 0)
                    
                    val template = FurnitureTemplate(
                        name = name,
                        category = selectedCategory,
                        width = Length(width.cm),
                        depth = Length(depth.cm)
                    )
                    furnitureTemplateViewModel.saveCustomTemplate(template)
                    onFurnitureCreated()
                }
            },
            confirmEnabled = name.isNotBlank() && polygons.isNotEmpty(),
            modifier = Modifier.padding(16.dp)
        )
    }
}
