package tokyo.isseikuzumaki.vibeterminal.input

/**
 * Platform-independent representation of a key event.
 * This allows business logic to be tested without Android dependencies.
 */
data class KeyEventData(
    val keyCode: Int,
    val action: KeyAction,
    val isCtrlPressed: Boolean = false,
    val isAltPressed: Boolean = false,
    val isShiftPressed: Boolean = false,
    val isMetaPressed: Boolean = false
)

enum class KeyAction {
    DOWN,
    UP
}

/**
 * Platform-independent key codes that mirror common keyboard keys.
 * Values are chosen to match Android KeyEvent codes for easy mapping.
 */
object KeyCodes {
    // Letters A-Z (29-54)
    const val A = 29
    const val B = 30
    const val C = 31
    const val D = 32
    const val E = 33
    const val F = 34
    const val G = 35
    const val H = 36
    const val I = 37
    const val J = 38
    const val K = 39
    const val L = 40
    const val M = 41
    const val N = 42
    const val O = 43
    const val P = 44
    const val Q = 45
    const val R = 46
    const val S = 47
    const val T = 48
    const val U = 49
    const val V = 50
    const val W = 51
    const val X = 52
    const val Y = 53
    const val Z = 54

    // Numbers 0-9 (7-16)
    const val NUM_0 = 7
    const val NUM_1 = 8
    const val NUM_2 = 9
    const val NUM_3 = 10
    const val NUM_4 = 11
    const val NUM_5 = 12
    const val NUM_6 = 13
    const val NUM_7 = 14
    const val NUM_8 = 15
    const val NUM_9 = 16

    // Arrow keys
    const val DPAD_UP = 19
    const val DPAD_DOWN = 20
    const val DPAD_LEFT = 21
    const val DPAD_RIGHT = 22

    // Navigation keys
    const val MOVE_HOME = 122
    const val MOVE_END = 123
    const val PAGE_UP = 92
    const val PAGE_DOWN = 93
    const val INSERT = 124
    const val FORWARD_DEL = 112

    // Special keys
    const val ENTER = 66
    const val NUMPAD_ENTER = 160
    const val TAB = 61
    const val ESCAPE = 111
    const val DEL = 67  // Backspace
    const val SPACE = 62

    // Function keys F1-F12
    const val F1 = 131
    const val F2 = 132
    const val F3 = 133
    const val F4 = 134
    const val F5 = 135
    const val F6 = 136
    const val F7 = 137
    const val F8 = 138
    const val F9 = 139
    const val F10 = 140
    const val F11 = 141
    const val F12 = 142

    // Modifier keys
    const val SHIFT_LEFT = 59
    const val SHIFT_RIGHT = 60
    const val CTRL_LEFT = 113
    const val CTRL_RIGHT = 114
    const val ALT_LEFT = 57
    const val ALT_RIGHT = 58
    const val META_LEFT = 117
    const val META_RIGHT = 118
    const val CAPS_LOCK = 115
    const val NUM_LOCK = 143
    const val SCROLL_LOCK = 116
    const val FUNCTION = 119

    // Symbols
    const val MINUS = 69
    const val EQUALS = 70
    const val LEFT_BRACKET = 71
    const val RIGHT_BRACKET = 72
    const val BACKSLASH = 73
    const val SEMICOLON = 74
    const val APOSTROPHE = 75
    const val COMMA = 55
    const val PERIOD = 56
    const val SLASH = 76
    const val GRAVE = 68

    // JIS-specific keys
    const val YEN = 216
    const val RO = 217
}
