package tokyo.isseikuzumaki.puzzroom.ui.templates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Template for editor screens with canvas and sidebar
 */
@Composable
fun EditorScreenTemplate(
    header: @Composable () -> Unit,
    canvas: @Composable (Modifier) -> Unit,
    sidebar: @Composable (Modifier) -> Unit,
    footer: @Composable () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        header()

        // Main content: Canvas + Sidebar
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Canvas
            canvas(Modifier.weight(1f))

            // Sidebar
            sidebar(
                Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .padding(start = 16.dp)
            )
        }

        // Footer (optional toolbar)
        footer()
    }
}
