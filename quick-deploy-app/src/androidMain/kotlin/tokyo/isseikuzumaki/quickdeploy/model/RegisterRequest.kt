package tokyo.isseikuzumaki.quickdeploy.model

import kotlinx.serialization.Serializable

/**
 * Request payload for device registration (A-001)
 */
@Serializable
data class RegisterRequest(
    val fcmToken: String,
    val deviceInfo: DeviceInfo
)
