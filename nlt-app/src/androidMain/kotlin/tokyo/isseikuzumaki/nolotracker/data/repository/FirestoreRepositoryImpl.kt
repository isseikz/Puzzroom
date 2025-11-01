package tokyo.isseikuzumaki.nolotracker.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import tokyo.isseikuzumaki.nolotracker.data.model.NotificationRecord
import tokyo.isseikuzumaki.nolotracker.data.model.UserSettings

/**
 * Repository for managing NotificationRecord and UserSettings data in Firestore.
 * 
 * Implements FR 3.0 (data persistence), FR 3.1 (Firestore storage),
 * FR 3.2 (secure user-specific collections), and FR 4.1.2 (filtering and analysis).
 */
class FirestoreRepositoryImpl {
    
    private val tag = "FirestoreRepository"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // FR 3.2: Collection path structure
    // /artifacts/{app_id}/users/{userId}/recorded_notifications
    private val appId = "nlt_app"
    
    /**
     * Saves a notification record to Firestore.
     * 
     * FR 3.1: Securely stores all notification data
     * NFR 4.1.2: Completes within 500ms
     * NFR 4.2.1: Uses secure user-specific collection
     * 
     * @param record Notification record to save
     */
    suspend fun saveNotificationRecord(record: NotificationRecord) {
        try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                Log.e(tag, "Cannot save record: user not authenticated")
                return
            }
            
            val collectionPath = getCollectionPath(userId)
            
            // Convert to map for Firestore (time will be set by server timestamp)
            val data = mapOf(
                "id" to record.id,
                "userId" to userId,
                "packageName" to record.packageName,
                "title" to record.title,
                "text" to record.text,
                "time" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "latitude" to record.latitude,
                "longitude" to record.longitude,
                "isParsed" to record.isParsed,
                "parsedAmount" to record.parsedAmount,
                "parsedMerchant" to record.parsedMerchant
            )
            
            // Save to Firestore
            firestore.collection(collectionPath)
                .document(record.id)
                .set(data)
                .await()
            
            Log.d(tag, "Notification record saved successfully: ${record.id}")
            
        } catch (e: Exception) {
            Log.e(tag, "Error saving notification record", e)
            throw e
        }
    }
    
    /**
     * Retrieves notification records for the current user.
     * 
     * FR 4.1, FR 4.2: Provides data for calendar and map views
     * 
     * @param startTime Optional start time filter (Instant)
     * @param endTime Optional end time filter (Instant)
     * @return Flow of notification records
     */
    fun getNotificationRecords(
        startTime: Long? = null,
        endTime: Long? = null
    ): Flow<List<NotificationRecord>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            Log.e(tag, "Cannot retrieve records: user not authenticated")
            close()
            return@callbackFlow
        }
        
        val collectionPath = getCollectionPath(userId)
        var query: Query = firestore.collection(collectionPath)
            .orderBy("time", Query.Direction.DESCENDING)
        
        // Apply time filters if provided
        if (startTime != null) {
            query = query.whereGreaterThanOrEqualTo("time", startTime)
        }
        if (endTime != null) {
            query = query.whereLessThanOrEqualTo("time", endTime)
        }
        
        // FR 4.1: Real-time updates with onSnapshot
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error listening to notifications", error)
                close(error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val records = snapshot.documents.mapNotNull { doc ->
                    try {
                        NotificationRecord(
                            id = doc.getString("id") ?: "",
                            userId = doc.getString("userId") ?: "",
                            packageName = doc.getString("packageName") ?: "",
                            title = doc.getString("title"),
                            text = doc.getString("text") ?: "",
                            time = doc.getTimestamp("time")?.toDate()?.time?.let {
                                kotlinx.datetime.Instant.fromEpochMilliseconds(it)
                            },
                            latitude = doc.getDouble("latitude"),
                            longitude = doc.getDouble("longitude"),
                            isParsed = doc.getBoolean("isParsed") ?: false,
                            parsedAmount = doc.getString("parsedAmount"),
                            parsedMerchant = doc.getString("parsedMerchant")
                        )
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing document ${doc.id}", e)
                        null
                    }
                }
                
                trySend(records)
            }
        }
        
        awaitClose {
            registration.remove()
        }
    }
    
    /**
     * Retrieves notification records with location data only.
     * 
     * FR 4.3: Provides data for map view markers
     * 
     * @return Flow of notification records with valid location
     */
    fun getNotificationRecordsWithLocation(): Flow<List<NotificationRecord>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            close()
            return@callbackFlow
        }
        
        val collectionPath = getCollectionPath(userId)
        
        // Query only records with location data
        val query = firestore.collection(collectionPath)
            .whereNotEqualTo("latitude", null)
            .orderBy("latitude") // Required for whereNotEqualTo
            .orderBy("time", Query.Direction.DESCENDING)
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error listening to notifications with location", error)
                close(error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val records = snapshot.documents.mapNotNull { doc ->
                    try {
                        val latitude = doc.getDouble("latitude")
                        val longitude = doc.getDouble("longitude")
                        
                        if (latitude != null && longitude != null) {
                            NotificationRecord(
                                id = doc.getString("id") ?: "",
                                userId = doc.getString("userId") ?: "",
                                packageName = doc.getString("packageName") ?: "",
                                title = doc.getString("title"),
                                text = doc.getString("text") ?: "",
                                time = doc.getTimestamp("time")?.toDate()?.time?.let {
                                    kotlinx.datetime.Instant.fromEpochMilliseconds(it)
                                },
                                latitude = latitude,
                                longitude = longitude,
                                isParsed = doc.getBoolean("isParsed") ?: false,
                                parsedAmount = doc.getString("parsedAmount"),
                                parsedMerchant = doc.getString("parsedMerchant")
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing document ${doc.id}", e)
                        null
                    }
                }
                
                trySend(records)
            }
        }
        
        awaitClose {
            registration.remove()
        }
    }
    
    /**
     * Saves user settings to Firestore.
     * 
     * FR 4.1.2: Persist target applications list and keyword filters
     * Stores in First Normal Form (1NF) - all fields are atomic
     * 
     * @param settings User settings to save
     */
    suspend fun saveUserSettings(settings: UserSettings) {
        try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                Log.e(tag, "Cannot save settings: user not authenticated")
                return
            }
            
            val settingsPath = getSettingsDocumentPath(userId)
            
            // Convert to map for Firestore (First Normal Form)
            val data = mapOf(
                "userId" to userId,
                "targetPackages" to settings.targetPackages,
                "keywords" to settings.keywords
            )
            
            // Save to Firestore
            firestore.document(settingsPath)
                .set(data)
                .await()
            
            Log.d(tag, "User settings saved successfully for user: $userId")
            
        } catch (e: Exception) {
            Log.e(tag, "Error saving user settings", e)
            throw e
        }
    }
    
    /**
     * Loads user settings from Firestore.
     * 
     * FR 4.1.2: Load target applications list and keyword filters on app start
     * 
     * @return UserSettings or null if not found
     */
    suspend fun getUserSettings(): UserSettings? {
        try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                Log.e(tag, "Cannot load settings: user not authenticated")
                return null
            }
            
            val settingsPath = getSettingsDocumentPath(userId)
            
            val document = firestore.document(settingsPath)
                .get()
                .await()
            
            if (!document.exists()) {
                Log.d(tag, "No settings found for user: $userId")
                return null
            }
            
            // Parse from Firestore document
            val targetPackages = document.get("targetPackages") as? List<*>
            val keywords = document.get("keywords") as? List<*>
            
            return UserSettings(
                userId = userId,
                targetPackages = targetPackages?.mapNotNull { it as? String } ?: emptyList(),
                keywords = keywords?.mapNotNull { it as? String } ?: emptyList()
            )
            
        } catch (e: Exception) {
            Log.e(tag, "Error loading user settings", e)
            throw e
        }
    }
    
    /**
     * Gets the Firestore collection path for user's notification records.
     * 
     * FR 3.2: Private user collection path
     * 
     * @param userId User ID
     * @return Firestore collection path
     */
    private fun getCollectionPath(userId: String): String {
        return "/artifacts/$appId/users/$userId/recorded_notifications"
    }
    
    /**
     * Gets the Firestore document path for user's settings.
     * 
     * FR 4.1.2: Private user settings document path
     * 
     * @param userId User ID
     * @return Firestore document path
     */
    private fun getSettingsDocumentPath(userId: String): String {
        return "/artifacts/$appId/users/$userId/settings"
    }
    
    /**
     * Gets current authenticated user ID.
     * 
     * @return User ID or empty string if not authenticated
     */
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }
    
    companion object {
        private var instance: FirestoreRepositoryImpl? = null
        
        /**
         * Gets singleton instance of repository.
         */
        fun getInstance(): FirestoreRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: FirestoreRepositoryImpl().also { instance = it }
            }
        }
        
        /**
         * Static method for saving notification record.
         * Used by NotificationListenerService.
         */
        suspend fun saveNotificationRecord(record: NotificationRecord) {
            getInstance().saveNotificationRecord(record)
        }
    }
}
