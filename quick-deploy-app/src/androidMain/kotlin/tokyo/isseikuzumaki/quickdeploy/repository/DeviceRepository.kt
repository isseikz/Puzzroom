package tokyo.isseikuzumaki.quickdeploy.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import tokyo.isseikuzumaki.quickdeploy.BuildConfig
import tokyo.isseikuzumaki.quickdeploy.api.QuickDeployApiClient
import tokyo.isseikuzumaki.quickdeploy.model.DeviceInfo
import tokyo.isseikuzumaki.quickdeploy.model.RegisterRequest
import tokyo.isseikuzumaki.quickdeploy.model.RegisterResponse

/**
 * Repository for managing device registration and URLs
 */
class DeviceRepository(
    private val context: Context,
    private val apiClient: QuickDeployApiClient
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get stored device token
     */
    fun getDeviceToken(): String? = prefs.getString(KEY_DEVICE_TOKEN, null)

    /**
     * Get stored upload URL
     */
    fun getUploadUrl(): String? = prefs.getString(KEY_UPLOAD_URL, null)

    /**
     * Get stored download URL
     */
    fun getDownloadUrl(): String? = prefs.getString(KEY_DOWNLOAD_URL, null)

    /**
     * Check if device is registered
     */
    fun isRegistered(): Boolean = getDeviceToken() != null

    /**
     * Register device and store URLs (C-001, A-001)
     */
    suspend fun registerDevice(): Result<RegisterResponse> {
        return try {
            // Get FCM token
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "Got FCM token: $fcmToken")

            // Get device info
            val deviceInfo = DeviceInfo(
                deviceId = getDeviceId(),
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                appVersion = BuildConfig.VERSION_NAME
            )

            // Register with backend
            val request = RegisterRequest(fcmToken, deviceInfo)
            val result = apiClient.registerDevice(request)

            result.onSuccess { response ->
                // Store registration data
                prefs.edit()
                    .putString(KEY_DEVICE_TOKEN, response.deviceToken)
                    .putString(KEY_UPLOAD_URL, response.uploadUrl)
                    .putString(KEY_DOWNLOAD_URL, response.downloadUrl)
                    .apply()

                Log.d(TAG, "Device registered and stored: ${response.deviceToken}")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device", e)
            Result.failure(e)
        }
    }

    /**
     * Clear registration data
     */
    fun clearRegistration() {
        prefs.edit()
            .remove(KEY_DEVICE_TOKEN)
            .remove(KEY_UPLOAD_URL)
            .remove(KEY_DOWNLOAD_URL)
            .apply()
        Log.d(TAG, "Registration data cleared")
    }

    /**
     * Get device ID (Android ID)
     */
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    companion object {
        private const val TAG = "DeviceRepository"
        private const val PREFS_NAME = "quick_deploy_prefs"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_UPLOAD_URL = "upload_url"
        private const val KEY_DOWNLOAD_URL = "download_url"
    }
}
