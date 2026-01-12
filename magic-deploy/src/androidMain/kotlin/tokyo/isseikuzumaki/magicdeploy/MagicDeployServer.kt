package tokyo.isseikuzumaki.magicdeploy

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.ServerSocket

class MagicDeployServer(private val context: Context) {
    private val PORT = 8989
    private val TAG = "MagicDeploy"

    suspend fun start() = withContext(Dispatchers.IO) {
        try {
            val serverSocket = ServerSocket(PORT)
            Log.d(TAG, "Magic Deploy Server started on port $PORT")

            while (true) {
                val client = serverSocket.accept()
                Log.d(TAG, "Client connected: ${client.inetAddress}")

                try {
                    val inputStream = client.getInputStream()
                    val outputDir = File(context.cacheDir, "magic_deploy")
                    if (!outputDir.exists()) outputDir.mkdirs()
                    val outputFile = File(outputDir, "update.apk")
                    
                    if (outputFile.exists()) outputFile.delete()

                    FileOutputStream(outputFile).use { fos ->
                        inputStream.copyTo(fos)
                    }
                    Log.d(TAG, "APK received: ${outputFile.length()} bytes")

                    installApk(outputFile)

                } catch (e: Exception) {
                    Log.e(TAG, "Error receiving APK", e)
                } finally {
                    client.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Server failed", e)
        }
    }

    private fun installApk(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.magicdeploy.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "Installation intent started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start installation", e)
        }
    }
}
