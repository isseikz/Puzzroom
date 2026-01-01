package tokyo.isseikuzumaki.vibeterminal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
                    text = "新しいAPKファイルがダウンロードされました。インストールしますか?",
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
                    text = "ファイル: ${request.apkFile.name}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "サイズ: ${formatFileSize(request.apkFile.length())}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    request.onConfirm()
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

/**
 * ファイルサイズを人間が読みやすい形式にフォーマットする
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
