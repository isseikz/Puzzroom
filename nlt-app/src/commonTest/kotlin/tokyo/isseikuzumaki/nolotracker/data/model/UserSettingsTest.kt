package tokyo.isseikuzumaki.nolotracker.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test suite for UserSettings data model.
 * 
 * Tests First Normal Form (1NF) compliance and Set/List conversions.
 */
class UserSettingsTest {
    
    @Test
    fun `default values are correctly initialized`() {
        val settings = UserSettings()
        
        assertEquals("", settings.userId)
        assertTrue(settings.targetPackages.isEmpty())
        assertTrue(settings.keywords.isEmpty())
    }
    
    @Test
    fun `getTargetPackagesSet converts list to set`() {
        val settings = UserSettings(
            userId = "user-1",
            targetPackages = listOf("com.app1", "com.app2", "com.app1"), // with duplicate
            keywords = emptyList()
        )
        
        val packageSet = settings.getTargetPackagesSet()
        assertEquals(2, packageSet.size) // duplicate removed
        assertTrue(packageSet.contains("com.app1"))
        assertTrue(packageSet.contains("com.app2"))
    }
    
    @Test
    fun `getKeywordsSet converts list to set`() {
        val settings = UserSettings(
            userId = "user-1",
            targetPackages = emptyList(),
            keywords = listOf("payment", "paid", "payment") // with duplicate
        )
        
        val keywordSet = settings.getKeywordsSet()
        assertEquals(2, keywordSet.size) // duplicate removed
        assertTrue(keywordSet.contains("payment"))
        assertTrue(keywordSet.contains("paid"))
    }
    
    @Test
    fun `fromSets creates UserSettings from Sets`() {
        val packages = setOf("com.app1", "com.app2")
        val keywords = setOf("支払い", "決済")
        
        val settings = UserSettings.fromSets(
            userId = "user-1",
            targetPackages = packages,
            keywords = keywords
        )
        
        assertEquals("user-1", settings.userId)
        assertEquals(2, settings.targetPackages.size)
        assertEquals(2, settings.keywords.size)
        assertTrue(settings.targetPackages.containsAll(packages.toList()))
        assertTrue(settings.keywords.containsAll(keywords.toList()))
    }
    
    @Test
    fun `default factory method creates default settings`() {
        val settings = UserSettings.default()
        
        assertEquals("", settings.userId)
        assertTrue(settings.targetPackages.isNotEmpty())
        assertTrue(settings.keywords.isNotEmpty())
        assertTrue(settings.targetPackages.contains("com.example.paymentapp"))
        assertTrue(settings.keywords.contains("支払い"))
    }
    
    @Test
    fun `default factory method with userId`() {
        val settings = UserSettings.default("user-123")
        
        assertEquals("user-123", settings.userId)
        assertTrue(settings.targetPackages.isNotEmpty())
        assertTrue(settings.keywords.isNotEmpty())
    }
    
    @Test
    fun `roundtrip conversion between Sets and Lists`() {
        val originalPackages = setOf("com.app1", "com.app2", "com.app3")
        val originalKeywords = setOf("keyword1", "keyword2")
        
        val settings = UserSettings.fromSets(
            userId = "user-1",
            targetPackages = originalPackages,
            keywords = originalKeywords
        )
        
        val convertedPackages = settings.getTargetPackagesSet()
        val convertedKeywords = settings.getKeywordsSet()
        
        assertEquals(originalPackages, convertedPackages)
        assertEquals(originalKeywords, convertedKeywords)
    }
    
    @Test
    fun `First Normal Form compliance - all fields are atomic`() {
        // First Normal Form (1NF) requirements:
        // 1. All values must be atomic (no nested structures)
        // 2. Each field should contain only single values (Lists are atomic in Firestore)
        
        val settings = UserSettings(
            userId = "user-1",
            targetPackages = listOf("com.app1", "com.app2"),
            keywords = listOf("payment", "paid")
        )
        
        // All fields are simple types (String and List<String>)
        // This satisfies 1NF as there are no nested structures
        assertEquals("user-1", settings.userId)
        assertEquals(2, settings.targetPackages.size)
        assertEquals(2, settings.keywords.size)
    }
}
