package tokyo.isseikuzumaki.vibeterminal.macro.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A collection of macro tabs that can be installed, shared, or sold.
 *
 * @property id Unique identifier for the pack
 * @property metadata Pack metadata (name, description, version, etc.)
 * @property tabs List of macro tabs in this pack
 * @property source Origin of the pack (builtin, user, marketplace)
 * @property marketplaceId Reference ID if downloaded from marketplace
 * @property isActive Whether this pack is currently active/visible
 * @property displayOrder Global display order for sorting packs
 */
@Serializable
data class MacroPack(
    val id: String,
    val metadata: MacroPackMetadata,
    val tabs: List<MacroTabDefinition> = emptyList(),
    val source: MacroPackSource = MacroPackSource.USER,
    @SerialName("marketplace_id")
    val marketplaceId: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("display_order")
    val displayOrder: Int = 0
)

/**
 * Wrapper for serializing macro packs with format version.
 */
@Serializable
data class MacroPackExport(
    @SerialName("format_version")
    val formatVersion: String = "1.0",
    val pack: MacroPack
)
