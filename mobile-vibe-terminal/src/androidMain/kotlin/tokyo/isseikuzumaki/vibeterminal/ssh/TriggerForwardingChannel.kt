package tokyo.isseikuzumaki.vibeterminal.ssh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.sshd.client.channel.AbstractClientChannel
import org.apache.sshd.common.util.buffer.Buffer
import timber.log.Timber
import java.io.ByteArrayInputStream

/**
 * SSH Port Forwarding (forwarded-tcpip) request interceptor.
 * It intercepts data directed to the forwarded port and passes it to the TriggerChannel.
 */
class TriggerForwardingChannel : AbstractClientChannel("forwarded-tcpip") {

    override fun doOpen() {
        val msg = "TriggerForwardingChannel: Channel opened (Interceptor active)"
        Timber.d(msg)
        //CoroutineScope(Dispatchers.IO).launch {
        //    TriggerChannel.getInstance().sendDebugMessage(msg)
        //}
    }

    override fun doWriteData(buffer: ByteArray, off: Int, len: Long) {
        
        // Process asynchronously to avoid blocking SSH I/O thread
        //CoroutineScope(Dispatchers.IO).launch {
         //   TriggerChannel.getInstance().sendDebugMessage("Interceptor received: '$buffer'")
            
            //val data = String(buffer, off, len.toInt(), Charsets.UTF_8).trim()
            //if (data.isNotEmpty()) {
            //    // Wrap in InputStream to match existing interface
            //    val inputStream = ByteArrayInputStream(data.toByteArray(Charsets.UTF_8))
            //    TriggerChannel.getInstance().handleIncomingData(inputStream)
            //}
            
        //}

        // Close channel asynchronously to signal completion
        close(false)
    }

    override fun doWriteExtendedData(buffer: ByteArray, off: Int, len: Long) {
        Timber.d("TriggerForwardingChannel: Received extended data (ignored)")
    }
}
