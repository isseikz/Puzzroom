package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppCard
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText

/**
 * Texture options for furniture
 */
enum class TextureOption {
    NONE,
    WOOD_LIGHT,
    WOOD_DARK,
    METAL,
    FABRIC,
    GLASS;

    fun displayName(): String = when (this) {
        NONE -> "None"
        WOOD_LIGHT -> "Light Wood"
        WOOD_DARK -> "Dark Wood"
        METAL -> "Metal"
        FABRIC -> "Fabric"
        GLASS -> "Glass"
    }
}

/**
 * Texture selector molecule for selecting furniture texture
 */
@Composable
fun TextureSelector(
    selectedTexture: TextureOption,
    onTextureSelected: (TextureOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AppText(
            text = "Select Texture",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(TextureOption.entries) { texture ->
                TextureOptionCard(
                    texture = texture,
                    isSelected = texture == selectedTexture,
                    onClick = { onTextureSelected(texture) }
                )
            }
        }
    }
}

/**
 * Texture option card for individual texture selection
 */
@Composable
private fun TextureOptionCard(
    texture: TextureOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        onClick = onClick,
        modifier = modifier
            .width(80.dp)
            .height(80.dp)
            .selectionBorder(isSelected),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // TODO: Replace with actual texture preview when available
                AppText(
                    text = texture.name.take(1),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                AppText(
                    text = texture.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2
                )
            }
        }
    }
}

@Preview
@Composable
private fun TextureSelectorPreview() {
    TextureSelector(
        selectedTexture = TextureOption.WOOD_LIGHT,
        onTextureSelected = {}
    )
}
