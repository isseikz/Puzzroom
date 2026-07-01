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
    // Test Case Data Classes
    // ========================================

    /**
     * Test case for basic highlight operations that should not throw exceptions.
     */
    private data class HighlightTestCase(
        val name: String,
        val content: String,
        val extension: String,
        val expectedText: String = content  // defaults to same as content
    )

    /**
     * Test case for highlight operations that should produce span styles.
     */
    private data class HighlightWithStylesTestCase(
        val name: String,
        val content: String,
        val extension: String
    )

    // ========================================
    // Regression Tests (Bug Fix Verification)
    // Issue #162: StringIndexOutOfBoundsException with unterminated strings
    // ========================================

    private val unterminatedStringTestCases = listOf(
        // Kotlin unterminated strings
        HighlightTestCase(
            name = "Kotlin unterminated double quote",
            content = "val x = \"hello",
            extension = "kt"
        ),
        HighlightTestCase(
            name = "Kotlin unterminated single quote",
            content = "val x = 'hello",
            extension = "kt"
        ),
        HighlightTestCase(
            name = "Kotlin escape at end of string",
            content = "val x = \"hello\\",
            extension = "kt"
        ),
        HighlightTestCase(
            name = "Kotlin empty unterminated string",
            content = "val x = \"",
            extension = "kt"
        ),
        HighlightTestCase(
            name = "Kotlin single quote character only",
            content = "\"",
            extension = "kt"
        ),
        // JSON unterminated strings
        HighlightTestCase(
            name = "JSON unterminated string",
            content = "{\"key\": \"value",
            extension = "json"
        ),
        HighlightTestCase(
            name = "JSON escape at end",
            content = "{\"key\": \"value\\",
            extension = "json"
        ),
        // Other languages
        HighlightTestCase(
            name = "Java unterminated string",
            content = "String s = \"hello",
            extension = "java"
        ),
        HighlightTestCase(
            name = "Python unterminated string",
            content = "s = \"hello",
            extension = "py"
        ),
        HighlightTestCase(
            name = "JavaScript unterminated string",
            content = "const s = \"hello",
            extension = "js"
        ),
        // Multi-line cases
        HighlightTestCase(
            name = "Kotlin multiple unterminated strings",
            content = "val a = \"first\nval b = \"second",
            extension = "kt"
        ),
        HighlightTestCase(
            name = "Kotlin terminated then unterminated",
            content = "val a = \"complete\"\nval b = \"incomplete",
            extension = "kt"
        ),
        // Unclosed comments
        HighlightTestCase(
            name = "Kotlin unclosed multi-line comment",
            content = "/* unclosed comment",
            extension = "kt"
        )
    )

    @Test
    fun unterminatedStrings_noException() {
        unterminatedStringTestCases.forEach { case ->
            val result = SyntaxHighlighter.highlight(case.content, case.extension)
            assertNotNull(result, "Result should not be null for: ${case.name}")
            assertEquals(case.expectedText, result.text, "Text mismatch for: ${case.name}")
        }
    }

    // ========================================
    // Functional Tests (Ensure Fix Doesn't Break Existing)
    // ========================================

    private val validHighlightTestCases = listOf(
        HighlightWithStylesTestCase(
            name = "Kotlin valid string",
            content = "val x = \"hello\"",
            extension = "kt"
        ),
        HighlightWithStylesTestCase(
            name = "Kotlin escaped quotes in string",
            content = "val x = \"say \\\"hi\\\"\"",
            extension = "kt"
        ),
        HighlightWithStylesTestCase(
            name = "Kotlin keywords",
            content = "fun test()",
            extension = "kt"
        ),
        HighlightWithStylesTestCase(
            name = "Kotlin single-line comment",
            content = "// comment",
            extension = "kt"
        ),
        HighlightWithStylesTestCase(
            name = "Kotlin multi-line comment",
            content = "/* comment */",
            extension = "kt"
        ),
        HighlightWithStylesTestCase(
            name = "Kotlin integer number",
            content = "val x = 42",
            extension = "kt"
        ),
        HighlightWithStylesTestCase(
            name = "Kotlin float number",
            content = "val x = 3.14f",
            extension = "kt"
        ),
        HighlightWithStylesTestCase(
            name = "JSON number",
            content = "{\"count\": 42}",
            extension = "json"
        ),
        HighlightWithStylesTestCase(
            name = "JSON negative number",
            content = "{\"temp\": -10.5}",
            extension = "json"
        )
    )

    @Test
    fun validContent_hasSpanStyles() {
        validHighlightTestCases.forEach { case ->
            val result = SyntaxHighlighter.highlight(case.content, case.extension)
            assertEquals(case.content, result.text, "Text mismatch for: ${case.name}")
            assertTrue(result.spanStyles.isNotEmpty(), "Should have span styles for: ${case.name}")
        }
    }

    // ========================================
    // Edge Cases
    // ========================================

    private val edgeCaseTestCases = listOf(
        HighlightTestCase(
            name = "empty content",
            content = "",
            extension = "kt"
        ),
        HighlightTestCase(
            name = "whitespace only",
            content = "   \n\t",
            extension = "kt"
        ),
        HighlightTestCase(
            name = "unknown extension",
            content = "random text",
            extension = "xyz"
        ),
        HighlightTestCase(
            name = "single character",
            content = "x",
            extension = "kt"
        )
    )

    @Test
    fun edgeCases_noException() {
        edgeCaseTestCases.forEach { case ->
            val result = SyntaxHighlighter.highlight(case.content, case.extension)
            assertNotNull(result, "Result should not be null for: ${case.name}")
            assertEquals(case.expectedText, result.text, "Text mismatch for: ${case.name}")
        }
    }

    // ========================================
    // File Extension Support Tests
    // ========================================

    private val extensionTestCases = listOf(
        // Kotlin variants
        HighlightTestCase(
            name = "kt extension",
            content = "val x = \"hello\"",
            extension = "kt"
        ),
        HighlightTestCase(
            name = "kts extension",
            content = "val x = \"hello\"",
            extension = "kts"
        ),
        // JavaScript variants
        HighlightTestCase(
            name = "js extension",
            content = "const x = \"hello\"",
            extension = "js"
        ),
        HighlightTestCase(
            name = "ts extension",
            content = "const x = \"hello\"",
            extension = "ts"
        ),
        HighlightTestCase(
            name = "jsx extension",
            content = "const x = <div>",
            extension = "jsx"
        ),
        HighlightTestCase(
            name = "tsx extension",
            content = "const x: string = \"test\"",
            extension = "tsx"
        ),
        // Other languages
        HighlightTestCase(
            name = "java extension",
            content = "String s = \"hello\";",
            extension = "java"
        ),
        HighlightTestCase(
            name = "py extension",
            content = "s = \"hello\"",
            extension = "py"
        ),
        HighlightTestCase(
            name = "json extension",
            content = "{\"key\": \"value\"}",
            extension = "json"
        ),
        // Markup languages
        HighlightTestCase(
            name = "xml extension",
            content = "<root><child /></root>",
            extension = "xml"
        ),
        HighlightTestCase(
            name = "html extension",
            content = "<html><body></body></html>",
            extension = "html"
        ),
        HighlightTestCase(
            name = "md extension",
            content = "# Title\n\nText",
            extension = "md"
        ),
        HighlightTestCase(
            name = "markdown extension",
            content = "# Title\n\nText",
            extension = "markdown"
        )
    )

    @Test
    fun fileExtensions_allSupported() {
        extensionTestCases.forEach { case ->
            val result = SyntaxHighlighter.highlight(case.content, case.extension)
            assertNotNull(result, "Result should not be null for: ${case.name}")
            assertEquals(case.expectedText, result.text, "Text mismatch for: ${case.name}")
        }
    }

    // ========================================
    // Case Sensitivity Tests
    // ========================================

    private val caseSensitivityTestCases = listOf(
        HighlightTestCase(
            name = "uppercase KT",
            content = "val x = 1",
            extension = "KT"
        ),
        HighlightTestCase(
            name = "mixed case Kt",
            content = "val x = 1",
            extension = "Kt"
        ),
        HighlightTestCase(
            name = "uppercase JSON",
            content = "{\"a\": 1}",
            extension = "JSON"
        ),
        HighlightTestCase(
            name = "uppercase PY",
            content = "x = 1",
            extension = "PY"
        )
    )

    @Test
    fun extensionCaseSensitivity_caseInsensitive() {
        caseSensitivityTestCases.forEach { case ->
            val result = SyntaxHighlighter.highlight(case.content, case.extension)
            assertNotNull(result, "Result should not be null for: ${case.name}")
            assertEquals(case.expectedText, result.text, "Text mismatch for: ${case.name}")
        }
    }
}
