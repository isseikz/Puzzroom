package tokyo.isseikuzumaki.quickdeploy.functions

import tokyo.isseikuzumaki.quickdeploy.data.Message
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Firebase Cloud Functions implementation using Kotlin/JS.
 * This file demonstrates how to share data structures between client and server.
 */

// Initialize Firebase Admin
private val app = initializeApp()
private val db = getFirestore()

/**
 * HTTP Cloud Function that converts text to uppercase.
 * Example: https://us-central1-<project-id>.cloudfunctions.net/makeUppercase?text=hello
 */
@JsExport
val makeUppercase = https.onRequest { req, res ->
    try {
        val text = req.asDynamic().query.text as? String ?: ""
        val uppercase = text.uppercase()
        
        logger.info("Converting text to uppercase: $text -> $uppercase")
        
        res.json(js("""{ "result": "$uppercase" }"""))
    } catch (e: Exception) {
        logger.error("Error in makeUppercase: ${e.message}")
        res.status(500).send("Internal Server Error")
    }
}

/**
 * Firestore-triggered Cloud Function that processes new messages.
 * Triggered when a new document is created in the 'messages' collection.
 */
@JsExport
val addMessage = firestore.onDocumentCreated("messages/{messageId}") { event ->
    try {
        val snapshot = event.asDynamic().data as DocumentSnapshot
        val original = snapshot.get("original") as? String ?: ""
        
        logger.info("Processing new message: $original")
        
        // Create a Message object using shared data structure
        val message = Message(
            text = original.uppercase(),
            timestamp = js("Date.now()") as Long
        )
        
        // Serialize to JSON using kotlinx.serialization
        val messageJson = Json.encodeToString(message)
        logger.info("Created message: $messageJson")
        
        // Store the uppercase version back to Firestore
        val messageId = event.asDynamic().params.messageId as String
        db.collection("messages")
            .doc(messageId)
            .set(js("""{ "uppercase": "${message.text}", "timestamp": ${message.timestamp} }"""))
        
        logger.info("Message processed successfully")
    } catch (e: Exception) {
        logger.error("Error in addMessage: ${e.message}")
        throw e
    }
}

/**
 * Main entry point for the functions module.
 * This is required for the Kotlin/JS compiler.
 */
fun main() {
    // This function is called when the module is loaded
    logger.info("Firebase Functions module loaded")
}
