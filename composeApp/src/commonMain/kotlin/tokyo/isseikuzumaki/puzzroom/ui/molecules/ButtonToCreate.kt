package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIconButton

@Composable
fun ButtonToCreate(
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    AppIconButton(
        imageVector = Icons.Default.Add,
        contentDescription = "Create New",
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    )
}
