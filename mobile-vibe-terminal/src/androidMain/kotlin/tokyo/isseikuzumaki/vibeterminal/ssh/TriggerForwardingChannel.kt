package tokyo.isseikuzumaki.vibeterminal.ssh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apache.sshd.common.channel.AbstractChannel
import org.apache.sshd.common.channel.Channel
import org.apache.sshd.common.channel.Window
import org.apache.sshd.common.PropertyResolver
import org.apache.sshd.client.future.DefaultOpenFuture
import org.apache.sshd.client.future.OpenFuture
import org.apache.sshd.common.util.buffer.Buffer
import timber.log.Timber
import java.io.ByteArrayInputStream

/**
 * SSH Port Forwarding (forwarded-tcpip) リクエストをインターセプトするチャネル
 * フォワードされたポートに送信されたデータを受信し、TriggerChannelに渡す
 *
 * AbstractChannel を直接拡張してサーバー開始のチャネルを適切に処理する
 */
class TriggerForwardingChannel : AbstractChannel("forwarded-tcpip", false) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataBuffer = StringBuilder()

    /**
     * サーバーからチャネルオープンリクエストを受信した時に呼ばれる
     * forwarded-tcpip の場合: connected_address, connected_port, originator_address, originator_port
     */
    override fun open(recipient: Long, rwSize: Long, packetSize: Long, buffer: Buffer): OpenFuture {
        val msg = "TriggerForwardingChannel: open() recipient=$recipient rwSize=$rwSize packetSize=$packetSize"
        Timber.d(msg)
        scope.launch {
            TriggerChannel.getInstance().sendDebugMessage(msg)
        }

        // フォワーディング情報を読み取る
        val connectedAddress = buffer.getString()
        val connectedPort = buffer.getInt()
        val originatorAddress = buffer.getString()
        val originatorPort = buffer.getInt()
        val info = "Connected: $connectedAddress:$connectedPort from $originatorAddress:$originatorPort"
        Timber.d("TriggerForwardingChannel: $info")
        scope.launch {
            TriggerChannel.getInstance().sendDebugMessage(info)
        }

        // リモートウィンドウパラメータを設定
        setRecipient(recipient)

        // ウィンドウを初期化 (リフレクションを使用してprotectedメソッドにアクセス)
        val windowSize = 2097152L
        val localPacketSize = 32768L
        
        initWindow(remoteWindow, rwSize, packetSize, getSession())
        initWindow(localWindow, windowSize, localPacketSize, getSession())

        val winMsg = "Windows initialized: Local=${localWindow.size}/${localWindow.packetSize}, Remote=${remoteWindow.size}/${remoteWindow.packetSize}"
        Timber.d(winMsg)
        scope.launch {
            TriggerChannel.getInstance().sendDebugMessage(winMsg)
        }

        val future = DefaultOpenFuture(this.toString(), this.futureLock)

        try {
            // チャネルオープンを確認
            Timber.d("Sending open confirmation...")
            sendOpenConfirmation()
            Timber.d("Open confirmation sent.")
            
            future.setOpened()

            val openMsg = "TriggerForwardingChannel: Channel opened successfully"
            Timber.d(openMsg)
            scope.launch {
                TriggerChannel.getInstance().sendDebugMessage(openMsg)
            }
        } catch (t: Throwable) {
            Timber.e(t, "TriggerForwardingChannel: Failed to open channel")
            scope.launch {
                TriggerChannel.getInstance().sendDebugMessage("Open failed: ${t.message}")
            }
            future.setException(t)
        }

        return future
    }

    override fun handleData(buffer: Buffer) {
        Timber.d("TriggerForwardingChannel: handleData called, available=${buffer.available()}")
        super.handleData(buffer)
    }

    /**
     * クライアント開始のチャネルがサーバーに承認された時に呼ばれる
     * forwarded-tcpip はサーバー開始なのでこれは通常呼ばれない
     */
    override fun handleOpenSuccess(packetSize: Long, rwSize: Long, maxPacketSize: Long, buffer: Buffer) {
        val msg = "TriggerForwardingChannel: handleOpenSuccess (unexpected for server-initiated channel)"
        Timber.d(msg)
        scope.launch {
            TriggerChannel.getInstance().sendDebugMessage(msg)
        }
    }

    /**
     * チャネルオープンが失敗した時に呼ばれる
     */
    override fun handleOpenFailure(buffer: Buffer) {
        val reasonCode = buffer.getInt()
        val message = buffer.getString()
        val lang = buffer.getString()
        val msg = "TriggerForwardingChannel: handleOpenFailure reason=$reasonCode msg=$message"
        Timber.e(msg)
        scope.launch {
            TriggerChannel.getInstance().sendDebugMessage(msg)
        }
    }

    override fun doWriteData(data: ByteArray, off: Int, len: Long) {
        try {
            val received = String(data, off, len.toInt(), Charsets.UTF_8)
            Timber.d("TriggerForwardingChannel: Received ${len} bytes: '$received'")

            scope.launch {
                TriggerChannel.getInstance().sendDebugMessage("Received: $received")
            }

            // データをバッファに追加
            dataBuffer.append(received)

            // 改行があればデータを処理
            if (received.contains("\n") || received.contains("\r")) {
                processBuffer()
            }
        } catch (e: Exception) {
            Timber.e(e, "TriggerForwardingChannel: Error in doWriteData")
            scope.launch {
                TriggerChannel.getInstance().sendDebugMessage("doWriteData error: ${e.message}")
            }
        }
    }

    override fun handleEof() {
        super.handleEof()
        Timber.d("TriggerForwardingChannel: EOF received")
        processBuffer()
    }

    private fun processBuffer() {
        if (dataBuffer.isEmpty()) return

        val fullData = dataBuffer.toString().trim()
        dataBuffer.clear()

        if (fullData.isNotEmpty()) {
            scope.launch {
                try {
                    val inputStream = ByteArrayInputStream(fullData.toByteArray(Charsets.UTF_8))
                    TriggerChannel.getInstance().handleIncomingData(inputStream)
                } catch (e: Exception) {
                    Timber.e(e, "Error processing trigger data")
                    TriggerChannel.getInstance().sendDebugMessage("Error: ${e.message}")
                }
            }
            // データの受信が完了したらチャネルを閉じて、クライアント(nc)を終了させる
            close(false)
        }
    }

    override fun doWriteExtendedData(data: ByteArray, off: Int, len: Long) {
        Timber.d("TriggerForwardingChannel: Received extended data (${len} bytes, ignored)")
    }

    /**
     * チャネルオープン確認メッセージをサーバーに送信
     */
    private fun sendOpenConfirmation() {
        val session = getSession()
        val buffer = session.createBuffer(
            org.apache.sshd.common.SshConstants.SSH_MSG_CHANNEL_OPEN_CONFIRMATION,
            64 // Sufficient size
        )
        buffer.putInt(getRecipient().toLong())
        buffer.putInt(getChannelId().toLong())
        buffer.putInt(localWindow.size.toLong())
        buffer.putInt(localWindow.packetSize.toLong())
        session.writePacket(buffer)
    }

    private fun initWindow(window: Window, size: Long, packetSize: Long, resolver: PropertyResolver) {
        try {
            val method = Window::class.java.getDeclaredMethod("init", Long::class.javaPrimitiveType, Long::class.javaPrimitiveType, PropertyResolver::class.java)
            method.isAccessible = true
            method.invoke(window, size, packetSize, resolver)
        } catch (e: Exception) {
            Timber.e(e, "Failed to init window via reflection")
            scope.launch {
                TriggerChannel.getInstance().sendDebugMessage("Window init failed: ${e.message}")
            }
        }
    }
}
