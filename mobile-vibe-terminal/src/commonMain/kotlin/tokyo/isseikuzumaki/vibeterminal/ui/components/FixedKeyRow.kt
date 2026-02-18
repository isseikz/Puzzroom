package tokyo.isseikuzumaki.vibeterminal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tokyo.isseikuzumaki.vibeterminal.util.getHapticFeedbackProvider

/**
 * A fixed auxiliary key row providing quick access to frequently used terminal keys.
 *
 * This row is permanently visible above the macro tab bar and contains 8 keys:
 * ESC, TAB, CTRL+C, |, /, ↑, ↓, Enter
 *
 * Features:
 * - Always visible regardless of active tab
 * - Sends keystrokes immediately on tap
 * - Provides visual (ripple) and haptic feedback
 * - Visual separator between arrow keys and Enter
 *
 * @param onKeyPress Callback invoked with the key code when a key is tapped
 * @param modifier Modifier for customizing the row layout
 * @param enabled Whether the keys respond to taps (default: true)
 */
@Composable
fun FixedKeyRow(
    onKeyPress: (keyCode: String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Get haptic feedback at composable level
    val hapticFeedback = getHapticFeedbackProvider().getHapticFeedback()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // Top border separator
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )

        // Key row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FixedKey.ALL.forEach { key ->
                // Key button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onKeyPress(key.keyCode)
                            }
                        )
                        .semantics {
                            contentDescription = key.label
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = key.label,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Visual separator between ↓ and Enter
                if (key.hasTrailingSeparator) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}
