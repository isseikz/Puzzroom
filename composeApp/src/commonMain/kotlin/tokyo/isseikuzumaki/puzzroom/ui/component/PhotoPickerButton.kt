package tokyo.isseikuzumaki.puzzroom.ui.component

import androidx.compose.runtime.Composable

@Composable
expect fun PhotoPickerButton(
    onImagePicked: (String) -> Unit
)
