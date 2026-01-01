package tokyo.isseikuzumaki.vibeterminal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.vibeterminal.installer.InstallConfirmationRequest

/**
 * APKインストール確認ダイアログ
 *
 * リモートビルドトリガーを受信した際に表示される確認ダイアログ。
 * ユーザーがインストールを承認または拒否できる。
 *
 * @param request インストール確認リクエスト
 * @param onDismiss ダイアログを閉じる際のコールバック
 */
@Composable
fun InstallConfirmationDialog(
    request: InstallConfirmationRequest,
    onDismiss: () -> Unit
) {
    val autoInstallChecked = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            request.onCancel()
            onDismiss()
        },
        title = {
            Text(text = "APKをインストール")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "新しいAPKファイルを検出しました。ダウンロードしてインストールしますか?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ダウンロード元:",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = request.apkUrl,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ファイル名: ${request.fileName}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = autoInstallChecked.value,
                        onCheckedChange = { autoInstallChecked.value = it }
                    )
                    Text(
                        text = "次回から自動的にインストールする",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { autoInstallChecked.value = !autoInstallChecked.value }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    request.onConfirm(autoInstallChecked.value)
                    onDismiss()
                }
            ) {
                Text("インストール")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    request.onCancel()
                    onDismiss()
                }
            ) {
                Text("キャンセル")
            }
        }
    )
}
