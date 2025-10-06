package tokyo.isseikuzumaki.puzzroom.ui

import androidx.compose.runtime.Composable

@Composable
expect fun PhotoPickerButton(
    onImagePicked: (String) -> Unit
)
