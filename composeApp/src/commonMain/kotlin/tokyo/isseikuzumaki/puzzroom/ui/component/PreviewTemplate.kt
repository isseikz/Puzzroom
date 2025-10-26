package tokyo.isseikuzumaki.puzzroom.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import tokyo.isseikuzumaki.puzzroom.ui.theme.PuzzroomTheme

@Composable
fun PreviewTemplate(content: @Composable () -> Unit) {
    PuzzroomTheme {
        Surface {
            content()
        }
    }
}