package tokyo.isseikuzumaki.vibeterminal.installer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import tokyo.isseikuzumaki.vibeterminal.data.datastore.PreferencesHelper
import tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.ssh.TriggerChannel
import tokyo.isseikuzumaki.vibeterminal.ssh.TriggerEvent
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.HttpURLConnection

/**
 * インストール確認リクエストを表すデータクラス
 *
 * @param apkUrl ダウンロード元URL
 * @param apkFile ダウンロード済みのAPKファイル
 * @param onConfirm 確認時のコールバック
 * @param onCancel キャンセル時のコールバック
 */
data class InstallConfirmationRequest(
    val apkUrl: String,
    val apkFile: File,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

/**
 * トリガーイベントを処理してAPKのダウンロードとインストールを行うハンドラー
 *
 * TriggerChannelからのイベントを購読し、以下の処理を行う:
 * 1. APKをダウンロード
 * 2. 自動インストールが有効な場合は直接インストール
 * 3. 無効な場合はユーザーに確認ダイアログを表示
 */
class TriggerEventHandler(
    private val context: Context,
    private val preferencesHelper: PreferencesHelper,
    private val apkInstaller: ApkInstaller,
    private val sshRepository: SshRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val triggerChannel = TriggerChannel.getInstance()

    private val _installConfirmationRequests = MutableSharedFlow<InstallConfirmationRequest>()

    /**
     * インストール確認リクエストを購読するための Flow
     * UI層でこれを購読して確認ダイアログを表示する
     */
    val installConfirmationRequests: Flow<InstallConfirmationRequest> = _installConfirmationRequests.asSharedFlow()

    val debugEvents: Flow<String> = triggerChannel.debugEvents

    private var isListening = false

    /**
     * トリガーイベントの監視を開始する
     */
    fun startListening() {
        if (isListening) return
        isListening = true

        scope.launch {
            triggerChannel.triggerEvents.collect { event ->
                handleTriggerEvent(event)
            }
        }
    }

    /**
     * トリガーイベントの監視を停止する
     */
    fun stopListening() {
        isListening = false
    }

    /**
     * トリガーイベントを処理する
     */
    private suspend fun handleTriggerEvent(event: TriggerEvent) {
        Timber.d("=== Handling Trigger Event ===")
        Timber.d("APK Path/URL: ${event.apkUrl}")

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Trigger received: ${event.apkUrl}", Toast.LENGTH_SHORT).show()
        }

        try {
            // APKをダウンロード
            val apkFile = downloadApk(event.apkUrl)
            if (apkFile == null) {
                Timber.e("Failed to download APK")
                return
            }

            Timber.d("APK downloaded: ${apkFile.absolutePath}")

            // 自動インストール設定を確認
            val autoInstallEnabled = preferencesHelper.autoInstallEnabled.first()

            if (autoInstallEnabled) {
                // 自動インストールが有効な場合は直接インストール
                Timber.d("Auto-install enabled, launching install...")
                launchInstallIntent(apkFile)
            } else {
                // 確認ダイアログを表示
                Timber.d("Auto-install disabled, requesting confirmation...")
                requestInstallConfirmation(event.apkUrl, apkFile)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error handling trigger event")
        }
    }

    /**
     * APKをダウンロードする (HTTP or SFTP)
     */
    private suspend fun downloadApk(apkUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            Timber.d("=== Downloading APK ===")
            Timber.d("Target: $apkUrl")

            val cacheDir = apkInstaller.getCacheDir()
            val apkFile = File(cacheDir, "downloaded_${System.currentTimeMillis()}.apk")

            if (apkUrl.startsWith("http://") || apkUrl.startsWith("https://")) {
                downloadHttp(apkUrl, apkFile)
            } else {
                downloadSftp(apkUrl, apkFile)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error downloading APK")
            null
        }
    }

    private suspend fun downloadSftp(remotePath: String, localFile: File): File? {
        try {
            // Expand tilde (~) if present
            val expandedPath = if (remotePath.startsWith("~/")) {
                val homeResult = sshRepository.executeCommand("echo \$HOME")
                val homePath = homeResult.getOrNull()?.trim() ?: ""
                if (homePath.isNotEmpty()) {
                    remotePath.replaceFirst("~", homePath)
                } else {
                    remotePath
                }
            } else {
                remotePath
            }
            
            Timber.d("SFTP Download: $expandedPath -> ${localFile.absolutePath}")
            val result = sshRepository.downloadFile(expandedPath, localFile)
            
            return if (result.isSuccess) {
                Timber.d("SFTP Download complete")
                localFile
            } else {
                Timber.e("SFTP Download failed: ${result.exceptionOrNull()?.message}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "SFTP Download exception")
            return null
        }
    }

    private fun downloadHttp(urlStr: String, outputFile: File): File? {
        try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 30000
                readTimeout = 60000
                doInput = true
            }

            try {
                connection.connect()
                val responseCode = connection.responseCode

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Timber.e("HTTP Download failed with response code: $responseCode")
                    return null
                }

                val contentLength = connection.contentLength
                Timber.d("Content length: $contentLength bytes")

                connection.inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            // 進捗をログ (1MB毎)
                            if (totalBytesRead % (1024 * 1024) == 0L) {
                                Timber.d("Downloaded: ${totalBytesRead / (1024 * 1024)} MB")
                            }
                        }
                        Timber.d("HTTP Download complete: $totalBytesRead bytes")
                    }
                }
                return outputFile
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Timber.e(e, "HTTP Download exception")
            return null
        }
    }

    /**
     * インストール確認をリクエストする
     */
    private suspend fun requestInstallConfirmation(apkUrl: String, apkFile: File) {
        _installConfirmationRequests.emit(
            InstallConfirmationRequest(
                apkUrl = apkUrl,
                apkFile = apkFile,
                onConfirm = {
                    scope.launch {
                        launchInstallIntent(apkFile)
                    }
                },
                onCancel = {
                    // キャンセル時はAPKファイルを削除
                    apkFile.delete()
                    Timber.d("Install cancelled, APK file deleted")
                }
            )
        )
    }

    /**
     * インストールインテントを起動する
     */
    private fun launchInstallIntent(apkFile: File) {
        Timber.d("=== Launching Install Intent ===")
        Timber.d("APK file: ${apkFile.absolutePath}")

        val result = apkInstaller.installApk(apkFile)
        if (result.isSuccess) {
            Timber.d("Install intent launched successfully")
        } else {
            Timber.e("Failed to launch install intent: ${result.exceptionOrNull()?.message}")
        }
    }

    companion object {
        private var instance: TriggerEventHandler? = null

        /**
         * シングルトンインスタンスを取得する
         */
        fun getInstance(
            context: Context,
            preferencesHelper: PreferencesHelper,
            apkInstaller: ApkInstaller,
            sshRepository: SshRepository
        ): TriggerEventHandler {
            return instance ?: synchronized(this) {
                instance ?: TriggerEventHandler(context, preferencesHelper, apkInstaller, sshRepository).also {
                    instance = it
                }
            }
        }

        /**
         * 既存のインスタンスがあれば取得する
         */
        fun getInstanceOrNull(): TriggerEventHandler? = instance
    }
}
