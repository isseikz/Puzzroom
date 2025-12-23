package tokyo.isseikuzumaki.vibeterminal.ui.components.macro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEmphasis) {
                Color(0xFF39D353).copy(alpha = 0.2f)
            } else {
                Color(0xFF2A2A2A)
            },
            contentColor = if (isEmphasis) {
                Color(0xFF39D353)
            } else {
                Color(0xFF00FF00)
            }
        ),
        border = if (isEmphasis) {
            BorderStroke(1.dp, Color(0xFF39D353))
        } else null,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = modifier.height(36.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
