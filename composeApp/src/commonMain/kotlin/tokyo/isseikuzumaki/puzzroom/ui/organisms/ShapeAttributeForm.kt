package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.domain.RoomShapeType
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIconButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppTextField
import tokyo.isseikuzumaki.puzzroom.ui.atoms.VerticalSpacer
import tokyo.isseikuzumaki.puzzroom.ui.molecules.ActionButtons
import tokyo.isseikuzumaki.puzzroom.ui.molecules.DecimalDimensionInput

/**
 * Shape attribute form data for room elements
 */
data class ShapeAttributeFormData(
    val shapeType: RoomShapeType = RoomShapeType.WALL,
    val width: String = "",
    val length: String = "",  // 壁の長さなど
    val angle: String = ""     // 扉の開き角度など
)

/**
 * Shape attribute edit form organism
 * Form for editing room shape attributes (wall width, door dimensions, etc.)
 * 
 * @param initialData Initial form data
 * @param onDismiss Callback when form is dismissed/cancelled
 * @param onSave Callback when form is saved with validated data
 * @param modifier Modifier for the form container
 */
@Composable
fun ShapeAttributeForm(
    initialData: ShapeAttributeFormData = ShapeAttributeFormData(),
    onDismiss: () -> Unit,
    onSave: (ShapeAttributeFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    var formData by remember { mutableStateOf(initialData) }
    
    // Validation
    val widthValue = formData.width.toFloatOrNull()
    val isWidthValid = widthValue != null && widthValue > 0f
    
    val lengthValue = formData.length.toFloatOrNull()
    val isLengthValid = when (formData.shapeType) {
        RoomShapeType.WALL -> lengthValue != null && lengthValue > 0f
        else -> true // Optional for doors/windows
    }
    
    val angleValue = formData.angle.toFloatOrNull()
    val isAngleValid = when (formData.shapeType) {
        RoomShapeType.DOOR -> angleValue != null && angleValue > 0f && angleValue <= 180f
        else -> true // Not required for walls
    }
    
    val isFormValid = isWidthValid && isLengthValid && isAngleValid
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = "Edit ${formData.shapeType.displayName()}",
                style = MaterialTheme.typography.titleLarge
            )
            AppIconButton(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                onClick = onDismiss
            )
        }
        
        HorizontalDivider()
        
        // Shape type selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoomShapeType.entries.forEach { shapeType ->
                FilterChip(
                    selected = formData.shapeType == shapeType,
                    onClick = { formData = formData.copy(shapeType = shapeType) },
                    label = { AppText(shapeType.displayName()) }
                )
            }
        }
        
        VerticalSpacer(height = 8.dp)
        
        // Width input (common for all types)
        AppTextField(
            value = formData.width,
            onValueChange = { formData = formData.copy(width = it) },
            label = "Width (cm)",
            modifier = Modifier.fillMaxWidth()
        )
        
        // Length input (for walls)
        if (formData.shapeType == RoomShapeType.WALL) {
            AppTextField(
                value = formData.length,
                onValueChange = { formData = formData.copy(length = it) },
                label = "Length (cm)",
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Angle input (for doors)
        if (formData.shapeType == RoomShapeType.DOOR) {
            AppTextField(
                value = formData.angle,
                onValueChange = { formData = formData.copy(angle = it) },
                label = "Opening Angle (degrees)",
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        VerticalSpacer(height = 8.dp)
        
        HorizontalDivider()
        
        // Action buttons
        ActionButtons(
            onCancel = onDismiss,
            onConfirm = {
                if (isFormValid) {
                    onSave(formData)
                }
            },
            confirmText = "Save",
            confirmEnabled = isFormValid
        )
    }
}

@Preview
@Composable
private fun ShapeAttributeFormPreview() {
    ShapeAttributeForm(
        initialData = ShapeAttributeFormData(
            shapeType = RoomShapeType.WALL,
            width = "10",
            length = "200"
        ),
        onDismiss = {},
        onSave = {}
    )
}

/**
 * Integration example showing how to use in room creation screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ShapeAttributeFormWithBottomSheetPreview(
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
            ShapeAttributeFormData(
                shapeType = RoomShapeType.DOOR,
                width = "80",
                angle = "90"
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
                text = "Room Shape Edit Modal Demo",
                style = MaterialTheme.typography.titleLarge
            )

            AppText(
                text = "Current: ${currentData.shapeType.displayName()} (${currentData.width}cm width)",
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
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            ShapeAttributeForm(
                initialData = currentData,
                onDismiss = {
                    showBottomSheet = false
                },
                onSave = { formData ->
                    currentData = formData
                    showBottomSheet = false
                }
            )
        }
    }
}
