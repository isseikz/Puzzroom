package tokyo.isseikuzumaki.shared.ui.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.shared.ui.atoms.CopyButton

/**
 * Code block molecule with integrated copy button
 * 
 * Combines CodeBlock atom functionality with CopyButton atom to create
 * a reusable component following atomic design principles.
 * 
 * The copy button is positioned in the top-right corner of the code block,
 * badge-sized as requested.
 * 
 * @param code The code content to display
 * @param onCopy Callback when copy button is clicked
 * @param modifier Modifier for the container
 * @param copyButtonContentDescription Content description for the copy button
 */
@Composable
fun CodeBlockWithCopy(
    code: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    copyButtonContentDescription: String = "Copy code"
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Code block surface
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = code,
                modifier = Modifier.padding(12.dp).padding(end = 36.dp), // Extra padding for the button
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Copy button positioned at top-right corner
        CopyButton(
            onClick = onCopy,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp), // Badge-sized button
            contentDescription = copyButtonContentDescription
        )
    }
}
