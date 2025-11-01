package tokyo.isseikuzumaki.shared.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import tokyo.isseikuzumaki.shared.ui.theme.AppTheme

@Composable
fun PreviewTemplate(content: @Composable () -> Unit) {
    AppTheme {
        Surface {
            content()
        }
    }
}
