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
     */
    @Serializable
    @SerialName("buffer_insert")
    data class BufferInsert(val text: String) : MacroAction()
}
