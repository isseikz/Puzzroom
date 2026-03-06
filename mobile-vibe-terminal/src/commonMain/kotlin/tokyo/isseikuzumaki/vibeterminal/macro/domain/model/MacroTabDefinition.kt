package tokyo.isseikuzumaki.vibeterminal.macro.domain.model

import kotlinx.serialization.Serializable

/**
 * Defines a tab containing multiple macro keys.
 *
 * @property id Unique identifier for the tab
 * @property name Display name of the tab
 * @property icon Optional icon (emoji or icon reference)
 * @property keys List of macro keys in this tab
 * @property displayOrder Position in the tab row
 */
@Serializable
data class MacroTabDefinition(
    val id: String,
    val name: String,
    val icon: String? = null,
    val keys: List<MacroKeyDefinition> = emptyList(),
    val displayOrder: Int = 0
)
