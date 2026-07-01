package tokyo.isseikuzumaki.vibeterminal.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for UnicodeWidth utility.
 * Tests wide character (full-width) detection for CJK characters.
 *
 * Wide characters (2-cell width):
 * - CJK Unified Ideographs (Chinese, Japanese Kanji, Korean Hanja)
 * - Hiragana and Katakana
 * - Full-width ASCII variants
 * - Korean Hangul syllables
 *
 * Narrow characters (1-cell width):
 * - ASCII letters, numbers, punctuation
 * - Latin characters
 * - Half-width Katakana
 */
class UnicodeWidthTest {

    // ========== Narrow Characters (1-cell width) ==========

    @Test
    fun testNarrowChar_ASCII_Letters() {
        assertFalse(UnicodeWidth.isWideChar('A'), "ASCII uppercase should be narrow")
        assertFalse(UnicodeWidth.isWideChar('z'), "ASCII lowercase should be narrow")
        assertFalse(UnicodeWidth.isWideChar('M'), "ASCII M should be narrow")
    }

    @Test
    fun testNarrowChar_ASCII_Numbers() {
        assertFalse(UnicodeWidth.isWideChar('0'), "ASCII 0 should be narrow")
        assertFalse(UnicodeWidth.isWideChar('9'), "ASCII 9 should be narrow")
        assertFalse(UnicodeWidth.isWideChar('5'), "ASCII 5 should be narrow")
    }

    @Test
    fun testNarrowChar_ASCII_Punctuation() {
        assertFalse(UnicodeWidth.isWideChar('.'), "Period should be narrow")
        assertFalse(UnicodeWidth.isWideChar(','), "Comma should be narrow")
        assertFalse(UnicodeWidth.isWideChar('!'), "Exclamation should be narrow")
        assertFalse(UnicodeWidth.isWideChar('?'), "Question mark should be narrow")
        assertFalse(UnicodeWidth.isWideChar(' '), "Space should be narrow")
    }

    @Test
    fun testNarrowChar_Latin_Extended() {
        assertFalse(UnicodeWidth.isWideChar('é'), "Latin e with acute should be narrow")
        assertFalse(UnicodeWidth.isWideChar('ñ'), "Latin n with tilde should be narrow")
        assertFalse(UnicodeWidth.isWideChar('ü'), "Latin u with umlaut should be narrow")
    }

    // ========== Wide Characters - Hiragana (2-cell width) ==========

    @Test
    fun testWideChar_Hiragana() {
        assertTrue(UnicodeWidth.isWideChar('あ'), "Hiragana 'a' should be wide")
        assertTrue(UnicodeWidth.isWideChar('い'), "Hiragana 'i' should be wide")
        assertTrue(UnicodeWidth.isWideChar('う'), "Hiragana 'u' should be wide")
        assertTrue(UnicodeWidth.isWideChar('え'), "Hiragana 'e' should be wide")
        assertTrue(UnicodeWidth.isWideChar('お'), "Hiragana 'o' should be wide")
        assertTrue(UnicodeWidth.isWideChar('ん'), "Hiragana 'n' should be wide")
    }

    @Test
    fun testWideChar_Hiragana_Extended() {
        assertTrue(UnicodeWidth.isWideChar('が'), "Hiragana 'ga' should be wide")
        assertTrue(UnicodeWidth.isWideChar('ぱ'), "Hiragana 'pa' should be wide")
        assertTrue(UnicodeWidth.isWideChar('っ'), "Small tsu should be wide")
        assertTrue(UnicodeWidth.isWideChar('ゃ'), "Small ya should be wide")
    }

    // ========== Wide Characters - Katakana (2-cell width) ==========

    @Test
    fun testWideChar_Katakana() {
        assertTrue(UnicodeWidth.isWideChar('ア'), "Katakana 'a' should be wide")
        assertTrue(UnicodeWidth.isWideChar('イ'), "Katakana 'i' should be wide")
        assertTrue(UnicodeWidth.isWideChar('ウ'), "Katakana 'u' should be wide")
        assertTrue(UnicodeWidth.isWideChar('エ'), "Katakana 'e' should be wide")
        assertTrue(UnicodeWidth.isWideChar('オ'), "Katakana 'o' should be wide")
        assertTrue(UnicodeWidth.isWideChar('ン'), "Katakana 'n' should be wide")
    }

    @Test
    fun testWideChar_Katakana_Extended() {
        assertTrue(UnicodeWidth.isWideChar('ガ'), "Katakana 'ga' should be wide")
        assertTrue(UnicodeWidth.isWideChar('パ'), "Katakana 'pa' should be wide")
        assertTrue(UnicodeWidth.isWideChar('ッ'), "Small tsu should be wide")
        assertTrue(UnicodeWidth.isWideChar('ャ'), "Small ya should be wide")
    }

    // ========== Wide Characters - Kanji/CJK Unified Ideographs (2-cell width) ==========

    @Test
    fun testWideChar_Kanji_Common() {
        assertTrue(UnicodeWidth.isWideChar('日'), "Kanji 'sun/day' should be wide")
        assertTrue(UnicodeWidth.isWideChar('本'), "Kanji 'book/origin' should be wide")
        assertTrue(UnicodeWidth.isWideChar('語'), "Kanji 'language' should be wide")
        assertTrue(UnicodeWidth.isWideChar('人'), "Kanji 'person' should be wide")
        assertTrue(UnicodeWidth.isWideChar('大'), "Kanji 'big' should be wide")
        assertTrue(UnicodeWidth.isWideChar('中'), "Kanji 'middle' should be wide")
    }

    @Test
    fun testWideChar_Kanji_Numbers() {
        assertTrue(UnicodeWidth.isWideChar('一'), "Kanji 'one' should be wide")
        assertTrue(UnicodeWidth.isWideChar('二'), "Kanji 'two' should be wide")
        assertTrue(UnicodeWidth.isWideChar('三'), "Kanji 'three' should be wide")
        assertTrue(UnicodeWidth.isWideChar('十'), "Kanji 'ten' should be wide")
        assertTrue(UnicodeWidth.isWideChar('百'), "Kanji 'hundred' should be wide")
        assertTrue(UnicodeWidth.isWideChar('千'), "Kanji 'thousand' should be wide")
    }

    // ========== Wide Characters - Korean Hangul (2-cell width) ==========

    @Test
    fun testWideChar_Hangul() {
        assertTrue(UnicodeWidth.isWideChar('한'), "Hangul 'han' should be wide")
        assertTrue(UnicodeWidth.isWideChar('글'), "Hangul 'geul' should be wide")
        assertTrue(UnicodeWidth.isWideChar('가'), "Hangul 'ga' should be wide")
        assertTrue(UnicodeWidth.isWideChar('나'), "Hangul 'na' should be wide")
    }

    // ========== Wide Characters - Full-width Forms (2-cell width) ==========

    @Test
    fun testWideChar_FullWidth_ASCII() {
        assertTrue(UnicodeWidth.isWideChar('Ａ'), "Full-width A should be wide")
        assertTrue(UnicodeWidth.isWideChar('Ｚ'), "Full-width Z should be wide")
        assertTrue(UnicodeWidth.isWideChar('０'), "Full-width 0 should be wide")
        assertTrue(UnicodeWidth.isWideChar('９'), "Full-width 9 should be wide")
    }

    @Test
    fun testWideChar_FullWidth_Punctuation() {
        assertTrue(UnicodeWidth.isWideChar('。'), "Full-width period should be wide")
        assertTrue(UnicodeWidth.isWideChar('、'), "Full-width comma should be wide")
        assertTrue(UnicodeWidth.isWideChar('！'), "Full-width exclamation should be wide")
        assertTrue(UnicodeWidth.isWideChar('？'), "Full-width question should be wide")
    }

    // ========== Wide Characters - CJK Symbols (2-cell width) ==========

    @Test
    fun testWideChar_CJK_Symbols() {
        assertTrue(UnicodeWidth.isWideChar('【'), "CJK bracket should be wide")
        assertTrue(UnicodeWidth.isWideChar('】'), "CJK bracket should be wide")
        assertTrue(UnicodeWidth.isWideChar('「'), "CJK quote should be wide")
        assertTrue(UnicodeWidth.isWideChar('」'), "CJK quote should be wide")
        assertTrue(UnicodeWidth.isWideChar('　'), "Ideographic space should be wide")
    }

    // ========== charWidth function ==========

    @Test
    fun testCharWidth_Narrow() {
        assertEquals(1, UnicodeWidth.charWidth('A'), "ASCII A should have width 1")
        assertEquals(1, UnicodeWidth.charWidth('0'), "ASCII 0 should have width 1")
        assertEquals(1, UnicodeWidth.charWidth(' '), "Space should have width 1")
    }

    @Test
    fun testCharWidth_Wide() {
        assertEquals(2, UnicodeWidth.charWidth('あ'), "Hiragana should have width 2")
        assertEquals(2, UnicodeWidth.charWidth('ア'), "Katakana should have width 2")
        assertEquals(2, UnicodeWidth.charWidth('日'), "Kanji should have width 2")
        assertEquals(2, UnicodeWidth.charWidth('한'), "Hangul should have width 2")
    }

    // ========== Edge Cases ==========

    @Test
    fun testEdgeCase_NullChar() {
        assertFalse(UnicodeWidth.isWideChar('\u0000'), "Null char should be narrow")
        assertEquals(1, UnicodeWidth.charWidth('\u0000'), "Null char should have width 1")
    }

    @Test
    fun testEdgeCase_ControlChars() {
        assertFalse(UnicodeWidth.isWideChar('\n'), "Newline should be narrow")
        assertFalse(UnicodeWidth.isWideChar('\t'), "Tab should be narrow")
        assertFalse(UnicodeWidth.isWideChar('\r'), "CR should be narrow")
    }

    // ========== Boundary Testing - Unicode Ranges ==========

    @Test
    fun testBoundary_HiraganaRange() {
        // Hiragana: U+3040 - U+309F
        assertTrue(UnicodeWidth.isWideChar('\u3041'), "First hiragana should be wide")
        assertTrue(UnicodeWidth.isWideChar('\u309F'), "Last hiragana should be wide")
    }

    @Test
    fun testBoundary_KatakanaRange() {
        // Katakana: U+30A0 - U+30FF
        assertTrue(UnicodeWidth.isWideChar('\u30A0'), "First katakana should be wide")
        assertTrue(UnicodeWidth.isWideChar('\u30FF'), "Last katakana should be wide")
    }

    @Test
    fun testBoundary_CJKUnifiedRange() {
        // CJK Unified Ideographs: U+4E00 - U+9FFF
        assertTrue(UnicodeWidth.isWideChar('\u4E00'), "First CJK ideograph should be wide")
        assertTrue(UnicodeWidth.isWideChar('\u9FFF'), "Last CJK ideograph should be wide")
    }

    @Test
    fun testBoundary_HangulRange() {
        // Hangul Syllables: U+AC00 - U+D7AF
        assertTrue(UnicodeWidth.isWideChar('\uAC00'), "First hangul syllable should be wide")
        assertTrue(UnicodeWidth.isWideChar('\uD7AF'), "Last hangul syllable should be wide")
    }
}
