package tokyo.isseikuzumaki.vibeterminal.macro.data.builtin

import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroAction
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroKeyDefinition
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroPack
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroPackMetadata
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroPackSource
import tokyo.isseikuzumaki.vibeterminal.macro.domain.model.MacroTabDefinition

/**
 * Builtin macro packs that ship with the app.
 * These provide the default terminal control keys.
 */
object BuiltinMacroPacks {

    /**
     * The default builtin pack containing basic terminal controls.
     * Uses lazy initialization to ensure tab definitions are available.
     */
    val defaultPack: MacroPack by lazy {
        MacroPack(
            id = "builtin-default",
            metadata = MacroPackMetadata(
                name = "Default",
                description = "Default terminal macro keys",
                version = "1.0.0",
                author = "Puzzroom"
            ),
            tabs = listOf(
                basicTab,
                navTab,
                vimTab,
                functionTab
            ),
            source = MacroPackSource.BUILTIN,
            isActive = true,
            displayOrder = 0
        )
    }

    private val basicTab = MacroTabDefinition(
        id = "builtin-basic",
        name = "Basic",
        keys = listOf(
            MacroKeyDefinition(
                id = "basic-esc",
                label = "ESC",
                action = MacroAction.DirectSend("\u001B"),
                description = "Escape",
                displayOrder = 0
            ),
            MacroKeyDefinition(
                id = "basic-tab",
                label = "TAB",
                action = MacroAction.DirectSend("\t"),
                description = "Tab",
                displayOrder = 1
            ),
            MacroKeyDefinition(
                id = "basic-ctrl-c",
                label = "CTRL+C",
                action = MacroAction.DirectSend("\u0003"),
                description = "Interrupt",
                displayOrder = 2
            ),
            MacroKeyDefinition(
                id = "basic-ctrl-d",
                label = "CTRL+D",
                action = MacroAction.DirectSend("\u0004"),
                description = "EOF",
                displayOrder = 3
            ),
            MacroKeyDefinition(
                id = "basic-ctrl-z",
                label = "CTRL+Z",
                action = MacroAction.DirectSend("\u001A"),
                description = "Suspend",
                displayOrder = 4
            ),
            MacroKeyDefinition(
                id = "basic-pipe",
                label = "|",
                action = MacroAction.BufferInsert("|"),
                description = "Pipe",
                displayOrder = 5
            ),
            MacroKeyDefinition(
                id = "basic-arrow",
                label = "->",
                action = MacroAction.BufferInsert(" -> "),
                description = "Arrow",
                displayOrder = 6
            ),
            MacroKeyDefinition(
                id = "basic-and",
                label = "&&",
                action = MacroAction.BufferInsert(" && "),
                description = "AND",
                displayOrder = 7
            ),
            MacroKeyDefinition(
                id = "basic-or",
                label = "||",
                action = MacroAction.BufferInsert(" || "),
                description = "OR",
                displayOrder = 8
            ),
            MacroKeyDefinition(
                id = "basic-home",
                label = "~/",
                action = MacroAction.BufferInsert("~/"),
                description = "Home",
                displayOrder = 9
            ),
            MacroKeyDefinition(
                id = "basic-parent",
                label = "../",
                action = MacroAction.BufferInsert("../"),
                description = "Parent",
                displayOrder = 10
            )
        ),
        displayOrder = 0
    )

    private val navTab = MacroTabDefinition(
        id = "builtin-nav",
        name = "Nav",
        keys = listOf(
            MacroKeyDefinition(
                id = "nav-up",
                label = "↑",
                action = MacroAction.DirectSend("\u001B[A"),
                description = "Up",
                displayOrder = 0
            ),
            MacroKeyDefinition(
                id = "nav-down",
                label = "↓",
                action = MacroAction.DirectSend("\u001B[B"),
                description = "Down",
                displayOrder = 1
            ),
            MacroKeyDefinition(
                id = "nav-right",
                label = "→",
                action = MacroAction.DirectSend("\u001B[C"),
                description = "Right",
                displayOrder = 2
            ),
            MacroKeyDefinition(
                id = "nav-left",
                label = "←",
                action = MacroAction.DirectSend("\u001B[D"),
                description = "Left",
                displayOrder = 3
            ),
            MacroKeyDefinition(
                id = "nav-enter",
                label = "↵",
                action = MacroAction.DirectSend("\r"),
                description = "Enter",
                displayOrder = 4
            ),
            MacroKeyDefinition(
                id = "nav-home",
                label = "Home",
                action = MacroAction.DirectSend("\u001B[H"),
                description = "Home",
                displayOrder = 5
            ),
            MacroKeyDefinition(
                id = "nav-end",
                label = "End",
                action = MacroAction.DirectSend("\u001B[F"),
                description = "End",
                displayOrder = 6
            ),
            MacroKeyDefinition(
                id = "nav-pgup",
                label = "PgUp",
                action = MacroAction.DirectSend("\u001B[5~"),
                description = "Page Up",
                displayOrder = 7
            ),
            MacroKeyDefinition(
                id = "nav-pgdn",
                label = "PgDn",
                action = MacroAction.DirectSend("\u001B[6~"),
                description = "Page Down",
                displayOrder = 8
            )
        ),
        displayOrder = 1
    )

    private val vimTab = MacroTabDefinition(
        id = "builtin-vim",
        name = "Vim",
        keys = listOf(
            MacroKeyDefinition(
                id = "vim-i",
                label = "i",
                action = MacroAction.DirectSend("i"),
                description = "Insert mode",
                displayOrder = 0
            ),
            MacroKeyDefinition(
                id = "vim-a",
                label = "a",
                action = MacroAction.DirectSend("a"),
                description = "Append",
                displayOrder = 1
            ),
            MacroKeyDefinition(
                id = "vim-o",
                label = "o",
                action = MacroAction.DirectSend("o"),
                description = "New line below",
                displayOrder = 2
            ),
            MacroKeyDefinition(
                id = "vim-O",
                label = "O",
                action = MacroAction.DirectSend("O"),
                description = "New line above",
                displayOrder = 3
            ),
            MacroKeyDefinition(
                id = "vim-x",
                label = "x",
                action = MacroAction.DirectSend("x"),
                description = "Delete char",
                displayOrder = 4
            ),
            MacroKeyDefinition(
                id = "vim-dd",
                label = "dd",
                action = MacroAction.DirectSend("dd"),
                description = "Delete line",
                displayOrder = 5
            ),
            MacroKeyDefinition(
                id = "vim-yy",
                label = "yy",
                action = MacroAction.DirectSend("yy"),
                description = "Copy line",
                displayOrder = 6
            ),
            MacroKeyDefinition(
                id = "vim-p",
                label = "p",
                action = MacroAction.DirectSend("p"),
                description = "Paste",
                displayOrder = 7
            ),
            MacroKeyDefinition(
                id = "vim-u",
                label = "u",
                action = MacroAction.DirectSend("u"),
                description = "Undo",
                displayOrder = 8
            ),
            MacroKeyDefinition(
                id = "vim-ctrl-r",
                label = "CTRL+R",
                action = MacroAction.DirectSend("\u0012"),
                description = "Redo",
                displayOrder = 9
            ),
            MacroKeyDefinition(
                id = "vim-w",
                label = ":w",
                action = MacroAction.BufferInsert(":w"),
                description = "Save",
                displayOrder = 10
            ),
            MacroKeyDefinition(
                id = "vim-q",
                label = ":q",
                action = MacroAction.BufferInsert(":q"),
                description = "Quit",
                displayOrder = 11
            ),
            MacroKeyDefinition(
                id = "vim-wq",
                label = ":wq",
                action = MacroAction.BufferInsert(":wq"),
                description = "Save & quit",
                displayOrder = 12
            ),
            MacroKeyDefinition(
                id = "vim-q-force",
                label = ":q!",
                action = MacroAction.BufferInsert(":q!"),
                description = "Force quit",
                displayOrder = 13
            ),
            MacroKeyDefinition(
                id = "vim-search",
                label = "/",
                action = MacroAction.DirectSend("/"),
                description = "Search",
                displayOrder = 14
            ),
            MacroKeyDefinition(
                id = "vim-next",
                label = "n",
                action = MacroAction.DirectSend("n"),
                description = "Next match",
                displayOrder = 15
            ),
            MacroKeyDefinition(
                id = "vim-prev",
                label = "N",
                action = MacroAction.DirectSend("N"),
                description = "Prev match",
                displayOrder = 16
            )
        ),
        displayOrder = 2
    )

    private val functionTab = MacroTabDefinition(
        id = "builtin-function",
        name = "Function",
        keys = listOf(
            MacroKeyDefinition(
                id = "fn-f1",
                label = "F1",
                action = MacroAction.DirectSend("\u001BOP"),
                displayOrder = 0
            ),
            MacroKeyDefinition(
                id = "fn-f2",
                label = "F2",
                action = MacroAction.DirectSend("\u001BOQ"),
                displayOrder = 1
            ),
            MacroKeyDefinition(
                id = "fn-f3",
                label = "F3",
                action = MacroAction.DirectSend("\u001BOR"),
                displayOrder = 2
            ),
            MacroKeyDefinition(
                id = "fn-f4",
                label = "F4",
                action = MacroAction.DirectSend("\u001BOS"),
                displayOrder = 3
            ),
            MacroKeyDefinition(
                id = "fn-f5",
                label = "F5",
                action = MacroAction.DirectSend("\u001B[15~"),
                displayOrder = 4
            ),
            MacroKeyDefinition(
                id = "fn-f6",
                label = "F6",
                action = MacroAction.DirectSend("\u001B[17~"),
                displayOrder = 5
            ),
            MacroKeyDefinition(
                id = "fn-f7",
                label = "F7",
                action = MacroAction.DirectSend("\u001B[18~"),
                displayOrder = 6
            ),
            MacroKeyDefinition(
                id = "fn-f8",
                label = "F8",
                action = MacroAction.DirectSend("\u001B[19~"),
                displayOrder = 7
            ),
            MacroKeyDefinition(
                id = "fn-f9",
                label = "F9",
                action = MacroAction.DirectSend("\u001B[20~"),
                displayOrder = 8
            ),
            MacroKeyDefinition(
                id = "fn-f10",
                label = "F10",
                action = MacroAction.DirectSend("\u001B[21~"),
                displayOrder = 9
            ),
            MacroKeyDefinition(
                id = "fn-f11",
                label = "F11",
                action = MacroAction.DirectSend("\u001B[23~"),
                displayOrder = 10
            ),
            MacroKeyDefinition(
                id = "fn-f12",
                label = "F12",
                action = MacroAction.DirectSend("\u001B[24~"),
                displayOrder = 11
            )
        ),
        displayOrder = 3
    )

    /**
     * All builtin packs.
     */
    val allPacks: List<MacroPack> by lazy { listOf(defaultPack) }
}
