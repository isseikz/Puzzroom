package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Utility function to create a selection border modifier
 * Used for selectable card components to show selection state
 * 
 * @param isSelected Whether the item is currently selected
 * @return Modifier with border if selected, empty modifier otherwise
 */
@Composable
fun Modifier.selectionBorder(isSelected: Boolean): Modifier {
    return if (isSelected) {
        this.border(
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            shape = MaterialTheme.shapes.medium
        )
    } else {
        this
    }
}
