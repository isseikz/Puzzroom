package tokyo.isseikuzumaki.nlt.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Core data class for storing notification records with location and parsed payment information.
 * 
 * This model represents a notification that has been captured by the NotificationListenerService,
 * along with its associated location data and any parsed payment information.
 * 
 * @property id Unique identifier for this notification record
 * @property userId User ID of the authenticated user who owns this record
 * @property packageName Source application package name (e.g., "com.example.paymentapp")
 * @property title Notification title (optional)
 * @property text Raw notification body text
 * @property time UTC timestamp when the notification was posted (server timestamp from Firestore)
 * @property latitude Captured latitude coordinate (null if location unavailable)
 * @property longitude Captured longitude coordinate (null if location unavailable)
 * @property isParsed Flag indicating whether payment parsing was successful
 * @property parsedAmount Extracted payment amount with currency (e.g., "1500 JPY")
 * @property parsedMerchant Extracted merchant/store name (e.g., "Starbucks")
 */
@Serializable
data class NotificationRecord(
    val id: String = "",
    val userId: String = "",
    val packageName: String = "",
    val title: String? = null,
    val text: String = "",
    
    // Firestore timestamp - will be set on server
    val time: Instant? = null,
    
    // Location data
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    // Parsed payment information
    val isParsed: Boolean = false,
    val parsedAmount: String? = null,
    val parsedMerchant: String? = null
) {
    /**
     * Generates a summary text for map marker info windows.
     * 
     * Returns formatted payment information if available, otherwise returns
     * the notification title or package name as fallback.
     * 
     * @return Summary text for display in UI
     */
    fun getSummaryText(): String {
        return if (parsedMerchant != null && parsedAmount != null) {
            "Paid $parsedAmount at $parsedMerchant"
        } else {
            title ?: packageName
        }
    }
    
    /**
     * Checks if this record has valid location data.
     * 
     * @return true if both latitude and longitude are non-null
     */
    fun hasLocation(): Boolean = latitude != null && longitude != null
}
