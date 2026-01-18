package tokyo.isseikuzumaki.vibeterminal.macro.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tests for macro model serialization/deserialization.
 */
class MacroSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
        classDiscriminator = "type"
    }

    // ========== MacroAction Serialization Tests ==========

    @Test
    fun directSend_serializesCorrectly() {
        val action: MacroAction = MacroAction.DirectSend("\u001B[A")
        val serialized = json.encodeToString(action)
        assertTrue(serialized.contains("direct_send"), "Expected 'direct_send' in: $serialized")
        assertTrue(serialized.contains("sequence"), "Expected 'sequence' in: $serialized")
    }

    @Test
    fun directSend_deserializesCorrectly() {
        val jsonString = """{"type":"direct_send","sequence":"\u001B[A"}"""
        val action = json.decodeFromString<MacroAction>(jsonString)
        assertTrue(action is MacroAction.DirectSend)
        assertEquals("\u001B[A", (action as MacroAction.DirectSend).sequence)
    }

    @Test
    fun bufferInsert_serializesCorrectly() {
        val action: MacroAction = MacroAction.BufferInsert(":wq")
        val serialized = json.encodeToString(action)
        assertTrue(serialized.contains("buffer_insert"), "Expected 'buffer_insert' in: $serialized")
        assertTrue(serialized.contains("text"), "Expected 'text' in: $serialized")
    }

    @Test
    fun bufferInsert_deserializesCorrectly() {
        val jsonString = """{"type":"buffer_insert","text":":wq"}"""
        val action = json.decodeFromString<MacroAction>(jsonString)
        assertTrue(action is MacroAction.BufferInsert)
        assertEquals(":wq", (action as MacroAction.BufferInsert).text)
    }

    @Test
    fun directSend_roundTrip() {
        val original: MacroAction = MacroAction.DirectSend("\u0003")
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<MacroAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun bufferInsert_roundTrip() {
        val original: MacroAction = MacroAction.BufferInsert("echo hello")
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<MacroAction>(serialized)
        assertEquals(original, deserialized)
    }

    // ========== MacroKeyDefinition Serialization Tests ==========

    @Test
    fun macroKeyDefinition_serializesCorrectly() {
        val key = MacroKeyDefinition(
            id = "test-key",
            label = "TEST",
            action = MacroAction.DirectSend("test"),
            description = "Test key",
            displayOrder = 0
        )
        val serialized = json.encodeToString(key)
        assertTrue(serialized.contains("test-key"))
        assertTrue(serialized.contains("TEST"))
    }

    @Test
    fun macroKeyDefinition_roundTrip() {
        val original = MacroKeyDefinition(
            id = "key-1",
            label = "Key 1",
            action = MacroAction.BufferInsert("hello"),
            description = "First key",
            displayOrder = 5
        )
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<MacroKeyDefinition>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun macroKeyDefinition_withNullDescription_roundTrip() {
        val original = MacroKeyDefinition(
            id = "key-2",
            label = "Key 2",
            action = MacroAction.DirectSend("\t"),
            description = null,
            displayOrder = 0
        )
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<MacroKeyDefinition>(serialized)
        assertEquals(original, deserialized)
    }

    // ========== MacroTabDefinition Serialization Tests ==========

    @Test
    fun macroTabDefinition_serializesCorrectly() {
        val tab = MacroTabDefinition(
            id = "test-tab",
            name = "Test Tab",
            icon = "🔧",
            keys = listOf(
                MacroKeyDefinition("k1", "K1", MacroAction.DirectSend("a"), displayOrder = 0)
            ),
            displayOrder = 0
        )
        val serialized = json.encodeToString(tab)
        assertTrue(serialized.contains("test-tab"))
        assertTrue(serialized.contains("Test Tab"))
    }

    @Test
    fun macroTabDefinition_roundTrip() {
        val original = MacroTabDefinition(
            id = "tab-1",
            name = "Tab One",
            icon = "📁",
            keys = listOf(
                MacroKeyDefinition("k1", "Key1", MacroAction.DirectSend("x"), displayOrder = 0),
                MacroKeyDefinition("k2", "Key2", MacroAction.BufferInsert("y"), displayOrder = 1)
            ),
            displayOrder = 2
        )
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<MacroTabDefinition>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun macroTabDefinition_emptyKeys_roundTrip() {
        val original = MacroTabDefinition(
            id = "empty-tab",
            name = "Empty",
            keys = emptyList(),
            displayOrder = 0
        )
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<MacroTabDefinition>(serialized)
        assertEquals(original, deserialized)
    }

    // ========== MacroPackMetadata Serialization Tests ==========

    @Test
    fun macroPackMetadata_roundTrip() {
        val original = MacroPackMetadata(
            name = "My Pack",
            description = "A custom pack",
            version = "2.0.0",
            author = "TestUser",
            tags = listOf("vim", "terminal"),
            license = "MIT"
        )
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<MacroPackMetadata>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun macroPackMetadata_withDefaults_roundTrip() {
        val original = MacroPackMetadata(name = "Minimal")
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<MacroPackMetadata>(serialized)
        assertEquals(original.name, deserialized.name)
        assertEquals("1.0.0", deserialized.version)
    }

    // ========== MacroPack Serialization Tests ==========

    @Test
    fun macroPack_roundTrip() {
        val original = MacroPack(
            id = "pack-1",
            metadata = MacroPackMetadata(
                name = "Test Pack",
                description = "For testing",
                version = "1.0.0"
            ),
            tabs = listOf(
                MacroTabDefinition(
                    id = "tab-1",
                    name = "Tab 1",
                    keys = listOf(
                        MacroKeyDefinition("k1", "K1", MacroAction.DirectSend("\u001B"), displayOrder = 0)
                    ),
                    displayOrder = 0
                )
            ),
            source = MacroPackSource.USER,
            isActive = true,
            displayOrder = 1
        )
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<MacroPack>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun macroPack_marketplaceSource_roundTrip() {
        val original = MacroPack(
            id = "marketplace-pack",
            metadata = MacroPackMetadata(name = "Pro Pack"),
            tabs = emptyList(),
            source = MacroPackSource.MARKETPLACE,
            marketplaceId = "mp-12345",
            isActive = false,
            displayOrder = 10
        )
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<MacroPack>(serialized)
        assertEquals(original, deserialized)
        assertEquals("mp-12345", deserialized.marketplaceId)
    }

    // ========== MacroPackExport Serialization Tests ==========

    @Test
    fun macroPackExport_roundTrip() {
        val pack = MacroPack(
            id = "export-test",
            metadata = MacroPackMetadata(name = "Export Test"),
            tabs = emptyList(),
            source = MacroPackSource.USER
        )
        val original = MacroPackExport(
            formatVersion = "1.0",
            pack = pack
        )
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<MacroPackExport>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun macroPackExport_containsFormatVersion() {
        val export = MacroPackExport(
            formatVersion = "2.0",
            pack = MacroPack(
                id = "test",
                metadata = MacroPackMetadata(name = "Test"),
                source = MacroPackSource.USER
            )
        )
        val serialized = json.encodeToString(export)
        assertTrue(serialized.contains("format_version"))
        assertTrue(serialized.contains("2.0"))
    }

    // ========== Special Character Tests ==========

    @Test
    fun escapeSequence_preservedInSerialization() {
        val action: MacroAction = MacroAction.DirectSend("\u001B[A") // ESC [ A
        val serialized = json.encodeToString(action)
        val deserialized = json.decodeFromString<MacroAction>(serialized)
        assertEquals(action, deserialized)
    }

    @Test
    fun controlCharacter_preservedInSerialization() {
        val action: MacroAction = MacroAction.DirectSend("\u0003") // CTRL+C
        val serialized = json.encodeToString(action)
        val deserialized = json.decodeFromString<MacroAction>(serialized)
        assertEquals(action, deserialized)
    }

    @Test
    fun unicodeEmoji_preservedInSerialization() {
        val tab = MacroTabDefinition(
            id = "emoji-tab",
            name = "Emoji Tab",
            icon = "🚀🔥💻",
            keys = emptyList(),
            displayOrder = 0
        )
        val serialized = json.encodeToString(tab)
        val deserialized = json.decodeFromString<MacroTabDefinition>(serialized)
        assertEquals("🚀🔥💻", deserialized.icon)
    }

    // ========== MacroPackSource Serialization Tests ==========

    @Test
    fun macroPackSource_builtin_serializes() {
        val serialized = json.encodeToString(MacroPackSource.BUILTIN)
        assertTrue(serialized.contains("builtin"))
    }

    @Test
    fun macroPackSource_user_serializes() {
        val serialized = json.encodeToString(MacroPackSource.USER)
        assertTrue(serialized.contains("user"))
    }

    @Test
    fun macroPackSource_marketplace_serializes() {
        val serialized = json.encodeToString(MacroPackSource.MARKETPLACE)
        assertTrue(serialized.contains("marketplace"))
    }

    @Test
    fun macroPackSource_roundTrip() {
        MacroPackSource.entries.forEach { source ->
            val serialized = json.encodeToString(source)
            val deserialized = json.decodeFromString<MacroPackSource>(serialized)
            assertEquals(source, deserialized)
        }
    }
}
