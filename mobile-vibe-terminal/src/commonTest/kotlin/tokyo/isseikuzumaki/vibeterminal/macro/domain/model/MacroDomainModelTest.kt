package tokyo.isseikuzumaki.vibeterminal.macro.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for macro domain models - equality, defaults, and property behavior.
 */
class MacroDomainModelTest {

    // ========== MacroAction Tests ==========

    @Test
    fun directSend_equality() {
        val action1 = MacroAction.DirectSend("\u001B[A")
        val action2 = MacroAction.DirectSend("\u001B[A")
        val action3 = MacroAction.DirectSend("\u001B[B")

        assertEquals(action1, action2)
        assertNotEquals(action1, action3)
    }

    @Test
    fun bufferInsert_equality() {
        val action1 = MacroAction.BufferInsert("hello")
        val action2 = MacroAction.BufferInsert("hello")
        val action3 = MacroAction.BufferInsert("world")

        assertEquals(action1, action2)
        assertNotEquals(action1, action3)
    }

    @Test
    fun directSend_notEqualTo_bufferInsert() {
        val directSend = MacroAction.DirectSend("test")
        val bufferInsert = MacroAction.BufferInsert("test")

        assertNotEquals<MacroAction>(directSend, bufferInsert)
    }

    // ========== MacroKeyDefinition Tests ==========

    @Test
    fun macroKeyDefinition_defaultDescription_isNull() {
        val key = MacroKeyDefinition(
            id = "test",
            label = "Test",
            action = MacroAction.DirectSend("t")
        )

        assertNull(key.description)
    }

    @Test
    fun macroKeyDefinition_defaultDisplayOrder_isZero() {
        val key = MacroKeyDefinition(
            id = "test",
            label = "Test",
            action = MacroAction.DirectSend("t")
        )

        assertEquals(0, key.displayOrder)
    }

    @Test
    fun macroKeyDefinition_equality() {
        val key1 = MacroKeyDefinition("k1", "K1", MacroAction.DirectSend("a"), "desc", 0)
        val key2 = MacroKeyDefinition("k1", "K1", MacroAction.DirectSend("a"), "desc", 0)
        val key3 = MacroKeyDefinition("k2", "K1", MacroAction.DirectSend("a"), "desc", 0)

        assertEquals(key1, key2)
        assertNotEquals(key1, key3)
    }

    @Test
    fun macroKeyDefinition_copy_modifiesOnlySpecifiedFields() {
        val original = MacroKeyDefinition("k1", "K1", MacroAction.DirectSend("a"), "desc", 5)
        val copied = original.copy(label = "K2")

        assertEquals("K2", copied.label)
        assertEquals(original.id, copied.id)
        assertEquals(original.action, copied.action)
        assertEquals(original.description, copied.description)
        assertEquals(original.displayOrder, copied.displayOrder)
    }

    // ========== MacroTabDefinition Tests ==========

    @Test
    fun macroTabDefinition_defaultIcon_isNull() {
        val tab = MacroTabDefinition(
            id = "tab1",
            name = "Tab 1",
            keys = emptyList()
        )

        assertNull(tab.icon)
    }

    @Test
    fun macroTabDefinition_defaultKeys_isEmpty() {
        val tab = MacroTabDefinition(
            id = "tab1",
            name = "Tab 1"
        )

        assertTrue(tab.keys.isEmpty())
    }

    @Test
    fun macroTabDefinition_defaultDisplayOrder_isZero() {
        val tab = MacroTabDefinition(
            id = "tab1",
            name = "Tab 1"
        )

        assertEquals(0, tab.displayOrder)
    }

    @Test
    fun macroTabDefinition_equality() {
        val tab1 = MacroTabDefinition("t1", "Tab 1", "🔧", emptyList(), 0)
        val tab2 = MacroTabDefinition("t1", "Tab 1", "🔧", emptyList(), 0)
        val tab3 = MacroTabDefinition("t2", "Tab 1", "🔧", emptyList(), 0)

        assertEquals(tab1, tab2)
        assertNotEquals(tab1, tab3)
    }

    // ========== MacroPackMetadata Tests ==========

    @Test
    fun macroPackMetadata_defaultVersion_is1_0_0() {
        val metadata = MacroPackMetadata(name = "Test")

        assertEquals("1.0.0", metadata.version)
    }

    @Test
    fun macroPackMetadata_defaultTags_isEmpty() {
        val metadata = MacroPackMetadata(name = "Test")

        assertTrue(metadata.tags.isEmpty())
    }

    @Test
    fun macroPackMetadata_defaultOptionalFields_areNull() {
        val metadata = MacroPackMetadata(name = "Test")

        assertNull(metadata.description)
        assertNull(metadata.author)
        assertNull(metadata.license)
        assertNull(metadata.createdAt)
        assertNull(metadata.updatedAt)
    }

    @Test
    fun macroPackMetadata_equality() {
        val m1 = MacroPackMetadata("Pack", "Desc", "1.0.0", "Author", listOf("tag1"), "MIT")
        val m2 = MacroPackMetadata("Pack", "Desc", "1.0.0", "Author", listOf("tag1"), "MIT")
        val m3 = MacroPackMetadata("Pack2", "Desc", "1.0.0", "Author", listOf("tag1"), "MIT")

        assertEquals(m1, m2)
        assertNotEquals(m1, m3)
    }

    // ========== MacroPack Tests ==========

    @Test
    fun macroPack_defaultSource_isUser() {
        val pack = MacroPack(
            id = "pack1",
            metadata = MacroPackMetadata(name = "Test Pack")
        )

        assertEquals(MacroPackSource.USER, pack.source)
    }

    @Test
    fun macroPack_defaultIsActive_isTrue() {
        val pack = MacroPack(
            id = "pack1",
            metadata = MacroPackMetadata(name = "Test Pack")
        )

        assertTrue(pack.isActive)
    }

    @Test
    fun macroPack_defaultDisplayOrder_isZero() {
        val pack = MacroPack(
            id = "pack1",
            metadata = MacroPackMetadata(name = "Test Pack")
        )

        assertEquals(0, pack.displayOrder)
    }

    @Test
    fun macroPack_defaultMarketplaceId_isNull() {
        val pack = MacroPack(
            id = "pack1",
            metadata = MacroPackMetadata(name = "Test Pack")
        )

        assertNull(pack.marketplaceId)
    }

    @Test
    fun macroPack_defaultTabs_isEmpty() {
        val pack = MacroPack(
            id = "pack1",
            metadata = MacroPackMetadata(name = "Test Pack")
        )

        assertTrue(pack.tabs.isEmpty())
    }

    @Test
    fun macroPack_equality() {
        val pack1 = MacroPack("p1", MacroPackMetadata("Pack"), emptyList(), MacroPackSource.USER)
        val pack2 = MacroPack("p1", MacroPackMetadata("Pack"), emptyList(), MacroPackSource.USER)
        val pack3 = MacroPack("p2", MacroPackMetadata("Pack"), emptyList(), MacroPackSource.USER)

        assertEquals(pack1, pack2)
        assertNotEquals(pack1, pack3)
    }

    // ========== MacroPackExport Tests ==========

    @Test
    fun macroPackExport_defaultFormatVersion_is1_0() {
        val pack = MacroPack("p1", MacroPackMetadata("Test"))
        val export = MacroPackExport(pack = pack)

        assertEquals("1.0", export.formatVersion)
    }

    @Test
    fun macroPackExport_equality() {
        val pack = MacroPack("p1", MacroPackMetadata("Test"))
        val export1 = MacroPackExport("1.0", pack)
        val export2 = MacroPackExport("1.0", pack)
        val export3 = MacroPackExport("2.0", pack)

        assertEquals(export1, export2)
        assertNotEquals(export1, export3)
    }

    // ========== MacroPackSource Tests ==========

    @Test
    fun macroPackSource_hasThreeValues() {
        assertEquals(3, MacroPackSource.entries.size)
    }

    @Test
    fun macroPackSource_containsExpectedValues() {
        val sources = MacroPackSource.entries.map { it.name }

        assertTrue(sources.contains("BUILTIN"))
        assertTrue(sources.contains("USER"))
        assertTrue(sources.contains("MARKETPLACE"))
    }
}
