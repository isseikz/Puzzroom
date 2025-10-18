package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIconButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppTextField
import tokyo.isseikuzumaki.puzzroom.ui.atoms.VerticalSpacer
import tokyo.isseikuzumaki.puzzroom.ui.molecules.*

/**
 * Furniture shape edit form data
 */
data class FurnitureShapeFormData(
    val name: String = "",
    val width: String = "",
    val height: String = "",
    val shape: ShapeTemplate = ShapeTemplate.RECTANGLE,
    val texture: TextureOption = TextureOption.NONE
)

/**
 * Furniture shape edit form organism
 * Complete form for editing furniture shape properties
 * 
 * @param initialData Initial form data
 * @param onDismiss Callback when form is dismissed/cancelled
 * @param onSave Callback when form is saved with validated data
 * @param modifier Modifier for the form container
 */
@Composable
fun FurnitureShapeEditForm(
    initialData: FurnitureShapeFormData = FurnitureShapeFormData(),
    onDismiss: () -> Unit,
    onSave: (FurnitureShapeFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    var formData by remember { mutableStateOf(initialData) }
    
    // Validation
    val isNameValid = formData.name.isNotBlank()
    val widthValue = formData.width.toFloatOrNull()
    val isWidthValid = widthValue != null && widthValue > 0f
    val heightValue = formData.height.toFloatOrNull()
    val isHeightValid = heightValue != null && heightValue > 0f
    val isFormValid = isNameValid && isWidthValid && isHeightValid
    
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
                text = "Edit Shape",
                style = MaterialTheme.typography.titleLarge
            )
            AppIconButton(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                onClick = onDismiss
            )
        }
        
        HorizontalDivider()
        
        // Name input
        AppTextField(
            value = formData.name,
            onValueChange = { formData = formData.copy(name = it) },
            label = "Shape Name",
            modifier = Modifier.fillMaxWidth()
        )
        
        // Dimensions input
        DecimalDimensionInput(
            widthValue = formData.width,
            heightValue = formData.height,
            onWidthChange = { formData = formData.copy(width = it) },
            onHeightChange = { formData = formData.copy(height = it) }
        )
        
        VerticalSpacer(height = 8.dp)
        
        // Shape selector
        ShapeSelector(
            selectedShape = formData.shape,
            onShapeSelected = { formData = formData.copy(shape = it) }
        )
        
        VerticalSpacer(height = 8.dp)
        
        // Texture selector
        TextureSelector(
            selectedTexture = formData.texture,
            onTextureSelected = { formData = formData.copy(texture = it) }
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
