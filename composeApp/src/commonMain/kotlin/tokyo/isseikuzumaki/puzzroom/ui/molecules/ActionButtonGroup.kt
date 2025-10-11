package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppSecondaryButton

/**
 * Action button group molecule
 * アクションボタンのグループを表示する分子コンポーネント
 */
@Composable
fun ActionButtonGroup(
    buttons: List<ActionButton>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { button ->
            if (button.isSecondary) {
                AppSecondaryButton(
                    text = button.text,
                    onClick = button.onClick,
                    enabled = button.enabled
                )
            } else {
                AppButton(
                    text = button.text,
                    onClick = button.onClick,
                    enabled = button.enabled
                )
            }
        }
    }
}

/**
 * Action button data class
 */
data class ActionButton(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val isSecondary: Boolean = false
)
