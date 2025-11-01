package tokyo.isseikuzumaki.nolotracker.service

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.*
import tokyo.isseikuzumaki.nolotracker.data.model.NotificationRecord
import java.util.UUID

/**
 * Android NotificationListenerService for monitoring and recording notifications.
 * 
 * This service runs in the background and listens for notifications from specified apps.
 * When a notification matches the filter criteria, it:
 * 1. Extracts notification details (title, text, package name)
 * 2. Attempts to parse payment information
 * 3. Triggers location capture
 * 4. Saves the record to Firestore
 * 
 * Requires: android.permission.BIND_NOTIFICATION_LISTENER_SERVICE
 */
class NLTNotificationListenerService : NotificationListenerService() {
    
    private val tag = "NLTNotificationListener"
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Target apps to monitor (configurable via settings)
    private var targetPackages = setOf<String>()
    
    // Keywords to filter notifications (configurable via settings)
    private var filterKeywords = setOf<String>()
    
    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "NLT NotificationListenerService created")
        
        // Load user preferences for target apps and keywords
        loadUserPreferences()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(tag, "NLT NotificationListenerService destroyed")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val packageName = sbn.packageName
            val notification = sbn.notification
            
            // FR 1.3: Filter by target application
            if (targetPackages.isNotEmpty() && packageName !in targetPackages) {
                Log.d(tag, "Notification from $packageName ignored (not in target list)")
                return
            }
            
            // Extract notification content
            val extras = notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            
            // FR 1.4: Filter by keywords
            if (filterKeywords.isNotEmpty()) {
                val hasMatchingKeyword = filterKeywords.any { keyword ->
                    title?.contains(keyword, ignoreCase = true) == true ||
                    text.contains(keyword, ignoreCase = true)
                }
                
                if (!hasMatchingKeyword) {
                    Log.d(tag, "Notification ignored (no matching keywords)")
                    return
                }
            }
            
            Log.d(tag, "Processing notification from $packageName: $title")
            
            // Process the notification asynchronously
            serviceScope.launch {
                processNotification(packageName, title, text)
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error processing notification", e)
        }
    }
    
    /**
     * Processes a notification that passed filtering criteria.
     * 
     * @param packageName Source app package name
     * @param title Notification title
     * @param text Notification text body
     */
    private suspend fun processNotification(
        packageName: String,
        title: String?,
        text: String
    ) {
        try {
            // FR 2.1: Trigger immediate location capture
            val location = captureLocation()
            
            // Create notification record
            val record = NotificationRecord(
                id = UUID.randomUUID().toString(),
                userId = getCurrentUserId(),
                packageName = packageName,
                title = title,
                text = text,
                latitude = location?.latitude,
                longitude = location?.longitude
            )
            
            // FR 3.1: Save to Firestore
            saveToFirestore(record)
            
            Log.d(tag, "Notification record saved: ${record.id}")
            
        } catch (e: Exception) {
            Log.e(tag, "Error processing notification", e)
        }
    }
    
    /**
     * Captures current location with 5-second timeout.
     * 
     * FR 2.1, FR 2.3: Implements high-priority location request with strict timeout.
     * 
     * @return Location data or null if unavailable within timeout
     */
    private suspend fun captureLocation(): LocationData? {
        return withTimeoutOrNull(5000) {
            try {
                // FR 2.2: Use FusedLocationProviderClient via LocationServiceImpl
                LocationServiceImpl.getCurrentLocation(applicationContext)
            } catch (e: Exception) {
                Log.e(tag, "Location capture failed", e)
                null
            }
        }
    }
    
    /**
     * Saves notification record to Firestore.
     * 
     * FR 3.1, NFR 4.1.2: Saves data within 500ms to private user collection.
     * 
     * @param record Notification record to save
     */
    private suspend fun saveToFirestore(record: NotificationRecord) {
        try {
            // FR 3.1, FR 3.2: Save to user-specific Firestore collection
            tokyo.isseikuzumaki.nolotracker.data.repository.FirestoreRepositoryImpl.saveNotificationRecord(record)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save to Firestore", e)
            throw e
        }
    }
    
    /**
     * Gets current authenticated user ID.
     * 
     * @return User ID or empty string if not authenticated
     */
    private fun getCurrentUserId(): String {
        // Use Firebase Auth
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }
    
    /**
     * Loads user preferences for filtering.
     * 
     * Loads target app packages and filter keywords from SharedPreferences
     * or Firestore user settings.
     */
    private fun loadUserPreferences() {
        // Load from SharedPreferences or Firestore
        // Placeholder implementation
        targetPackages = setOf()
        filterKeywords = setOf("支払い", "決済", "payment", "paid")
    }
}
