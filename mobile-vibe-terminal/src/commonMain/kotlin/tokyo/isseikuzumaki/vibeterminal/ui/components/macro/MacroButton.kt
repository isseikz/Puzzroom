package tokyo.isseikuzumaki.vibeterminal.ui.components.macro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MacroButton(
    label: String,
    onClick: () -> Unit,
    isEmphasis: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isEmphasis) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            contentColor = if (isEmphasis) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isEmphasis) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = modifier.height(36.dp).focusProperties { canFocus = false }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
