package tokyo.isseikuzumaki.nlt.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test suite for NotificationRecord data model.
 */
class NotificationRecordTest {
    
    @Test
    fun `getSummaryText returns parsed payment info when available`() {
        val record = NotificationRecord(
            id = "test-1",
            userId = "user-1",
            packageName = "com.payment.app",
            title = "Payment Complete",
            text = "Paid 1500 JPY at Starbucks",
            isParsed = true,
            parsedAmount = "1500 JPY",
            parsedMerchant = "Starbucks"
        )
        
        assertEquals("Paid 1500 JPY at Starbucks", record.getSummaryText())
    }
    
    @Test
    fun `getSummaryText returns title when payment info unavailable`() {
        val record = NotificationRecord(
            id = "test-2",
            userId = "user-1",
            packageName = "com.other.app",
            title = "New Message",
            text = "You have a new message",
            isParsed = false,
            parsedAmount = null,
            parsedMerchant = null
        )
        
        assertEquals("New Message", record.getSummaryText())
    }
    
    @Test
    fun `getSummaryText returns package name when title and payment info unavailable`() {
        val record = NotificationRecord(
            id = "test-3",
            userId = "user-1",
            packageName = "com.example.app",
            title = null,
            text = "Some notification",
            isParsed = false,
            parsedAmount = null,
            parsedMerchant = null
        )
        
        assertEquals("com.example.app", record.getSummaryText())
    }
    
    @Test
    fun `hasLocation returns true when both coordinates are present`() {
        val record = NotificationRecord(
            id = "test-4",
            userId = "user-1",
            packageName = "com.app",
            text = "Test",
            latitude = 35.6812,
            longitude = 139.7671
        )
        
        assertTrue(record.hasLocation())
    }
    
    @Test
    fun `hasLocation returns false when latitude is null`() {
        val record = NotificationRecord(
            id = "test-5",
            userId = "user-1",
            packageName = "com.app",
            text = "Test",
            latitude = null,
            longitude = 139.7671
        )
        
        assertFalse(record.hasLocation())
    }
    
    @Test
    fun `hasLocation returns false when longitude is null`() {
        val record = NotificationRecord(
            id = "test-6",
            userId = "user-1",
            packageName = "com.app",
            text = "Test",
            latitude = 35.6812,
            longitude = null
        )
        
        assertFalse(record.hasLocation())
    }
    
    @Test
    fun `hasLocation returns false when both coordinates are null`() {
        val record = NotificationRecord(
            id = "test-7",
            userId = "user-1",
            packageName = "com.app",
            text = "Test",
            latitude = null,
            longitude = null
        )
        
        assertFalse(record.hasLocation())
    }
    
    @Test
    fun `default values are correctly initialized`() {
        val record = NotificationRecord()
        
        assertEquals("", record.id)
        assertEquals("", record.userId)
        assertEquals("", record.packageName)
        assertEquals(null, record.title)
        assertEquals("", record.text)
        assertEquals(null, record.time)
        assertEquals(null, record.latitude)
        assertEquals(null, record.longitude)
        assertFalse(record.isParsed)
        assertEquals(null, record.parsedAmount)
        assertEquals(null, record.parsedMerchant)
    }
}
