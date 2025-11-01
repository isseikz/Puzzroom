package tokyo.isseikuzumaki.quickdeploy.data

import kotlinx.serialization.Serializable

/**
 * Shared data structure for messages.
 * This class can be used on both client (Android/iOS) and server (Firebase Functions).
 */
@Serializable
data class Message(
    val text: String,
    val timestamp: Long = 0L
)

/**
 * Shared data structure for message requests.
 */
@Serializable
data class MessageRequest(
    val original: String
)

/**
 * Shared data structure for message responses.
 */
@Serializable
data class MessageResponse(
    val result: String
)
