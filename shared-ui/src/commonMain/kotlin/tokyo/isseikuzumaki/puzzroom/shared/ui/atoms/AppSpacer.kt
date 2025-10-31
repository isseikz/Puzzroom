package tokyo.isseikuzumaki.puzzroom.shared.ui.atoms

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Horizontal spacer atom
 */
@Composable
fun HorizontalSpacer(
    width: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Spacer(modifier = modifier.width(width))
}

/**
 * Vertical spacer atom
 */
@Composable
fun VerticalSpacer(
    height: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Spacer(modifier = modifier.height(height))
}
