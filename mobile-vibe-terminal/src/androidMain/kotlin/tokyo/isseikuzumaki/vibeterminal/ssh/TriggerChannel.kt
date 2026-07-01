package tokyo.isseikuzumaki.vibeterminal.ssh

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.io.InputStream

/**
 * リモートビルドトリガーイベントを表すデータクラス
 *
 * @param apkUrl ダウンロードするAPKのURL
 * @param timestamp トリガーを受信した時刻
 */
data class TriggerEvent(
    val apkUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * SSH remote port forwarding を介してビルドトリガーを受信するためのチャネル
 *
 * サーバーからのトリガー接続を受け付け、APKのURLをパースして
 * TriggerEvent として発行する
 */
class TriggerChannel {

    private val _triggerEvents = MutableSharedFlow<TriggerEvent>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _debugEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 20,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * トリガーイベントを購読するための Flow
     */
    val triggerEvents: Flow<TriggerEvent> = _triggerEvents.asSharedFlow()

    /**
     * デバッグメッセージを購読するための Flow
     */
    val debugEvents: Flow<String> = _debugEvents.asSharedFlow()

    suspend fun sendDebugMessage(message: String) {
        Timber.d("TriggerChannel Debug: $message")
        _debugEvents.emit(message)
    }

    /**
     * 受信したデータからトリガーイベントを処理する
     *
     * @param inputStream 受信データのストリーム
     */
    suspend fun handleIncomingData(inputStream: InputStream) {
        try {
            sendDebugMessage("Reading from stream...")
            // Read line by line instead of readText() to handle streams that stay open
            val reader = inputStream.bufferedReader()
            val data = reader.readLine() ?: run {
                sendDebugMessage("Stream empty or closed")
                return
            }
            sendDebugMessage("Received data: '$data'")
            Timber.d("TriggerChannel: Received data: $data")

            // データをパースしてパスを抽出
            val path = extractPath(data)
            if (path != null) {
                sendDebugMessage("Extracted Path: $path")
                Timber.d("TriggerChannel: Extracted Path: $path")
                _triggerEvents.emit(TriggerEvent(apkUrl = path))
            } else {
                // extractPath は空文字列以外はnullを返さないため、ここは実質的に到達しないはず
                sendDebugMessage("Failed to extract path from: $data")
                Timber.w("TriggerChannel: Could not extract path from data: $data")
            }
        } catch (e: Exception) {
            sendDebugMessage("Error: ${e.message}")
            Timber.e(e, "TriggerChannel: Error handling incoming data")
        }
    }

    /**
     * 受信データから対象パスを抽出する
     *
     * 以下の順で評価する:
     * 1. JSON形式: {"url": "..."}
     * 2. キー=値形式: url=...
     * 3. その他の文字列（そのままパスとして扱う）
     */
    private fun extractPath(data: String): String? {
        val trimmedData = data.trim()
        if (trimmedData.isEmpty()) return null

        // JSON形式をチェック: {"url": "..."} または {"apkUrl": "..."}
        val jsonUrlPattern = Regex(""""(?:url|apkUrl)"\s*:\s*"([^"]+)"""")
        jsonUrlPattern.find(trimmedData)?.let { match ->
            return match.groupValues[1]
        }

        // キー=値形式をチェック: url=... または apkUrl=...
        val keyValuePattern = Regex("""(?:url|apkUrl)=(.+)""", RegexOption.IGNORE_CASE)
        keyValuePattern.find(trimmedData)?.let { match ->
            return match.groupValues[1].trim()
        }

        // フォールバック: そのままパスとして扱う
        // ユーザーの意図通り、パース不要でそのまま渡す
        return trimmedData
    }

    companion object {
        private var instance: TriggerChannel? = null

        /**
         * シングルトンインスタンスを取得する
         */
        fun getInstance(): TriggerChannel {
            return instance ?: synchronized(this) {
                instance ?: TriggerChannel().also { instance = it }
            }
        }
    }
}
