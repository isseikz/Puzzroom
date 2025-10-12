package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIcon
import tokyo.isseikuzumaki.puzzroom.ui.atoms.HorizontalSpacer

/**
 * Icon with label molecule
 */
@Composable
fun IconWithLabel(
    icon: ImageVector,
    label: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(
            imageVector = icon,
            contentDescription = contentDescription
        )
        AppText(text = label)
    }
}
