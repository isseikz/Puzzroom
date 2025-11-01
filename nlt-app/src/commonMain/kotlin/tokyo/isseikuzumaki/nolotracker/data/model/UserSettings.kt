package tokyo.isseikuzumaki.nolotracker.data.model

import kotlinx.serialization.Serializable

/**
 * User settings for notification monitoring and filtering.
 * 
 * This model is stored in Firestore in First Normal Form (1NF):
 * - All fields are atomic (no nested structures)
 * - Sets are stored as Lists in Firestore for compatibility
 * - Associated with Firebase Auth user UID
 * 
 * @property userId User ID of the authenticated user who owns these settings
 * @property targetPackages List of package names to monitor for notifications
 * @property keywords List of keywords to filter notifications
 */
@Serializable
data class UserSettings(
    val userId: String = "",
    val targetPackages: List<String> = emptyList(),
    val keywords: List<String> = emptyList()
) {
    /**
     * Convert to Set for easier manipulation in the app
     */
    fun getTargetPackagesSet(): Set<String> = targetPackages.toSet()
    
    /**
     * Convert to Set for easier manipulation in the app
     */
    fun getKeywordsSet(): Set<String> = keywords.toSet()
    
    companion object {
        /**
         * Create UserSettings from Sets
         */
        fun fromSets(
            userId: String,
            targetPackages: Set<String>,
            keywords: Set<String>
        ): UserSettings {
            return UserSettings(
                userId = userId,
                targetPackages = targetPackages.toList(),
                keywords = keywords.toList()
            )
        }
        
        /**
         * Default settings
         */
        fun default(userId: String = ""): UserSettings {
            return UserSettings(
                userId = userId,
                targetPackages = listOf(
                    "com.example.paymentapp",
                    "jp.co.paymentservice"
                ),
                keywords = listOf(
                    "支払い", "決済", "payment", "paid"
                )
            )
        }
    }
}
