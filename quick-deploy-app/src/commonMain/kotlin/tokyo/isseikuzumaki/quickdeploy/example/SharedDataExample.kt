package tokyo.isseikuzumaki.quickdeploy.example

import tokyo.isseikuzumaki.quickdeploy.data.Message
import tokyo.isseikuzumaki.quickdeploy.data.MessageRequest
import tokyo.isseikuzumaki.quickdeploy.data.MessageResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Example usage of shared data structures.
 * 
 * This demonstrates how the same data models can be used across:
 * - Android/iOS clients
 * - Firebase Cloud Functions
 * 
 * The models are defined once in commonMain and can be serialized/deserialized
 * consistently across all platforms.
 */
object SharedDataExample {
    
    /**
     * Example: Creating and serializing a Message
     */
    fun createMessage(): String {
        val message = Message(
            text = "Hello from Kotlin!",
            timestamp = System.currentTimeMillis()
        )
        
        // Serialize to JSON
        val json = Json.encodeToString(message)
        println("Serialized message: $json")
        
        return json
    }
    
    /**
     * Example: Deserializing a Message from JSON
     */
    fun parseMessage(json: String): Message {
        val message = Json.decodeFromString<Message>(json)
        println("Parsed message: ${message.text} at ${message.timestamp}")
        
        return message
    }
    
    /**
     * Example: Creating a request to send to Firebase Functions
     */
    fun createRequest(text: String): String {
        val request = MessageRequest(original = text)
        return Json.encodeToString(request)
    }
    
    /**
     * Example: Parsing a response from Firebase Functions
     */
    fun parseResponse(json: String): MessageResponse {
        return Json.decodeFromString<MessageResponse>(json)
    }
    
    /**
     * Simulated workflow:
     * 1. Client creates a message
     * 2. Sends to Firebase Functions
     * 3. Function processes and returns result
     * 4. Client parses the response
     */
    fun simulateWorkflow() {
        // Step 1: Create message
        val originalText = "hello world"
        val request = createRequest(originalText)
        println("Request to Firebase: $request")
        
        // Step 2: Simulate Firebase Functions response
        // In reality, this would come from an HTTP call or Firestore listener
        val simulatedResponse = """{"result":"HELLO WORLD"}"""
        
        // Step 3: Parse response
        val response = parseResponse(simulatedResponse)
        println("Response from Firebase: ${response.result}")
        
        // Step 4: Create a Message object for storage
        val message = Message(
            text = response.result,
            timestamp = System.currentTimeMillis()
        )
        println("Final message: ${Json.encodeToString(message)}")
    }
}
