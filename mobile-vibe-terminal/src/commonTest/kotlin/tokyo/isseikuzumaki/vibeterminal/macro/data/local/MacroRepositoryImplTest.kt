package tokyo.isseikuzumaki.vibeterminal.macro.data.local

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import tokyo.isseikuzumaki.vibeterminal.macro.data.builtin.BuiltinMacroPacks
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroAction
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroKeyDefinition
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroPack
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroPackMetadata
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroPackSource
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroTabDefinition

/**
 * Tests for MacroRepositoryImpl.
 */
class MacroRepositoryImplTest {

    private lateinit var fakeDataStore: FakeDataStore
    private lateinit var repository: MacroRepositoryImpl

    @BeforeTest
    fun setup() {
        fakeDataStore = FakeDataStore()
        repository = MacroRepositoryImpl(fakeDataStore)
    }

    // ========== getAllPacks Tests ==========

    @Test
    fun getAllPacks_returnsBuiltinPacks_whenNoUserPacks() = runTest {
        val packs = repository.getAllPacks().first()

        assertTrue(packs.isNotEmpty())
        assertTrue(packs.any { it.source == MacroPackSource.BUILTIN })
    }

    @Test
    fun getAllPacks_includesUserPacks_afterSaving() = runTest {
        val userPack = createTestUserPack("user-1")
        repository.savePack(userPack)

        val packs = repository.getAllPacks().first()

        assertTrue(packs.any { it.id == "user-1" })
        assertTrue(packs.any { it.source == MacroPackSource.BUILTIN })
    }

    // ========== getActivePacks Tests ==========

    @Test
    fun getActivePacks_returnsOnlyActivePacks() = runTest {
        val activePack = createTestUserPack("active-1", isActive = true)
        val inactivePack = createTestUserPack("inactive-1", isActive = false)

        repository.savePack(activePack)
        repository.savePack(inactivePack)

        val activePacks = repository.getActivePacks().first()

        assertTrue(activePacks.any { it.id == "active-1" })
        assertFalse(activePacks.any { it.id == "inactive-1" })
    }

    @Test
    fun getActivePacks_sortedByDisplayOrder() = runTest {
        val pack1 = createTestUserPack("pack-1", displayOrder = 10)
        val pack2 = createTestUserPack("pack-2", displayOrder = 5)

        repository.savePack(pack1)
        repository.savePack(pack2)

        val activePacks = repository.getActivePacks().first()
        val userPacks = activePacks.filter { it.source == MacroPackSource.USER }

        // pack-2 should come before pack-1 due to lower displayOrder
        val pack1Index = userPacks.indexOfFirst { it.id == "pack-1" }
        val pack2Index = userPacks.indexOfFirst { it.id == "pack-2" }
        assertTrue(pack2Index < pack1Index)
    }

    // ========== getPackById Tests ==========

    @Test
    fun getPackById_returnsBuiltinPack() = runTest {
        val pack = repository.getPackById("builtin-default").first()

        assertNotNull(pack)
        assertEquals(MacroPackSource.BUILTIN, pack.source)
    }

    @Test
    fun getPackById_returnsUserPack() = runTest {
        val userPack = createTestUserPack("user-pack-1")
        repository.savePack(userPack)

        val pack = repository.getPackById("user-pack-1").first()

        assertNotNull(pack)
        assertEquals("user-pack-1", pack.id)
    }

    @Test
    fun getPackById_returnsNull_forNonExistentPack() = runTest {
        val pack = repository.getPackById("non-existent").first()

        assertNull(pack)
    }

    // ========== savePack Tests ==========

    @Test
    fun savePack_createsNewPack() = runTest {
        val newPack = createTestUserPack("new-pack")

        repository.savePack(newPack)

        val savedPack = repository.getPackById("new-pack").first()
        assertNotNull(savedPack)
        assertEquals("new-pack", savedPack.id)
    }

    @Test
    fun savePack_updatesExistingPack() = runTest {
        val pack = createTestUserPack("update-pack", name = "Original")
        repository.savePack(pack)

        val updatedPack = pack.copy(
            metadata = pack.metadata.copy(name = "Updated")
        )
        repository.savePack(updatedPack)

        val savedPack = repository.getPackById("update-pack").first()
        assertNotNull(savedPack)
        assertEquals("Updated", savedPack.metadata.name)
    }

    @Test
    fun savePack_doesNotSaveBuiltinPack() = runTest {
        val builtinPack = MacroPack(
            id = "fake-builtin",
            metadata = MacroPackMetadata(name = "Fake Builtin"),
            source = MacroPackSource.BUILTIN
        )

        repository.savePack(builtinPack)

        // Should not appear in user packs
        val allPacks = repository.getAllPacks().first()
        assertFalse(allPacks.any { it.id == "fake-builtin" })
    }

    // ========== deletePack Tests ==========

    @Test
    fun deletePack_removesUserPack() = runTest {
        val pack = createTestUserPack("delete-me")
        repository.savePack(pack)

        repository.deletePack("delete-me")

        val deletedPack = repository.getPackById("delete-me").first()
        assertNull(deletedPack)
    }

    @Test
    fun deletePack_doesNotRemoveBuiltinPack() = runTest {
        repository.deletePack("builtin-default")

        val builtinPack = repository.getPackById("builtin-default").first()
        assertNotNull(builtinPack)
    }

    // ========== setPackActive Tests ==========

    @Test
    fun setPackActive_updatesUserPackStatus() = runTest {
        val pack = createTestUserPack("toggle-pack", isActive = true)
        repository.savePack(pack)

        repository.setPackActive("toggle-pack", false)

        val updatedPack = repository.getPackById("toggle-pack").first()
        assertNotNull(updatedPack)
        assertFalse(updatedPack.isActive)
    }

    // ========== getAllActiveTabs Tests ==========

    @Test
    fun getAllActiveTabs_returnsFlattenedTabs() = runTest {
        val tabs = repository.getAllActiveTabs().first()

        // Should include builtin tabs
        assertTrue(tabs.isNotEmpty())
    }

    @Test
    fun getAllActiveTabs_sortedByDisplayOrder() = runTest {
        val tabs = repository.getAllActiveTabs().first()

        // Verify tabs are sorted
        val displayOrders = tabs.map { it.displayOrder }
        assertEquals(displayOrders.sorted(), displayOrders)
    }

    // ========== updateTabOrder Tests ==========

    @Test
    fun updateTabOrder_reordersTabsInUserPack() = runTest {
        val pack = createTestUserPackWithTabs("reorder-pack")
        repository.savePack(pack)

        val originalTabs = pack.tabs.map { it.id }
        val reversedTabs = originalTabs.reversed()

        repository.updateTabOrder("reorder-pack", reversedTabs)

        val updatedPack = repository.getPackById("reorder-pack").first()
        assertNotNull(updatedPack)

        val updatedOrder = updatedPack.tabs.sortedBy { it.displayOrder }.map { it.id }
        assertEquals(reversedTabs, updatedOrder)
    }

    // ========== updateKeyOrder Tests ==========

    @Test
    fun updateKeyOrder_reordersKeysInTab() = runTest {
        val pack = createTestUserPackWithTabs("key-reorder-pack")
        repository.savePack(pack)

        val tab = pack.tabs.first()
        val originalKeys = tab.keys.map { it.id }
        val reversedKeys = originalKeys.reversed()

        repository.updateKeyOrder(tab.id, reversedKeys)

        val updatedPack = repository.getPackById("key-reorder-pack").first()
        assertNotNull(updatedPack)

        val updatedTab = updatedPack.tabs.find { it.id == tab.id }
        assertNotNull(updatedTab)

        val updatedOrder = updatedTab.keys.sortedBy { it.displayOrder }.map { it.id }
        assertEquals(reversedKeys, updatedOrder)
    }

    // ========== importPack Tests ==========

    @Test
    fun importPack_successfullyImportsValidJson() = runTest {
        val jsonString = """
            {
                "format_version": "1.0",
                "pack": {
                    "id": "imported-pack",
                    "metadata": {
                        "name": "Imported Pack",
                        "version": "1.0.0"
                    },
                    "tabs": [],
                    "source": "user",
                    "is_active": true,
                    "display_order": 0
                }
            }
        """.trimIndent()

        val result = repository.importPack(jsonString)

        assertTrue(result.isSuccess)
        val importedPack = result.getOrNull()
        assertNotNull(importedPack)
        assertEquals("imported-pack", importedPack.id)
        assertEquals(MacroPackSource.USER, importedPack.source)
    }

    @Test
    fun importPack_failsOnInvalidJson() = runTest {
        val invalidJson = "{ invalid json }"

        val result = repository.importPack(invalidJson)

        assertTrue(result.isFailure)
    }

    @Test
    fun importPack_convertsToUserSource() = runTest {
        val jsonString = """
            {
                "format_version": "1.0",
                "pack": {
                    "id": "marketplace-import",
                    "metadata": { "name": "From Marketplace" },
                    "tabs": [],
                    "source": "marketplace",
                    "is_active": true,
                    "display_order": 0
                }
            }
        """.trimIndent()

        val result = repository.importPack(jsonString)

        assertTrue(result.isSuccess)
        val importedPack = result.getOrNull()
        assertNotNull(importedPack)
        assertEquals(MacroPackSource.USER, importedPack.source)
    }

    // ========== exportPack Tests ==========

    @Test
    fun exportPack_successfullyExportsUserPack() = runTest {
        val pack = createTestUserPack("export-pack")
        repository.savePack(pack)

        val result = repository.exportPack("export-pack")

        assertTrue(result.isSuccess)
        val jsonString = result.getOrNull()
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("export-pack"))
        assertTrue(jsonString.contains("format_version"))
    }

    @Test
    fun exportPack_failsForNonExistentPack() = runTest {
        val result = repository.exportPack("non-existent-pack")

        assertTrue(result.isFailure)
    }

    @Test
    fun exportPack_canExportBuiltinPack() = runTest {
        val result = repository.exportPack("builtin-default")

        assertTrue(result.isSuccess)
        val jsonString = result.getOrNull()
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("builtin-default"))
    }

    // ========== Import/Export Round Trip Tests ==========

    @Test
    fun importExport_roundTrip_preservesData() = runTest {
        val original = createTestUserPackWithTabs("roundtrip-pack")
        repository.savePack(original)

        val exportResult = repository.exportPack("roundtrip-pack")
        assertTrue(exportResult.isSuccess)

        // Clear and reimport
        fakeDataStore.clear()
        repository = MacroRepositoryImpl(fakeDataStore)

        val importResult = repository.importPack(exportResult.getOrThrow())
        assertTrue(importResult.isSuccess)

        val reimported = repository.getPackById("roundtrip-pack").first()
        assertNotNull(reimported)
        assertEquals(original.metadata.name, reimported.metadata.name)
        assertEquals(original.tabs.size, reimported.tabs.size)
    }

    // ========== Helper Functions ==========

    private fun createTestUserPack(
        id: String,
        name: String = "Test Pack",
        isActive: Boolean = true,
        displayOrder: Int = 0
    ): MacroPack {
        return MacroPack(
            id = id,
            metadata = MacroPackMetadata(name = name),
            tabs = emptyList(),
            source = MacroPackSource.USER,
            isActive = isActive,
            displayOrder = displayOrder
        )
    }

    private fun createTestUserPackWithTabs(id: String): MacroPack {
        return MacroPack(
            id = id,
            metadata = MacroPackMetadata(name = "Pack with Tabs"),
            tabs = listOf(
                MacroTabDefinition(
                    id = "$id-tab-1",
                    name = "Tab 1",
                    keys = listOf(
                        MacroKeyDefinition("$id-key-1", "K1", MacroAction.DirectSend("a"), displayOrder = 0),
                        MacroKeyDefinition("$id-key-2", "K2", MacroAction.DirectSend("b"), displayOrder = 1),
                        MacroKeyDefinition("$id-key-3", "K3", MacroAction.DirectSend("c"), displayOrder = 2)
                    ),
                    displayOrder = 0
                ),
                MacroTabDefinition(
                    id = "$id-tab-2",
                    name = "Tab 2",
                    keys = listOf(
                        MacroKeyDefinition("$id-key-4", "K4", MacroAction.BufferInsert("x"), displayOrder = 0)
                    ),
                    displayOrder = 1
                )
            ),
            source = MacroPackSource.USER,
            isActive = true,
            displayOrder = 0
        )
    }
}
