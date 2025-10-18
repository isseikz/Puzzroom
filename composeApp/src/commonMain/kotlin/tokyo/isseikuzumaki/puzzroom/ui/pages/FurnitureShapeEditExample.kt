package tokyo.isseikuzumaki.puzzroom.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.organisms.FurnitureShapeEditForm
import tokyo.isseikuzumaki.puzzroom.ui.organisms.FurnitureShapeFormData

/**
 * Example page demonstrating the Furniture Shape Edit Modal Bottom Sheet
 * 
 * This page shows how to properly integrate the ModalBottomSheet with:
 * - Proper state management using rememberModalBottomSheetState
 * - Show/hide control with mutableStateOf
 * - Accessibility features (close button, back gesture support)
 * - No nested bottom sheets (as per UX guidelines)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FurnitureShapeEditExample(
    modifier: Modifier = Modifier
) {
    // State for showing/hiding the bottom sheet
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Sheet state for managing expand/collapse
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    
    // Sample data to edit
    var currentData by remember {
        mutableStateOf(
            FurnitureShapeFormData(
                name = "Sample Furniture",
                width = "100.0",
                height = "50.0"
            )
        )
    }
    
    // Main content
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppText(
                text = "Shape Edit Modal Bottom Sheet Demo",
                style = MaterialTheme.typography.titleLarge
            )
            
            AppText(
                text = "Current Data: ${currentData.name} (${currentData.width}cm × ${currentData.height}cm)",
                style = MaterialTheme.typography.bodyMedium
            )
            
            AppButton(
                text = "Edit Shape",
                onClick = { showBottomSheet = true }
            )
        }
    }
    
    // Modal Bottom Sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                // User dismissed by swipe down or tapping scrim
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            FurnitureShapeEditForm(
                initialData = currentData,
                onDismiss = {
                    // User clicked close button or cancel
                    showBottomSheet = false
                },
                onSave = { formData ->
                    // User saved the form
                    currentData = formData
                    showBottomSheet = false
                }
            )
        }
    }
}

/**
 * Integration example showing how to use in existing furniture management screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FurnitureShapeEditIntegrationExample(
    furnitureId: String?,
    onEditComplete: (FurnitureShapeFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    // Load furniture data based on furnitureId
    val furnitureData by remember(furnitureId) {
        derivedStateOf {
            // TODO: Load from ViewModel/Repository
            FurnitureShapeFormData(
                name = "Furniture #$furnitureId",
                width = "120.0",
                height = "80.0"
            )
        }
    }
    
    Column(modifier = modifier) {
        // Main content goes here
        AppButton(
            text = "Edit",
            onClick = { showEditSheet = true }
        )
        
        // Bottom sheet for editing
        if (showEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEditSheet = false },
                sheetState = sheetState
            ) {
                FurnitureShapeEditForm(
                    initialData = furnitureData,
                    onDismiss = { showEditSheet = false },
                    onSave = { formData ->
                        onEditComplete(formData)
                        showEditSheet = false
                    }
                )
            }
        }
    }
}
