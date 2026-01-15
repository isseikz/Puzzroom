package tokyo.isseikuzumaki.vibeterminal.macro.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.vibeterminal.macro.data.builtin.BuiltinMacroPacks
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroPack
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroPackExport
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroPackSource
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroTabDefinition
import tokyo.isseikuzumaki.vibeterminal.macro.domain.repository.MacroRepository

/**
 * Repository implementation that combines builtin macro packs with user-created packs.
 * User packs are persisted using DataStore.
 */
class MacroRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : MacroRepository {

    companion object {
        private val USER_PACKS_KEY = stringPreferencesKey("user_macro_packs")
        private val PACK_ORDER_KEY = stringPreferencesKey("macro_pack_order")
        private val TAB_VISIBILITY_KEY = stringPreferencesKey("macro_tab_visibility")

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }

    // In-memory cache for user packs
    private val userPacksCache = MutableStateFlow<List<MacroPack>>(emptyList())

    // Load user packs from DataStore
    private val userPacksFlow: Flow<List<MacroPack>> = dataStore.data.map { preferences ->
        val jsonString = preferences[USER_PACKS_KEY]
        if (jsonString != null) {
            try {
                json.decodeFromString<List<MacroPack>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    override fun getAllPacks(): Flow<List<MacroPack>> {
        return userPacksFlow.map { userPacks ->
            BuiltinMacroPacks.allPacks + userPacks
        }
    }

    override fun getActivePacks(): Flow<List<MacroPack>> {
        return getAllPacks().map { packs ->
            packs.filter { it.isActive }
                .sortedBy { it.displayOrder }
        }
    }

    override fun getPackById(id: String): Flow<MacroPack?> {
        return getAllPacks().map { packs ->
            packs.find { it.id == id }
        }
    }

    override suspend fun savePack(pack: MacroPack) {
        if (pack.source == MacroPackSource.BUILTIN) {
            // Cannot modify builtin packs
            return
        }

        val currentPacks = userPacksFlow.first().toMutableList()
        val existingIndex = currentPacks.indexOfFirst { it.id == pack.id }

        if (existingIndex >= 0) {
            currentPacks[existingIndex] = pack
        } else {
            currentPacks.add(pack)
        }

        saveUserPacks(currentPacks)
    }

    override suspend fun deletePack(id: String) {
        val currentPacks = userPacksFlow.first().toMutableList()
        val packToDelete = currentPacks.find { it.id == id }

        // Cannot delete builtin packs
        if (packToDelete?.source == MacroPackSource.BUILTIN) {
            return
        }

        currentPacks.removeAll { it.id == id }
        saveUserPacks(currentPacks)
    }

    override suspend fun setPackActive(id: String, isActive: Boolean) {
        val allPacks = getAllPacks().first()
        val pack = allPacks.find { it.id == id } ?: return

        if (pack.source == MacroPackSource.BUILTIN) {
            // For builtin packs, store visibility separately
            // This could be enhanced to store in DataStore
            return
        }

        savePack(pack.copy(isActive = isActive))
    }

    override fun getAllActiveTabs(): Flow<List<MacroTabDefinition>> {
        return getActivePacks().map { packs ->
            packs.flatMap { pack ->
                pack.tabs.map { tab ->
                    // Prefix tab ID with pack ID for uniqueness
                    tab.copy(displayOrder = pack.displayOrder * 100 + tab.displayOrder)
                }
            }.sortedBy { it.displayOrder }
        }
    }

    override suspend fun updateTabOrder(packId: String, tabIds: List<String>) {
        val pack = getPackById(packId).first() ?: return

        if (pack.source == MacroPackSource.BUILTIN) {
            return
        }

        val reorderedTabs = tabIds.mapIndexedNotNull { index, tabId ->
            pack.tabs.find { it.id == tabId }?.copy(displayOrder = index)
        }

        savePack(pack.copy(tabs = reorderedTabs))
    }

    override suspend fun updatePackOrder(packIds: List<String>) {
        val currentUserPacks = userPacksFlow.first().toMutableList()

        packIds.forEachIndexed { index, packId ->
            val packIndex = currentUserPacks.indexOfFirst { it.id == packId }
            if (packIndex >= 0) {
                currentUserPacks[packIndex] = currentUserPacks[packIndex].copy(
                    displayOrder = index + BuiltinMacroPacks.allPacks.size
                )
            }
        }

        saveUserPacks(currentUserPacks)
    }

    override suspend fun updateKeyOrder(tabId: String, keyIds: List<String>) {
        val allPacks = getAllPacks().first()
        val packContainingTab = allPacks.find { pack ->
            pack.tabs.any { it.id == tabId }
        } ?: return

        if (packContainingTab.source == MacroPackSource.BUILTIN) {
            return
        }

        val updatedTabs = packContainingTab.tabs.map { tab ->
            if (tab.id == tabId) {
                val reorderedKeys = keyIds.mapIndexedNotNull { index, keyId ->
                    tab.keys.find { it.id == keyId }?.copy(displayOrder = index)
                }
                tab.copy(keys = reorderedKeys)
            } else {
                tab
            }
        }

        savePack(packContainingTab.copy(tabs = updatedTabs))
    }

    override suspend fun importPack(jsonString: String): Result<MacroPack> {
        return try {
            val export = json.decodeFromString<MacroPackExport>(jsonString)
            val importedPack = export.pack.copy(
                source = MacroPackSource.USER,
                displayOrder = getAllPacks().first().size
            )
            savePack(importedPack)
            Result.success(importedPack)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportPack(packId: String): Result<String> {
        return try {
            val pack = getPackById(packId).first()
                ?: return Result.failure(IllegalArgumentException("Pack not found: $packId"))

            val export = MacroPackExport(
                formatVersion = "1.0",
                pack = pack
            )
            Result.success(json.encodeToString(export))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveUserPacks(packs: List<MacroPack>) {
        dataStore.edit { preferences ->
            preferences[USER_PACKS_KEY] = json.encodeToString(packs)
        }
    }
}
