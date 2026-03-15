package tokyo.isseikuzumaki.vibeterminal.macro.domain.repository

import kotlinx.coroutines.flow.Flow
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroPack
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroTabDefinition

/**
 * Repository interface for managing macro packs.
 */
interface MacroRepository {
    // Pack operations

    /**
     * Get all macro packs (active and inactive).
     */
    fun getAllPacks(): Flow<List<MacroPack>>

    /**
     * Get only active macro packs.
     */
    fun getActivePacks(): Flow<List<MacroPack>>

    /**
     * Get a specific pack by ID.
     */
    fun getPackById(id: String): Flow<MacroPack?>

    /**
     * Save a macro pack (insert or update).
     */
    suspend fun savePack(pack: MacroPack)

    /**
     * Delete a macro pack by ID.
     */
    suspend fun deletePack(id: String)

    /**
     * Set whether a pack is active.
     */
    suspend fun setPackActive(id: String, isActive: Boolean)

    // Tab ordering operations

    /**
     * Get all tabs from active packs, flattened and ordered.
     */
    fun getAllActiveTabs(): Flow<List<MacroTabDefinition>>

    /**
     * Update the display order of tabs within a pack.
     */
    suspend fun updateTabOrder(packId: String, tabIds: List<String>)

    /**
     * Update the global display order of packs.
     */
    suspend fun updatePackOrder(packIds: List<String>)

    // Key ordering operations

    /**
     * Update the display order of keys within a tab.
     */
    suspend fun updateKeyOrder(tabId: String, keyIds: List<String>)

    // Import/Export operations

    /**
     * Import a macro pack from JSON string.
     * @return Result containing the imported pack or an error.
     */
    suspend fun importPack(json: String): Result<MacroPack>

    /**
     * Export a macro pack to JSON string.
     * @return Result containing the JSON string or an error.
     */
    suspend fun exportPack(packId: String): Result<String>
}
