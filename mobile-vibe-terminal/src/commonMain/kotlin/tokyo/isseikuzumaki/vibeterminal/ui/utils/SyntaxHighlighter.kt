package tokyo.isseikuzumaki.vibeterminal.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

object SyntaxHighlighter {
    // Color scheme (GitHub Dark theme inspired)
    private val KEYWORD_COLOR = Color(0xFFFF7B72)     // Red
    private val STRING_COLOR = Color(0xFFA5D6FF)      // Light blue
    private val COMMENT_COLOR = Color(0xFF8B949E)     // Gray
    private val FUNCTION_COLOR = Color(0xFFD2A8FF)    // Purple
    private val NUMBER_COLOR = Color(0xFF79C0FF)      // Blue
    private val DEFAULT_COLOR = Color(0xFFE6EDF3)     // White

    // Keywords for various languages
    private val KOTLIN_KEYWORDS = setOf(
        "package", "import", "class", "interface", "fun", "val", "var",
        "if", "else", "when", "for", "while", "do", "return", "break",
        "continue", "object", "companion", "data", "sealed", "enum",
        "try", "catch", "finally", "throw", "public", "private", "protected",
        "internal", "override", "abstract", "open", "final", "suspend",
        "inline", "typealias", "this", "super", "null", "true", "false",
        "is", "in", "as", "by"
    )

    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch",
        "char", "class", "const", "continue", "default", "do", "double",
        "else", "enum", "extends", "final", "finally", "float", "for",
        "goto", "if", "implements", "import", "instanceof", "int", "interface",
        "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch",
        "synchronized", "this", "throw", "throws", "transient", "try",
        "void", "volatile", "while", "true", "false", "null"
    )

    private val PYTHON_KEYWORDS = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await",
        "break", "class", "continue", "def", "del", "elif", "else", "except",
        "finally", "for", "from", "global", "if", "import", "in", "is",
        "lambda", "nonlocal", "not", "or", "pass", "raise", "return",
        "try", "while", "with", "yield"
    )

    fun highlight(content: String, extension: String): AnnotatedString {
        return when (extension.lowercase()) {
            "kt", "kts" -> highlightKotlin(content)
            "java" -> highlightJava(content)
            "py" -> highlightPython(content)
            "js", "ts", "jsx", "tsx" -> highlightJavaScript(content)
            "json" -> highlightJson(content)
            "xml", "html" -> highlightXml(content)
            "md", "markdown" -> highlightMarkdown(content)
            else -> buildAnnotatedString {
                append(content)
                addStyle(SpanStyle(color = DEFAULT_COLOR), 0, content.length)
            }
        }
    }

    private fun highlightKotlin(content: String): AnnotatedString {
        return highlightWithKeywords(content, KOTLIN_KEYWORDS)
    }

    private fun highlightJava(content: String): AnnotatedString {
        return highlightWithKeywords(content, JAVA_KEYWORDS)
    }

    private fun highlightPython(content: String): AnnotatedString {
        return highlightWithKeywords(content, PYTHON_KEYWORDS)
    }

    private fun highlightJavaScript(content: String): AnnotatedString {
        val keywords = setOf(
            "break", "case", "catch", "class", "const", "continue", "debugger",
            "default", "delete", "do", "else", "export", "extends", "finally",
            "for", "function", "if", "import", "in", "instanceof", "let", "new",
            "return", "super", "switch", "this", "throw", "try", "typeof",
            "var", "void", "while", "with", "yield", "async", "await", "true",
            "false", "null", "undefined"
        )
        return highlightWithKeywords(content, keywords)
    }

    private fun highlightJson(content: String): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < content.length) {
                when {
                    content[i] == '"' -> {
                        // String
                        val start = i
                        i++
                        while (i < content.length && content[i] != '"') {
                            if (content[i] == '\\' && i + 1 < content.length) i++
                            i++
                        }
                        if (i < content.length) i++ // consume closing quote if present
                        append(content.substring(start, i))
                        addStyle(SpanStyle(color = STRING_COLOR), start, i)
                    }
                    content[i].isDigit() || (content[i] == '-' && i + 1 < content.length && content[i + 1].isDigit()) -> {
                        // Number
                        val start = i
                        if (content[i] == '-') i++
                        while (i < content.length && (content[i].isDigit() || content[i] == '.')) i++
                        append(content.substring(start, i))
                        addStyle(SpanStyle(color = NUMBER_COLOR), start, i)
                    }
                    else -> {
                        append(content[i])
                        addStyle(SpanStyle(color = DEFAULT_COLOR), i, i + 1)
                        i++
                    }
                }
            }
        }
    }

    private fun highlightXml(content: String): AnnotatedString {
        return buildAnnotatedString {
            append(content)
            addStyle(SpanStyle(color = DEFAULT_COLOR), 0, content.length)
        }
    }

    private fun highlightMarkdown(content: String): AnnotatedString {
        return buildAnnotatedString {
            append(content)
            addStyle(SpanStyle(color = DEFAULT_COLOR), 0, content.length)
        }
    }

    private fun highlightWithKeywords(content: String, keywords: Set<String>): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < content.length) {
                when {
                    // Single-line comment
                    content.startsWith("//", i) -> {
                        val start = i
                        while (i < content.length && content[i] != '\n') i++
                        append(content.substring(start, i))
                        addStyle(SpanStyle(color = COMMENT_COLOR), start, i)
                    }
                    // Multi-line comment
                    content.startsWith("/*", i) -> {
                        val start = i
                        i += 2
                        while (i < content.length - 1 && !content.startsWith("*/", i)) i++
                        if (i < content.length - 1) i += 2
                        append(content.substring(start, i))
                        addStyle(SpanStyle(color = COMMENT_COLOR), start, i)
                    }
                    // String (double quotes)
                    content[i] == '"' -> {
                        val start = i
                        i++
                        while (i < content.length && content[i] != '"') {
                            if (content[i] == '\\' && i + 1 < content.length) i++
                            i++
                        }
                        if (i < content.length) i++ // consume closing quote if present
                        append(content.substring(start, i))
                        addStyle(SpanStyle(color = STRING_COLOR), start, i)
                    }
                    // String (single quotes)
                    content[i] == '\'' -> {
                        val start = i
                        i++
                        while (i < content.length && content[i] != '\'') {
                            if (content[i] == '\\' && i + 1 < content.length) i++
                            i++
                        }
                        if (i < content.length) i++ // consume closing quote if present
                        append(content.substring(start, i))
                        addStyle(SpanStyle(color = STRING_COLOR), start, i)
                    }
                    // Number
                    content[i].isDigit() -> {
                        val start = i
                        while (i < content.length && (content[i].isDigit() || content[i] == '.' || content[i] == 'f' || content[i] == 'L')) i++
                        append(content.substring(start, i))
                        addStyle(SpanStyle(color = NUMBER_COLOR), start, i)
                    }
                    // Identifier or keyword
                    content[i].isLetter() || content[i] == '_' -> {
                        val start = i
                        while (i < content.length && (content[i].isLetterOrDigit() || content[i] == '_')) i++
                        val word = content.substring(start, i)
                        append(word)
                        if (keywords.contains(word)) {
                            addStyle(SpanStyle(color = KEYWORD_COLOR), start, i)
                        } else {
                            addStyle(SpanStyle(color = DEFAULT_COLOR), start, i)
                        }
                    }
                    // Default
                    else -> {
                        append(content[i])
                        addStyle(SpanStyle(color = DEFAULT_COLOR), i, i + 1)
                        i++
                    }
                }
            }
        }
    }
}
