package tokyo.isseikuzumaki.vibeterminal.macro.domain.model

import kotlinx.serialization.Serializable

/**
 * Defines a single macro key within a tab.
 *
 * @property id Unique identifier for the key
 * @property label Display label on the button
 * @property action Action to perform when pressed
 * @property description Optional description/tooltip
 * @property displayOrder Position within the tab
 */
@Serializable
data class MacroKeyDefinition(
    val id: String,
    val label: String,
    val action: MacroAction,
    val description: String? = null,
    val displayOrder: Int = 0
)
