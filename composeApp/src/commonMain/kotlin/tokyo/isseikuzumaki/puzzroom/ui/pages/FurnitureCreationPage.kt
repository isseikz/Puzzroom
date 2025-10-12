package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.AppState
import tokyo.isseikuzumaki.puzzroom.domain.*
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppCard
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.component.EditMode
import tokyo.isseikuzumaki.puzzroom.ui.component.EditablePolygonCanvas
import tokyo.isseikuzumaki.puzzroom.ui.organisms.FurnitureTemplateCard
import kotlin.math.roundToInt

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
                text = { Text("Presets") }
            )
            Tab(
                selected = creationMode == FurnitureCreationMode.SIMPLE,
                onClick = { creationMode = FurnitureCreationMode.SIMPLE },
                text = { Text("Simple Editor") }
            )
            Tab(
                selected = creationMode == FurnitureCreationMode.DETAILED,
                onClick = { creationMode = FurnitureCreationMode.DETAILED },
                text = { Text("Detailed Editor") }
            )
        }
        
        // Content based on selected mode
        when (creationMode) {
            FurnitureCreationMode.PRESET -> {
                PresetSelectionContent(
                    appState = appState,
                    onFurnitureCreated = onFurnitureCreated,
                    onCancel = onCancel
                )
            }
            FurnitureCreationMode.SIMPLE -> {
                SimpleEditorContent(
                    appState = appState,
                    onFurnitureCreated = onFurnitureCreated,
                    onCancel = onCancel
                )
            }
            FurnitureCreationMode.DETAILED -> {
                DetailedEditorContent(
                    appState = appState,
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
    onFurnitureCreated: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPreset by remember { mutableStateOf<FurnitureTemplate?>(null) }
    
    Column(modifier = modifier.fillMaxSize()) {
        Text(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    selectedPreset?.let { preset ->
                        appState.addCustomFurnitureTemplate(preset)
                        onFurnitureCreated()
                    }
                },
                enabled = selectedPreset != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Add to Library")
            }
        }
    }
}

/**
 * Simple rectangle editor content
 */
@Composable
private fun SimpleEditorContent(
    appState: AppState,
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
        Text(
            "Create a rectangular furniture",
            style = MaterialTheme.typography.titleMedium
        )
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Furniture Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Category selection
        Column {
            Text("Category", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FurnitureCategory.entries.take(3).forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.name) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FurnitureCategory.entries.drop(3).forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.name) }
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = widthStr,
                onValueChange = { widthStr = it },
                label = { Text("Width (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = depthStr,
                onValueChange = { depthStr = it },
                label = { Text("Depth (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val width = widthStr.toIntOrNull()
                    val depth = depthStr.toIntOrNull()
                    if (name.isNotBlank() && width != null && depth != null && width > 0 && depth > 0) {
                        val template = FurnitureTemplate(
                            name = name,
                            category = selectedCategory,
                            width = Centimeter(width),
                            depth = Centimeter(depth)
                        )
                        appState.addCustomFurnitureTemplate(template)
                        onFurnitureCreated()
                    }
                },
                enabled = name.isNotBlank() && 
                         widthStr.toIntOrNull()?.let { it > 0 } == true &&
                         depthStr.toIntOrNull()?.let { it > 0 } == true,
                modifier = Modifier.weight(1f)
            ) {
                Text("Create")
            }
        }
    }
}

/**
 * Detailed polygon editor content
 */
@Composable
private fun DetailedEditorContent(
    appState: AppState,
    onFurnitureCreated: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FurnitureCategory.CUSTOM) }
    var polygons by remember { mutableStateOf<List<Polygon>>(emptyList()) }
    var currentPolygon by remember { mutableStateOf<Polygon?>(null) }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Input fields
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Create a custom shape furniture",
                style = MaterialTheme.typography.titleMedium
            )
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Furniture Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Category selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FurnitureCategory.entries.take(4).forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.name.take(3)) }
                    )
                }
            }
            
            Text(
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
                polygons = polygons + listOfNotNull(currentPolygon),
                selectedPolygonIndex = null,
                backgroundImageUrl = null,
                editMode = EditMode.Creation,
                onNewVertex = { offset ->
                    val point = Point(
                        x = Centimeter(offset.x.roundToInt()),
                        y = Centimeter(offset.y.roundToInt())
                    )
                    
                    if (currentPolygon == null) {
                        currentPolygon = Polygon(points = listOf(point))
                    } else {
                        val current = currentPolygon!!
                        // Check if clicking near the first point to close the polygon
                        val firstPoint = current.points.first()
                        val distance = kotlin.math.sqrt(
                            ((point.x.value - firstPoint.x.value) * (point.x.value - firstPoint.x.value) +
                             (point.y.value - firstPoint.y.value) * (point.y.value - firstPoint.y.value)).toFloat()
                        )
                        
                        if (distance < 20 && current.points.size >= 3) {
                            // Close the polygon
                            polygons = polygons + currentPolygon!!
                            currentPolygon = null
                        } else {
                            currentPolygon = current.copy(points = current.points + point)
                        }
                    }
                },
                onCompletePolygon = { polygon ->
                    // Not used in this mode
                },
                onVertexMove = { _, _, _ ->
                    // Not used in creation mode
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val finalPolygon = polygons.firstOrNull()
                    if (name.isNotBlank() && finalPolygon != null) {
                        // Calculate bounding box for width and depth
                        val xs = finalPolygon.points.map { it.x.value }
                        val ys = finalPolygon.points.map { it.y.value }
                        val width = (xs.maxOrNull() ?: 0) - (xs.minOrNull() ?: 0)
                        val depth = (ys.maxOrNull() ?: 0) - (ys.minOrNull() ?: 0)
                        
                        val template = FurnitureTemplate(
                            name = name,
                            category = selectedCategory,
                            width = Centimeter(width),
                            depth = Centimeter(depth)
                        )
                        appState.addCustomFurnitureTemplate(template)
                        onFurnitureCreated()
                    }
                },
                enabled = name.isNotBlank() && polygons.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Create")
            }
        }
    }
}
