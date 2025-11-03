package tokyo.isseikuzumaki.quickdeploy.model

import kotlinx.serialization.Serializable

/**
 * Device information for registration
 */
@Serializable
data class DeviceInfo(
    val deviceId: String,
    val deviceModel: String,
    val osVersion: String,
    val appVersion: String
)
