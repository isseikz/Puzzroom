package tokyo.isseikuzumaki.vibeterminal.macro.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Metadata for a macro pack, used for display and marketplace information.
 *
 * @property name Display name of the pack
 * @property description Description of what the pack contains
 * @property version Semantic version string
 * @property author Author name or identifier
 * @property tags Tags for categorization and search
 * @property license License type (e.g., "MIT", "CC-BY")
 * @property createdAt ISO 8601 timestamp of creation
 * @property updatedAt ISO 8601 timestamp of last update
 */
@Serializable
data class MacroPackMetadata(
    val name: String,
    val description: String? = null,
    val version: String = "1.0.0",
    val author: String? = null,
    val tags: List<String> = emptyList(),
    val license: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
