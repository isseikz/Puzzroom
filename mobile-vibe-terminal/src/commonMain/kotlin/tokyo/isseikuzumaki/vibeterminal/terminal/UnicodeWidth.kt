package tokyo.isseikuzumaki.vibeterminal.terminal

/**
 * Utility for determining character width in a terminal.
 * Based on East Asian Width properties.
 */
object UnicodeWidth {

    /**
     * Returns true if the character is a wide character (occupies 2 cells).
     */
    fun isWideChar(char: Char): Boolean {
        val code = char.code
        
        // Quick check for ASCII
        if (code < 0x80) return false

        // Check specific ranges
        return when (code) {
            // CJK Symbols and Punctuation (e.g., 、。space)
            in 0x3000..0x303F -> true
            // Hiragana
            in 0x3040..0x309F -> true
            // Katakana
            in 0x30A0..0x30FF -> true
            // CJK Unified Ideographs
            in 0x4E00..0x9FFF -> true
            // Hangul Syllables
            in 0xAC00..0xD7AF -> true
            // Fullwidth Forms (ASCII variants and symbols)
            // U+FF01 (!) to U+FF60 (fullwidth paranthesis)
            in 0xFF01..0xFF60 -> true
            // Halfwidth forms are narrow (U+FF61..U+FFDC), so we skip them
            // Fullwidth symbol variants (U+FFE0..U+FFE6)
            in 0xFFE0..0xFFE6 -> true
            else -> false
        }
    }

    /**
     * Returns the width of the character in cells (1 or 2).
     * Control characters generally return 1 (or 0 if ignored, but here we assume 1 for placeholder).
     */
    fun charWidth(char: Char): Int {
        if (char == '\u0000') return 1 // Treat null char as width 1 (usually space)
        // Control chars
        if (char < ' ' && char != '\u001B') return 1 // Default to 1 for unknown control chars
        
        return if (isWideChar(char)) 2 else 1
    }
}
