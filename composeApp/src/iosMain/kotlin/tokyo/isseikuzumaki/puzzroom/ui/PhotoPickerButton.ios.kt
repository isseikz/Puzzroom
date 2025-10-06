package tokyo.isseikuzumaki.puzzroom.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher


@Composable
actual fun PhotoPickerButton(
    onImagePicked: (String) -> Unit
) {
    var openPicker by remember { mutableStateOf(false) }
    androidx.compose.material3.Button(
        onClick = { openPicker = true }
    ) {
        Text("Pick Photo")
    }

    if (openPicker) {
        GalleryPickerLauncher(
            onPhotosSelected = {
                it.firstOrNull()?.let {
                    onImagePicked(it.uri)
                    openPicker = false
                }
            },
            onError = {

            },
            onDismiss = {

            },
            enableCrop = false,
            allowMultiple = false,
            mimeTypes = listOf(MimeType.IMAGE_PNG, MimeType.IMAGE_JPEG)
        )
    }
}
