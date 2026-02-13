package tokyo.isseikuzumaki.vibeterminal.ui.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression tests for SyntaxHighlighter.
 *
 * These tests verify the fix for StringIndexOutOfBoundsException
 * when parsing unterminated strings (Issue #162).
 */
class SyntaxHighlighterTest {

    // ========================================
    // Regression Tests (Bug Fix Verification)
    // ========================================

    /**
     * Test: Unterminated double-quoted string at EOF should not throw exception.
     *
     * This was the original bug - the highlighter would throw
     * StringIndexOutOfBoundsException when a string reached end of content
     * without a closing quote.
     */
    @Test
    fun highlight_unterminatedDoubleQuote_noException() {
        val result = SyntaxHighlighter.highlight("val x = \"hello", "kt")
        assertNotNull(result)
        assertEquals("val x = \"hello", result.text)
    }

    /**
     * Test: Unterminated single-quoted string at EOF should not throw exception.
     */
    @Test
    fun highlight_unterminatedSingleQuote_noException() {
        val result = SyntaxHighlighter.highlight("val x = 'hello", "kt")
        assertNotNull(result)
        assertEquals("val x = 'hello", result.text)
    }

    /**
     * Test: Escape character at end of string content should not throw exception.
     *
     * When a backslash is the last character before EOF, the increment past
     * the escape sequence must be bounds-checked.
     */
    @Test
    fun highlight_escapeAtEndOfString_noException() {
        val result = SyntaxHighlighter.highlight("val x = \"hello\\", "kt")
        assertNotNull(result)
        assertEquals("val x = \"hello\\", result.text)
    }

    /**
     * Test: Just an opening quote at EOF should not throw exception.
     */
    @Test
    fun highlight_emptyUnterminatedString_noException() {
        val result = SyntaxHighlighter.highlight("val x = \"", "kt")
        assertNotNull(result)
        assertEquals("val x = \"", result.text)
    }

    /**
     * Test: JSON with unterminated string should not throw exception.
     */
    @Test
    fun highlightJson_unterminatedString_noException() {
        val result = SyntaxHighlighter.highlight("{\"key\": \"value", "json")
        assertNotNull(result)
        assertEquals("{\"key\": \"value", result.text)
    }

    /**
     * Test: JSON with escape at end should not throw exception.
     */
    @Test
    fun highlightJson_escapeAtEnd_noException() {
        val result = SyntaxHighlighter.highlight("{\"key\": \"value\\", "json")
        assertNotNull(result)
        assertEquals("{\"key\": \"value\\", result.text)
    }

    /**
     * Test: Java with unterminated string should not throw exception.
     */
    @Test
    fun highlightJava_unterminatedString_noException() {
        val result = SyntaxHighlighter.highlight("String s = \"hello", "java")
        assertNotNull(result)
        assertEquals("String s = \"hello", result.text)
    }

    /**
     * Test: Python with unterminated string should not throw exception.
     */
    @Test
    fun highlightPython_unterminatedString_noException() {
        val result = SyntaxHighlighter.highlight("s = \"hello", "py")
        assertNotNull(result)
        assertEquals("s = \"hello", result.text)
    }

    /**
     * Test: JavaScript with unterminated string should not throw exception.
     */
    @Test
    fun highlightJavaScript_unterminatedString_noException() {
        val result = SyntaxHighlighter.highlight("const s = \"hello", "js")
        assertNotNull(result)
        assertEquals("const s = \"hello", result.text)
    }

    // ========================================
    // Functional Tests (Ensure Fix Doesn't Break Existing)
    // ========================================

    /**
     * Test: Valid Kotlin string should be highlighted correctly.
     */
    @Test
    fun highlight_validKotlinString_highlighted() {
        val result = SyntaxHighlighter.highlight("val x = \"hello\"", "kt")
        assertEquals("val x = \"hello\"", result.text)
        // Verify the string is present in the annotated string
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles")
    }

    /**
     * Test: Escaped quotes inside string should be handled correctly.
     */
    @Test
    fun highlight_escapedQuoteInString_highlighted() {
        val result = SyntaxHighlighter.highlight("val x = \"say \\\"hi\\\"\"", "kt")
        assertEquals("val x = \"say \\\"hi\\\"\"", result.text)
        assertNotNull(result)
    }

    /**
     * Test: Kotlin keywords should be highlighted.
     */
    @Test
    fun highlight_kotlinKeywords_highlighted() {
        val result = SyntaxHighlighter.highlight("fun test()", "kt")
        assertEquals("fun test()", result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles for keywords")
    }

    /**
     * Test: Single-line comment should be highlighted.
     */
    @Test
    fun highlight_singleLineComment_highlighted() {
        val result = SyntaxHighlighter.highlight("// comment", "kt")
        assertEquals("// comment", result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles for comment")
    }

    /**
     * Test: Multi-line comment should be highlighted.
     */
    @Test
    fun highlight_multiLineComment_highlighted() {
        val result = SyntaxHighlighter.highlight("/* comment */", "kt")
        assertEquals("/* comment */", result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles for comment")
    }

    /**
     * Test: Numbers should be highlighted.
     */
    @Test
    fun highlight_numbers_highlighted() {
        val result = SyntaxHighlighter.highlight("val x = 42", "kt")
        assertEquals("val x = 42", result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles for number")
    }

    /**
     * Test: Floating point numbers should be highlighted.
     */
    @Test
    fun highlight_floatNumbers_highlighted() {
        val result = SyntaxHighlighter.highlight("val x = 3.14f", "kt")
        assertEquals("val x = 3.14f", result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles for float")
    }

    /**
     * Test: JSON numbers should be highlighted.
     */
    @Test
    fun highlightJson_numbers_highlighted() {
        val result = SyntaxHighlighter.highlight("{\"count\": 42}", "json")
        assertEquals("{\"count\": 42}", result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles")
    }

    /**
     * Test: JSON negative numbers should be highlighted.
     */
    @Test
    fun highlightJson_negativeNumbers_highlighted() {
        val result = SyntaxHighlighter.highlight("{\"temp\": -10.5}", "json")
        assertEquals("{\"temp\": -10.5}", result.text)
        assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles")
    }

    // ========================================
    // Edge Cases
    // ========================================

    /**
     * Test: Empty content should return empty AnnotatedString.
     */
    @Test
    fun highlight_emptyContent_returnsEmpty() {
        val result = SyntaxHighlighter.highlight("", "kt")
        assertEquals("", result.text)
    }

    /**
     * Test: Only whitespace should not throw exception.
     */
    @Test
    fun highlight_onlyWhitespace_noException() {
        val result = SyntaxHighlighter.highlight("   \n\t", "kt")
        assertNotNull(result)
        assertEquals("   \n\t", result.text)
    }

    /**
     * Test: Unknown extension should use default color and not throw.
     */
    @Test
    fun highlight_unknownExtension_defaultColor() {
        val result = SyntaxHighlighter.highlight("random text", "xyz")
        assertNotNull(result)
        assertEquals("random text", result.text)
    }

    /**
     * Test: Unclosed multi-line comment should not throw exception.
     */
    @Test
    fun highlight_unclosedMultiLineComment_noException() {
        val result = SyntaxHighlighter.highlight("/* unclosed comment", "kt")
        assertNotNull(result)
        assertEquals("/* unclosed comment", result.text)
    }

    /**
     * Test: Multiple unterminated strings in sequence should not throw.
     */
    @Test
    fun highlight_multipleUnterminatedStrings_noException() {
        val result = SyntaxHighlighter.highlight("val a = \"first\nval b = \"second", "kt")
        assertNotNull(result)
        assertTrue(result.text.contains("first"))
        assertTrue(result.text.contains("second"))
    }

    /**
     * Test: String followed by newline then unterminated should work.
     */
    @Test
    fun highlight_terminatedThenUnterminated_noException() {
        val result = SyntaxHighlighter.highlight("val a = \"complete\"\nval b = \"incomplete", "kt")
        assertNotNull(result)
        assertEquals("val a = \"complete\"\nval b = \"incomplete", result.text)
    }

    /**
     * Test: Single character content should not throw.
     */
    @Test
    fun highlight_singleCharacter_noException() {
        val result = SyntaxHighlighter.highlight("x", "kt")
        assertNotNull(result)
        assertEquals("x", result.text)
    }

    /**
     * Test: Single quote character should not throw.
     */
    @Test
    fun highlight_singleQuoteCharacter_noException() {
        val result = SyntaxHighlighter.highlight("\"", "kt")
        assertNotNull(result)
        assertEquals("\"", result.text)
    }

    /**
     * Test: TypeScript extension should work.
     */
    @Test
    fun highlight_typescript_works() {
        val result = SyntaxHighlighter.highlight("const x = \"hello\"", "ts")
        assertNotNull(result)
        assertEquals("const x = \"hello\"", result.text)
    }

    /**
     * Test: JSX extension should work.
     */
    @Test
    fun highlight_jsx_works() {
        val result = SyntaxHighlighter.highlight("const x = <div>", "jsx")
        assertNotNull(result)
        assertEquals("const x = <div>", result.text)
    }

    /**
     * Test: TSX extension should work.
     */
    @Test
    fun highlight_tsx_works() {
        val result = SyntaxHighlighter.highlight("const x: string = \"test\"", "tsx")
        assertNotNull(result)
        assertEquals("const x: string = \"test\"", result.text)
    }

    /**
     * Test: XML extension should work.
     */
    @Test
    fun highlight_xml_works() {
        val result = SyntaxHighlighter.highlight("<root><child /></root>", "xml")
        assertNotNull(result)
        assertEquals("<root><child /></root>", result.text)
    }

    /**
     * Test: HTML extension should work.
     */
    @Test
    fun highlight_html_works() {
        val result = SyntaxHighlighter.highlight("<html><body></body></html>", "html")
        assertNotNull(result)
        assertEquals("<html><body></body></html>", result.text)
    }

    /**
     * Test: Markdown extension should work.
     */
    @Test
    fun highlight_markdown_works() {
        val result = SyntaxHighlighter.highlight("# Title\n\nText", "md")
        assertNotNull(result)
        assertEquals("# Title\n\nText", result.text)
    }

    /**
     * Test: KTS extension should work like Kotlin.
     */
    @Test
    fun highlight_kts_works() {
        val result = SyntaxHighlighter.highlight("val x = \"hello\"", "kts")
        assertNotNull(result)
        assertEquals("val x = \"hello\"", result.text)
    }
}
