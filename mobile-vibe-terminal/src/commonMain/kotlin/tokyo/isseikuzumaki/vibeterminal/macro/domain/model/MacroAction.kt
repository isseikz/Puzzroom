package tokyo.isseikuzumaki.vibeterminal.macro.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an action that a macro key can perform.
 */
@Serializable
sealed class MacroAction {
    /**
     * Sends a sequence directly to the terminal (e.g., escape sequences, control characters).
     */
    @Serializable
    @SerialName("direct_send")
    data class DirectSend(val sequence: String) : MacroAction()

    /**
     * Inserts text into the input buffer for user editing before sending.
     * @param text The text to insert
     * @param cursorOffset Offset from end of text for cursor position (0 = end, negative = left)
     */
    @Serializable
    @SerialName("buffer_insert")
    data class BufferInsert(
        val text: String,
        @SerialName("cursor_offset")
        val cursorOffset: Int = 0
    ) : MacroAction()

    /**
     * Sends a special key (ESC, ENTER, TAB, F1-F12, arrows, etc.).
     */
    @Serializable
    @SerialName("special_key")
    data class SpecialKey(val key: SpecialKeyType) : MacroAction()

    /**
     * Sends a key with modifier(s) (Ctrl, Alt, Shift, Meta).
     */
    @Serializable
    @SerialName("modifier_key")
    data class ModifierKey(
        val key: String,
        val ctrl: Boolean = false,
        val alt: Boolean = false,
        val shift: Boolean = false,
        val meta: Boolean = false
    ) : MacroAction()

    /**
     * Executes multiple actions in sequence.
     */
    @Serializable
    @SerialName("sequence")
    data class Sequence(
        val actions: List<MacroAction>,
        @SerialName("delay_ms")
        val delayMs: Int = 50
    ) : MacroAction()

    /**
     * Waits for a specified duration.
     */
    @Serializable
    @SerialName("delay")
    data class Delay(
        @SerialName("duration_ms")
        val durationMs: Int
    ) : MacroAction()
}

/**
 * Special key types supported by the terminal.
 */
@Serializable
enum class SpecialKeyType(val escapeSequence: String) {
    @SerialName("esc")
    ESC("\u001B"),

    @SerialName("enter")
    ENTER("\r"),

    @SerialName("tab")
    TAB("\t"),

    @SerialName("space")
    SPACE(" "),

    @SerialName("backspace")
    BACKSPACE("\u007F"),

    @SerialName("delete")
    DELETE("\u001B[3~"),

    @SerialName("up")
    UP("\u001B[A"),

    @SerialName("down")
    DOWN("\u001B[B"),

    @SerialName("right")
    RIGHT("\u001B[C"),

    @SerialName("left")
    LEFT("\u001B[D"),

    @SerialName("home")
    HOME("\u001B[H"),

    @SerialName("end")
    END("\u001B[F"),

    @SerialName("page_up")
    PAGE_UP("\u001B[5~"),

    @SerialName("page_down")
    PAGE_DOWN("\u001B[6~"),

    @SerialName("insert")
    INSERT("\u001B[2~"),

    @SerialName("f1")
    F1("\u001BOP"),

    @SerialName("f2")
    F2("\u001BOQ"),

    @SerialName("f3")
    F3("\u001BOR"),

    @SerialName("f4")
    F4("\u001BOS"),

    @SerialName("f5")
    F5("\u001B[15~"),

    @SerialName("f6")
    F6("\u001B[17~"),

    @SerialName("f7")
    F7("\u001B[18~"),

    @SerialName("f8")
    F8("\u001B[19~"),

    @SerialName("f9")
    F9("\u001B[20~"),

    @SerialName("f10")
    F10("\u001B[21~"),

    @SerialName("f11")
    F11("\u001B[23~"),

    @SerialName("f12")
    F12("\u001B[24~");

    companion object {
        /**
         * Parse a key name string to SpecialKeyType.
         */
        fun fromString(name: String): SpecialKeyType? {
            val normalized = name.uppercase().replace("_", "").replace("-", "")
            return when (normalized) {
                "ESC", "ESCAPE" -> ESC
                "ENTER", "RETURN", "CR" -> ENTER
                "TAB" -> TAB
                "SPACE", "SP" -> SPACE
                "BACKSPACE", "BS" -> BACKSPACE
                "DELETE", "DEL" -> DELETE
                "UP", "ARROWUP" -> UP
                "DOWN", "ARROWDOWN" -> DOWN
                "RIGHT", "ARROWRIGHT" -> RIGHT
                "LEFT", "ARROWLEFT" -> LEFT
                "HOME" -> HOME
                "END" -> END
                "PAGEUP", "PGUP" -> PAGE_UP
                "PAGEDOWN", "PGDN" -> PAGE_DOWN
                "INSERT", "INS" -> INSERT
                "F1" -> F1
                "F2" -> F2
                "F3" -> F3
                "F4" -> F4
                "F5" -> F5
                "F6" -> F6
                "F7" -> F7
                "F8" -> F8
                "F9" -> F9
                "F10" -> F10
                "F11" -> F11
                "F12" -> F12
                else -> null
            }
        }
    }
}

/**
 * Extension to convert ModifierKey to escape sequence.
 */
fun MacroAction.ModifierKey.toEscapeSequence(): String {
    val keyChar = key.lowercase().first()
    val code = keyChar.code

    return when {
        ctrl && !alt && !shift && !meta -> {
            // Ctrl+key: send control character (key - 96 for lowercase, key - 64 for uppercase)
            val ctrlCode = if (code in 97..122) code - 96 else code - 64
            ctrlCode.toChar().toString()
        }
        alt && !ctrl && !shift && !meta -> {
            // Alt+key: send ESC followed by key
            "\u001B$keyChar"
        }
        ctrl && alt -> {
            // Ctrl+Alt combinations (less common, terminal-specific)
            "\u001B${(code - 96).toChar()}"
        }
        else -> {
            // For other combinations, may need CSI sequences
            // This is a simplified implementation
            key
        }
    }
}
