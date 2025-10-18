package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage

/**
 * 図面の背景画像（Molecule）
 */
@Composable
fun FloorPlanBackgroundImage(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    imageUrl?.let { url ->
        AsyncImage(
            model = url,
            contentDescription = "Background Image",
            modifier = modifier.fillMaxSize()
        )
    }
}

