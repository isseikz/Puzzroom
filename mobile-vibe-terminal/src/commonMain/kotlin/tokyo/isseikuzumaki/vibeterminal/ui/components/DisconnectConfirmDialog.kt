package tokyo.isseikuzumaki.vibeterminal.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import puzzroom.mobile_vibe_terminal.generated.resources.*

/**
 * ターミナル切断確認ダイアログ。
 *
 * Android TVでBACKキーを押した際に表示され、
 * ユーザーに切断の確認を求める。
 *
 * @param onConfirm 切断を確認した時のコールバック
 * @param onDismiss キャンセルまたはダイアログ外タップ時のコールバック
 */
@Composable
fun DisconnectConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.terminal_disconnect_confirm_title)) },
        text = { Text(stringResource(Res.string.terminal_disconnect_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.terminal_disconnect_confirm_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.terminal_disconnect_confirm_no))
            }
        }
    )
}
