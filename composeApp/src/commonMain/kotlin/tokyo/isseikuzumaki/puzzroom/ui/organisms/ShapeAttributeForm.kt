package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter
import tokyo.isseikuzumaki.puzzroom.domain.Centimeter.Companion.cm
import tokyo.isseikuzumaki.puzzroom.domain.Degree
import tokyo.isseikuzumaki.puzzroom.domain.Degree.Companion.degree
import tokyo.isseikuzumaki.puzzroom.domain.RoomShapeType
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIconButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppTextField
import tokyo.isseikuzumaki.puzzroom.ui.atoms.VerticalSpacer
import tokyo.isseikuzumaki.puzzroom.ui.molecules.ActionButtons

/**
 * Shape attribute form data for room elements
 */
data class ShapeAttributeFormData(
    val shapeType: RoomShapeType = RoomShapeType.WALL,
    val width: Centimeter = 100.cm,
    val angle: Degree = 0.degree
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
    val isWidthValid = formData.width.value > 0

    val angleValue = formData.angle.value
    val isAngleValid = when (formData.shapeType) {
        RoomShapeType.DOOR -> angleValue in 0f..180f
        else -> true // Not required for walls
    }

    val isFormValid = isWidthValid && isAngleValid
    
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
            value = formData.width.value.toString(),
            onValueChange = { formData = formData.copy(width = (it.toIntOrNull() ?: 0).cm) },
            label = "Width (cm)",
            modifier = Modifier.fillMaxWidth()
        )
        
        VerticalSpacer(height = 8.dp)

        // Angle input (common for all types)
        AppTextField(
            value = formData.angle.value.toString(),
            onValueChange = { formData = formData.copy(angle = (it.toFloatOrNull() ?: 0f).degree()) },
            label = "Angle (degrees)",
            modifier = Modifier.fillMaxWidth()
        )

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
            shapeType = RoomShapeType.DOOR,
            width = 80.cm,
            angle = 90.degree
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
                width = 80.cm,
                angle = 90.degree
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
                text = if (currentData.shapeType == RoomShapeType.DOOR) {
                    "Current: ${currentData.shapeType.displayName()} (${currentData.width}cm width, ${currentData.angle.value}° angle)"
                } else {
                    "Current: ${currentData.shapeType.displayName()} (${currentData.width}cm width)"
                },
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
