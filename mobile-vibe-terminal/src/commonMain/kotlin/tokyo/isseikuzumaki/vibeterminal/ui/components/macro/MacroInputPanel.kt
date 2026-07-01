package tokyo.isseikuzumaki.vibeterminal.ui.components.macro

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import org.jetbrains.compose.resources.stringResource
import puzzroom.mobile_vibe_terminal.generated.resources.*
import io.github.isseikz.kmpinput.TerminalInputContainer
import io.github.isseikz.kmpinput.TerminalInputContainerState
import tokyo.isseikuzumaki.vibeterminal.input.ModifierButtonState
import tokyo.isseikuzumaki.vibeterminal.input.ModifierKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroInputPanel(
    selectedMacroTab: MacroTab,
    isAlternateScreen: Boolean,
    isImeEnabled: Boolean,
    isSoftKeyboardVisible: Boolean,
    shiftState: ModifierButtonState,
    ctrlState: ModifierButtonState,
    altState: ModifierButtonState,
    isHardwareKeyboardConnected: Boolean,
    onDirectSend: (String) -> Unit,
    onTabSelected: (MacroTab) -> Unit,
    onToggleImeMode: () -> Unit,
    onToggleSoftKeyboard: () -> Unit,
    onModifierTap: (ModifierKey) -> Unit,
    onModifierDoubleTap: (ModifierKey) -> Unit,
    modifier: Modifier = Modifier,
    terminalInputState: TerminalInputContainerState? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Tab Row
        MacroTabRow(
            selectedTab = selectedMacroTab,
            isAlternateScreen = isAlternateScreen,
            onTabSelected = onTabSelected
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 1.dp
        )

        // Button Grid
        MacroButtonGrid(
            tab = selectedMacroTab,
            isAlternateScreen = isAlternateScreen,
            onDirectSend = { sequence ->
                onDirectSend(sequence)
            },
            onBufferInsert = { text ->
                onDirectSend(text)
            }
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 1.dp
        )

        // Controls Row (Input Mode, Keyboard, Shift, Ctrl, Alt)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // IME Mode Toggle
            FilterChip(
                selected = isImeEnabled,
                onClick = onToggleImeMode,
                enabled = !isHardwareKeyboardConnected,
                label = { Text(if (isImeEnabled) stringResource(Res.string.macro_text_mode) else stringResource(Res.string.macro_cmd_mode)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = !isHardwareKeyboardConnected,
                    selected = isImeEnabled,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.focusProperties { canFocus = false }
            )

            // Keyboard Visibility Toggle
            if (terminalInputState != null) {
                TerminalInputContainer(
                    state = terminalInputState,
                    modifier = Modifier.wrapContentSize()
                ) {
                    IconButton(
                        onClick = onToggleSoftKeyboard,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (isSoftKeyboardVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (isSoftKeyboardVisible) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                            contentDescription = "Toggle Keyboard"
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onToggleSoftKeyboard,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isSoftKeyboardVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isSoftKeyboardVisible) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                        contentDescription = "Toggle Keyboard"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Shift Toggle
            ModifierChip(
                state = shiftState,
                label = stringResource(Res.string.macro_shift),
                modifierKey = ModifierKey.SHIFT,
                onTap = onModifierTap,
                onDoubleTap = onModifierDoubleTap
            )

            // Ctrl Toggle
            ModifierChip(
                state = ctrlState,
                label = stringResource(Res.string.macro_ctrl),
                modifierKey = ModifierKey.CTRL,
                onTap = onModifierTap,
                onDoubleTap = onModifierDoubleTap
            )

            // Alt Toggle
            ModifierChip(
                state = altState,
                label = stringResource(Res.string.macro_alt),
                modifierKey = ModifierKey.ALT,
                onTap = onModifierTap,
                onDoubleTap = onModifierDoubleTap
            )
        }
    }
}

/**
 * A three-state modifier toggle chip.
 *
 * Visual states:
 * - INACTIVE → surfaceVariant container
 * - ONE_SHOT → primary container (active for next key only)
 * - LOCKED   → tertiary container (stays active until tapped again)
 *
 * Single-tap → onTap; Double-tap → onDoubleTap.
 * Haptic feedback fires on every tap (single or double) within the pointerInput handler.
 */
/**
 * A three-state modifier toggle chip.
 *
 * Uses a plain Surface + combinedClickable instead of FilterChip so that
 * there is no competing internal onClick handler to steal the gesture.
 *
 * Visual states:
 * - INACTIVE → surfaceVariant container
 * - ONE_SHOT → primary container (active for next key only)
 * - LOCKED   → tertiary container (stays active until tapped again)
 *
 * Single-tap → onTap; Double-tap → onDoubleTap.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModifierChip(
    state: ModifierButtonState,
    label: String,
    modifierKey: ModifierKey,
    onTap: (ModifierKey) -> Unit,
    onDoubleTap: (ModifierKey) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val shape = FilterChipDefaults.shape
    val containerColor = when (state) {
        ModifierButtonState.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant
        ModifierButtonState.ONE_SHOT -> MaterialTheme.colorScheme.primary
        ModifierButtonState.LOCKED   -> MaterialTheme.colorScheme.tertiary
    }
    val labelColor = when (state) {
        ModifierButtonState.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
        ModifierButtonState.ONE_SHOT -> MaterialTheme.colorScheme.onPrimary
        ModifierButtonState.LOCKED   -> MaterialTheme.colorScheme.onTertiary
    }
    val borderColor = when (state) {
        ModifierButtonState.INACTIVE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ModifierButtonState.ONE_SHOT -> MaterialTheme.colorScheme.primary
        ModifierButtonState.LOCKED   -> MaterialTheme.colorScheme.tertiary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(shape)
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTap(modifierKey)
                },
                onDoubleClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDoubleTap(modifierKey)
                }
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}
