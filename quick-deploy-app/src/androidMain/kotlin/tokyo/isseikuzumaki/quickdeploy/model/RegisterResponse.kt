package tokyo.isseikuzumaki.quickdeploy.model

import kotlinx.serialization.Serializable

/**
 * Response from device registration (A-001)
 */
@Serializable
data class RegisterResponse(
    val deviceToken: String,
    val uploadUrl: String,
    val downloadUrl: String
)
