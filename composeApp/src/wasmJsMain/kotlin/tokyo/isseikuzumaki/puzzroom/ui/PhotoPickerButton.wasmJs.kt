package tokyo.isseikuzumaki.puzzroom.ui

import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Text
import org.w3c.files.FileReader
import org.w3c.files.get

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun PhotoPickerButton(
    onImagePicked: (String) -> Unit
) {
    val inputElement = remember {
        (document.createElement("input") as HTMLInputElement).apply {
            type = "file"
            accept = "image/png, image/jpeg"
        }
    }

    LaunchedEffect(onImagePicked) {
        inputElement.onchange = {
            val file = inputElement.files?.get(0)
                ?.also { println("Selected file: ${it.name}, size: ${it.size} bytes") }
            if (file != null) {
                val reader = FileReader()
                reader.onload = {
                    reader.result
                        ?.also { println("File read successfully, result: $it") }
                        ?.let { onImagePicked(it.toString()) }

                }
                reader.readAsDataURL(file)
            }
        }
    }

    Button(
        onClick = { inputElement.click() }
    ) {
        Text("Pick Photo")
    }
}
