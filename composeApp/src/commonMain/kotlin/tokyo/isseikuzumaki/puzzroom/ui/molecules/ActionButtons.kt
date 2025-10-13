package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppOutlinedButton

/**
 * Action buttons molecule for cancel and confirm actions
 */
@Composable
fun ActionButtons(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "Create",
    confirmEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppOutlinedButton(
            text = "Cancel",
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        )
        AppButton(
            text = confirmText,
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(1f)
        )
    }
}
