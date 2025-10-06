package tokyo.isseikuzumaki.puzzroom.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

@Composable
fun PreviewTemplate(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface {
            content()
        }
    }
}