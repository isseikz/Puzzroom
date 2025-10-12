package tokyo.isseikuzumaki.puzzroom.ui.atoms

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Text field atom with consistent styling
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { AppText(text = label) },
        modifier = modifier,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyMedium
    )
}
