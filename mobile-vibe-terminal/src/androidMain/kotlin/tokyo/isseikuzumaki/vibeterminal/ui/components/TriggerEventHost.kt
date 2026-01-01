package tokyo.isseikuzumaki.vibeterminal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.koinInject
import tokyo.isseikuzumaki.vibeterminal.installer.InstallConfirmationRequest
import tokyo.isseikuzumaki.vibeterminal.installer.TriggerEventHandler

/**
 * トリガーイベントを監視し、インストール確認ダイアログを表示するホストコンポーネント
 *
 * アプリのルートに配置して使用する。TriggerEventHandler からのイベントを購読し、
 * 必要に応じてインストール確認ダイアログを表示する。
 *
 * @param content ラップするコンテンツ
 */
@Composable
fun TriggerEventHost(
    content: @Composable () -> Unit
) {
    val triggerEventHandler = koinInject<TriggerEventHandler>()
    var installRequest by remember { mutableStateOf<InstallConfirmationRequest?>(null) }

    // TriggerEventHandler の監視を開始
    LaunchedEffect(Unit) {
        triggerEventHandler.startListening()
    }

    // インストール確認リクエストを購読
    LaunchedEffect(triggerEventHandler) {
        triggerEventHandler.installConfirmationRequests.collect { request ->
            installRequest = request
        }
    }

    // メインコンテンツを表示
    content()

    // インストール確認ダイアログを表示
    installRequest?.let { request ->
        InstallConfirmationDialog(
            request = request,
            onDismiss = { installRequest = null }
        )
    }
}
