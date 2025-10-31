package tokyo.isseikuzumaki.puzzroom.shared.ui.atoms

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icon atom with consistent styling
 */
@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

/**
 * Icon button atom
 */
@Composable
fun AppIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        AppIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

/**
 * Icon checkbox atom that toggles between checked and unchecked states
 * 
 * @param imageVector The icon to display
 * @param contentDescription Accessibility description
 * @param checked Whether the checkbox is checked
 * @param onCheckedChange Callback when the checked state changes
 * @param modifier Modifier for the component
 * @param enabled Whether the checkbox is enabled
 * @param checkedTint Color when checked (defaults to primary color)
 * @param uncheckedTint Color when unchecked (defaults to light gray)
 */
@Composable
fun AppIconCheckbox(
    imageVector: ImageVector,
    contentDescription: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedTint: Color = MaterialTheme.colorScheme.primary,
    uncheckedTint: Color = Color.LightGray
) {
    AppIconButton(
        imageVector = imageVector,
        contentDescription = contentDescription,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        enabled = enabled,
        tint = if (checked) checkedTint else uncheckedTint
    )
}
