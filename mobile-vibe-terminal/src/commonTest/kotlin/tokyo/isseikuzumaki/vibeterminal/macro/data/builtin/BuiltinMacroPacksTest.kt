package tokyo.isseikuzumaki.vibeterminal.macro.data.builtin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroAction
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroPackSource

/**
 * Tests for BuiltinMacroPacks to ensure data integrity and correctness.
 */
class BuiltinMacroPacksTest {

    // ========== Default Pack Tests ==========

    @Test
    fun defaultPack_hasCorrectId() {
        assertEquals("builtin-default", BuiltinMacroPacks.defaultPack.id)
    }

    @Test
    fun defaultPack_hasCorrectSource() {
        assertEquals(MacroPackSource.BUILTIN, BuiltinMacroPacks.defaultPack.source)
    }

    @Test
    fun defaultPack_isActive() {
        assertTrue(BuiltinMacroPacks.defaultPack.isActive)
    }

    @Test
    fun defaultPack_hasCorrectMetadata() {
        val metadata = BuiltinMacroPacks.defaultPack.metadata
        assertEquals("Default", metadata.name)
        assertEquals("1.0.0", metadata.version)
        assertNotNull(metadata.description)
    }

    @Test
    fun defaultPack_hasFourTabs() {
        assertEquals(4, BuiltinMacroPacks.defaultPack.tabs.size)
    }

    // ========== Tab Structure Tests ==========

    @Test
    fun basicTab_hasCorrectStructure() {
        val basicTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-basic" }
        assertNotNull(basicTab)
        assertEquals("Basic", basicTab.name)
        assertEquals(0, basicTab.displayOrder)
    }

    @Test
    fun navTab_hasCorrectStructure() {
        val navTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-nav" }
        assertNotNull(navTab)
        assertEquals("Nav", navTab.name)
        assertEquals(1, navTab.displayOrder)
    }

    @Test
    fun vimTab_hasCorrectStructure() {
        val vimTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-vim" }
        assertNotNull(vimTab)
        assertEquals("Vim", vimTab.name)
        assertEquals(2, vimTab.displayOrder)
    }

    @Test
    fun functionTab_hasCorrectStructure() {
        val functionTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-function" }
        assertNotNull(functionTab)
        assertEquals("Function", functionTab.name)
        assertEquals(3, functionTab.displayOrder)
    }

    // ========== Key Count Tests ==========

    @Test
    fun basicTab_hasCorrectKeyCount() {
        val basicTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-basic" }
        assertNotNull(basicTab)
        assertEquals(11, basicTab.keys.size)
    }

    @Test
    fun navTab_hasCorrectKeyCount() {
        val navTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-nav" }
        assertNotNull(navTab)
        assertEquals(9, navTab.keys.size)
    }

    @Test
    fun vimTab_hasCorrectKeyCount() {
        val vimTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-vim" }
        assertNotNull(vimTab)
        assertEquals(17, vimTab.keys.size)
    }

    @Test
    fun functionTab_hasCorrectKeyCount() {
        val functionTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-function" }
        assertNotNull(functionTab)
        assertEquals(12, functionTab.keys.size)
    }

    // ========== Escape Sequence Tests ==========

    @Test
    fun basicTab_escKey_hasCorrectSequence() {
        val basicTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-basic" }!!
        val escKey = basicTab.keys.find { it.label == "ESC" }
        assertNotNull(escKey)
        val action = escKey.action as MacroAction.DirectSend
        assertEquals("\u001B", action.sequence)
    }

    @Test
    fun basicTab_ctrlC_hasCorrectSequence() {
        val basicTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-basic" }!!
        val ctrlC = basicTab.keys.find { it.label == "CTRL+C" }
        assertNotNull(ctrlC)
        val action = ctrlC.action as MacroAction.DirectSend
        assertEquals("\u0003", action.sequence)
    }

    @Test
    fun navTab_arrowUp_hasCorrectSequence() {
        val navTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-nav" }!!
        val upKey = navTab.keys.find { it.label == "↑" }
        assertNotNull(upKey)
        val action = upKey.action as MacroAction.DirectSend
        assertEquals("\u001B[A", action.sequence)
    }

    @Test
    fun navTab_arrowDown_hasCorrectSequence() {
        val navTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-nav" }!!
        val downKey = navTab.keys.find { it.label == "↓" }
        assertNotNull(downKey)
        val action = downKey.action as MacroAction.DirectSend
        assertEquals("\u001B[B", action.sequence)
    }

    @Test
    fun functionTab_f1_hasCorrectSequence() {
        val functionTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-function" }!!
        val f1Key = functionTab.keys.find { it.label == "F1" }
        assertNotNull(f1Key)
        val action = f1Key.action as MacroAction.DirectSend
        assertEquals("\u001BOP", action.sequence)
    }

    @Test
    fun functionTab_f5_hasCorrectSequence() {
        val functionTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-function" }!!
        val f5Key = functionTab.keys.find { it.label == "F5" }
        assertNotNull(f5Key)
        val action = f5Key.action as MacroAction.DirectSend
        assertEquals("\u001B[15~", action.sequence)
    }

    @Test
    fun functionTab_f12_hasCorrectSequence() {
        val functionTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-function" }!!
        val f12Key = functionTab.keys.find { it.label == "F12" }
        assertNotNull(f12Key)
        val action = f12Key.action as MacroAction.DirectSend
        assertEquals("\u001B[24~", action.sequence)
    }

    // ========== Buffer Insert Tests ==========

    @Test
    fun basicTab_pipeKey_isBufferInsert() {
        val basicTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-basic" }!!
        val pipeKey = basicTab.keys.find { it.label == "|" }
        assertNotNull(pipeKey)
        val action = pipeKey.action as MacroAction.BufferInsert
        assertEquals("|", action.text)
    }

    @Test
    fun vimTab_saveCommand_isBufferInsert() {
        val vimTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-vim" }!!
        val saveKey = vimTab.keys.find { it.label == ":w" }
        assertNotNull(saveKey)
        val action = saveKey.action as MacroAction.BufferInsert
        assertEquals(":w", action.text)
    }

    // ========== All Packs Tests ==========

    @Test
    fun allPacks_containsDefaultPack() {
        assertTrue(BuiltinMacroPacks.allPacks.contains(BuiltinMacroPacks.defaultPack))
    }

    @Test
    fun allPacks_hasCorrectSize() {
        assertEquals(1, BuiltinMacroPacks.allPacks.size)
    }

    // ========== Unique ID Tests ==========

    @Test
    fun allTabs_haveUniqueIds() {
        val allTabIds = BuiltinMacroPacks.defaultPack.tabs.map { it.id }
        assertEquals(allTabIds.size, allTabIds.toSet().size)
    }

    @Test
    fun allKeys_haveUniqueIds() {
        val allKeyIds = BuiltinMacroPacks.defaultPack.tabs.flatMap { tab ->
            tab.keys.map { it.id }
        }
        assertEquals(allKeyIds.size, allKeyIds.toSet().size)
    }

    // ========== Display Order Tests ==========

    @Test
    fun tabs_haveSequentialDisplayOrder() {
        val tabs = BuiltinMacroPacks.defaultPack.tabs.sortedBy { it.displayOrder }
        tabs.forEachIndexed { index, tab ->
            assertEquals(index, tab.displayOrder)
        }
    }

    @Test
    fun basicTab_keys_haveSequentialDisplayOrder() {
        val basicTab = BuiltinMacroPacks.defaultPack.tabs.find { it.id == "builtin-basic" }!!
        val keys = basicTab.keys.sortedBy { it.displayOrder }
        keys.forEachIndexed { index, key ->
            assertEquals(index, key.displayOrder)
        }
    }
}
