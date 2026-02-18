package tokyo.isseikuzumaki.vibeterminal.ui.components

/**
 * Represents a single key in the fixed auxiliary key row.
 *
 * @property label Display text shown on the key button
 * @property keyCode The character(s) sent to the terminal when the key is tapped
 * @property position Order position in the row (0-7, left to right)
 * @property hasTrailingSeparator Whether to show a visual separator after this key
 */
data class FixedKey(
    val label: String,
    val keyCode: String,
    val position: Int,
    val hasTrailingSeparator: Boolean = false
) {
    companion object {
        /**
         * The complete list of 8 fixed auxiliary keys in display order.
         *
         * Layout: ESC | TAB | CTRL+C | | | / | ↑ | ↓ | Enter
         *
         * Key codes match MacroDefinition patterns for terminal compatibility.
         */
        val ALL: List<FixedKey> = listOf(
            FixedKey(label = "ESC", keyCode = "\u001B", position = 0),
            FixedKey(label = "TAB", keyCode = "\t", position = 1),
            FixedKey(label = "CTRL+C", keyCode = "\u0003", position = 2),
            FixedKey(label = "|", keyCode = "|", position = 3),
            FixedKey(label = "/", keyCode = "/", position = 4),
            FixedKey(label = "↑", keyCode = "\u001B[A", position = 5),
            FixedKey(label = "↓", keyCode = "\u001B[B", position = 6, hasTrailingSeparator = true),
            FixedKey(label = "Enter", keyCode = "\r", position = 7)
        )
    }
}
