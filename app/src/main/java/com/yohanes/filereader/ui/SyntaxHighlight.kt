package com.yohanes.filereader.ui

import com.yohanes.filereader.FileType

data class HighlightSpan(val start: Int, val end: Int, val color: Int)

private const val COLOR_KEYWORD = 0xFF569CD6.toInt()
private const val COLOR_STRING = 0xFFCE9178.toInt()
private const val COLOR_COMMENT = 0xFF6A9955.toInt()
private const val COLOR_NUMBER = 0xFFB5CEA8.toInt()
private const val COLOR_TAG = 0xFF569CD6.toInt()
private const val COLOR_ATTR = 0xFF9CDCFE.toInt()

private val JS_KEYWORDS = listOf(
    "const", "let", "var", "function", "return", "if", "else", "for", "while",
    "class", "new", "this", "import", "export", "default", "from", "async",
    "await", "try", "catch", "true", "false", "null", "undefined"
)

fun computeHighlightSpans(text: String, type: FileType, offset: Int = 0): List<HighlightSpan> {
    val rules: List<Pair<Regex, Int>> = when (type) {
        FileType.JSON -> listOf(
            Regex("\"(?:\\\\.|[^\"\\\\])*\"") to COLOR_STRING,
            Regex("-?\\b\\d+\\.?\\d*\\b") to COLOR_NUMBER
        )
        FileType.JS -> listOf(
            Regex("//[^\n]*|/\\*[\\s\\S]*?\\*/") to COLOR_COMMENT,
            Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`") to COLOR_STRING,
            Regex("\\b\\d+\\.?\\d*\\b") to COLOR_NUMBER,
            Regex("\\b(${JS_KEYWORDS.joinToString("|")})\\b") to COLOR_KEYWORD
        )
        FileType.HTML -> listOf(
            Regex("<!--[\\s\\S]*?-->") to COLOR_COMMENT,
            Regex("</?[a-zA-Z][a-zA-Z0-9-]*") to COLOR_TAG,
            Regex("\"[^\"]*\"|'[^']*'") to COLOR_STRING,
            Regex("[a-zA-Z-]+(?==)") to COLOR_ATTR
        )
        else -> emptyList()
    }
    if (rules.isEmpty() || text.isEmpty()) return emptyList()

    val colorAt = arrayOfNulls<Int>(text.length)
    for ((regex, color) in rules) {
        for (match in regex.findAll(text)) {
            for (i in match.range) {
                if (i < colorAt.size && colorAt[i] == null) colorAt[i] = color
            }
        }
    }

    val spans = mutableListOf<HighlightSpan>()
    var i = 0
    while (i < text.length) {
        val color = colorAt[i]
        if (color == null) { i++; continue }
        var j = i
        while (j < text.length && colorAt[j] == color) j++
        spans.add(HighlightSpan(offset + i, offset + j, color))
        i = j
    }
    return spans
}
