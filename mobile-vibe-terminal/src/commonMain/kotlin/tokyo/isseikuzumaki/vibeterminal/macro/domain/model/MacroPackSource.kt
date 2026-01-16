package tokyo.isseikuzumaki.vibeterminal.macro.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Indicates the origin of a macro pack.
 */
@Serializable
enum class MacroPackSource {
    /**
     * Built-in macro packs that ship with the app.
     */
    @SerialName("builtin")
    BUILTIN,

    /**
     * User-created macro packs stored locally.
     */
    @SerialName("user")
    USER,

    /**
     * Macro packs downloaded from the marketplace.
     */
    @SerialName("marketplace")
    MARKETPLACE
}
