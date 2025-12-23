package tokyo.isseikuzumaki.vibeterminal.ui.components.macro

sealed class MacroAction {
    data class DirectSend(val sequence: String) : MacroAction()
    data class BufferInsert(val text: String) : MacroAction()
}

data class MacroKey(
    val label: String,
    val action: MacroAction,
    val description: String? = null
)

enum class MacroTab {
    BASIC, NAV, VIM, FUNCTION
}

object MacroConfig {
    val basicKeys = listOf(
        MacroKey("ESC", MacroAction.DirectSend("\u001B"), "Escape"),
        MacroKey("TAB", MacroAction.DirectSend("\t"), "Tab"),
        MacroKey("CTRL+C", MacroAction.DirectSend("\u0003"), "Interrupt"),
        MacroKey("CTRL+D", MacroAction.DirectSend("\u0004"), "EOF"),
        MacroKey("CTRL+Z", MacroAction.DirectSend("\u001A"), "Suspend"),
        MacroKey("|", MacroAction.BufferInsert("|"), "Pipe"),
        MacroKey("->", MacroAction.BufferInsert(" -> "), "Arrow"),
        MacroKey("&&", MacroAction.BufferInsert(" && "), "AND"),
        MacroKey("||", MacroAction.BufferInsert(" || "), "OR"),
        MacroKey("~/", MacroAction.BufferInsert("~/"), "Home"),
        MacroKey("../", MacroAction.BufferInsert("../"), "Parent")
    )

    val navKeys = listOf(
        MacroKey("↑", MacroAction.DirectSend("\u001B[A"), "Up"),
        MacroKey("↓", MacroAction.DirectSend("\u001B[B"), "Down"),
        MacroKey("→", MacroAction.DirectSend("\u001B[C"), "Right"),
        MacroKey("←", MacroAction.DirectSend("\u001B[D"), "Left"),
        MacroKey("Home", MacroAction.DirectSend("\u001B[H"), "Home"),
        MacroKey("End", MacroAction.DirectSend("\u001B[F"), "End"),
        MacroKey("PgUp", MacroAction.DirectSend("\u001B[5~"), "Page Up"),
        MacroKey("PgDn", MacroAction.DirectSend("\u001B[6~"), "Page Down")
    )

    val vimKeys = listOf(
        // Normal mode commands (direct send)
        MacroKey("i", MacroAction.DirectSend("i"), "Insert mode"),
        MacroKey("a", MacroAction.DirectSend("a"), "Append"),
        MacroKey("o", MacroAction.DirectSend("o"), "New line below"),
        MacroKey("O", MacroAction.DirectSend("O"), "New line above"),
        MacroKey("x", MacroAction.DirectSend("x"), "Delete char"),
        MacroKey("dd", MacroAction.DirectSend("dd"), "Delete line"),
        MacroKey("yy", MacroAction.DirectSend("yy"), "Copy line"),
        MacroKey("p", MacroAction.DirectSend("p"), "Paste"),
        MacroKey("u", MacroAction.DirectSend("u"), "Undo"),
        MacroKey("CTRL+R", MacroAction.DirectSend("\u0012"), "Redo"),
        // Command mode commands (buffer insert)
        MacroKey(":w", MacroAction.BufferInsert(":w"), "Save"),
        MacroKey(":q", MacroAction.BufferInsert(":q"), "Quit"),
        MacroKey(":wq", MacroAction.BufferInsert(":wq"), "Save & quit"),
        MacroKey(":q!", MacroAction.BufferInsert(":q!"), "Force quit"),
        // Search mode
        MacroKey("/", MacroAction.DirectSend("/"), "Search"),
        MacroKey("n", MacroAction.DirectSend("n"), "Next match"),
        MacroKey("N", MacroAction.DirectSend("N"), "Prev match")
    )

    val functionKeys = listOf(
        MacroKey("F1", MacroAction.DirectSend("\u001BOP")),
        MacroKey("F2", MacroAction.DirectSend("\u001BOQ")),
        MacroKey("F3", MacroAction.DirectSend("\u001BOR")),
        MacroKey("F4", MacroAction.DirectSend("\u001BOS")),
        MacroKey("F5", MacroAction.DirectSend("\u001B[15~")),
        MacroKey("F6", MacroAction.DirectSend("\u001B[17~")),
        MacroKey("F7", MacroAction.DirectSend("\u001B[18~")),
        MacroKey("F8", MacroAction.DirectSend("\u001B[19~")),
        MacroKey("F9", MacroAction.DirectSend("\u001B[20~")),
        MacroKey("F10", MacroAction.DirectSend("\u001B[21~")),
        MacroKey("F11", MacroAction.DirectSend("\u001B[23~")),
        MacroKey("F12", MacroAction.DirectSend("\u001B[24~"))
    )
}
