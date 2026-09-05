package com.yohanes.filereader.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import com.yohanes.filereader.FileType

private val COLOR_KEYWORD = Color(0xFF569CD6)
private val COLOR_STRING = Color(0xFFCE9178)
private val COLOR_COMMENT = Color(0xFF6A9955)
private val COLOR_NUMBER = Color(0xFFB5CEA8)
private val COLOR_TAG = Color(0xFF569CD6)
private val COLOR_ATTR = Color(0xFF9CDCFE)

private const val MAX_HIGHLIGHT_LENGTH = 150_000

private val JS_KEYWORDS = listOf(
    "const", "let", "var", "function", "return", "if", "else", "for", "while",
    "class", "new", "this", "import", "export", "default", "from", "async",
    "await", "try", "catch", "true", "false", "null", "undefined"
)

/**
 * Highlighter ringan berbasis regex. Cukup untuk file kecil-menengah tanpa
 * perlu library parser berat yang bisa memberatkan device RAM kecil.
 * Untuk file besar (di atas MAX_HIGHLIGHT_LENGTH), highlighting dilewati
 * supaya editor tetap responsif - teks tetap tampil apa adanya.
 */
fun highlight(text: String, type: FileType): AnnotatedString {
    if (text.length > MAX_HIGHLIGHT_LENGTH) return AnnotatedString(text)
    return when (type) {
        FileType.JSON -> highlightJson(text)
        FileType.JS -> highlightJs(text)
        FileType.HTML -> highlightHtml(text)
        else -> AnnotatedString(text)
    }
}

private fun highlightJson(text: String): AnnotatedString {
    val stringRegex = Regex("\"(?:\\\\.|[^\"\\\\])*\"")
    val numberRegex = Regex("-?\\b\\d+\\.?\\d*\\b")
    return buildAnnotated(text, listOf(
        stringRegex to COLOR_STRING,
        numberRegex to COLOR_NUMBER
    ))
}

private fun highlightJs(text: String): AnnotatedString {
    val stringRegex = Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`")
    val commentRegex = Regex("//[^\n]*|/\\*[\\s\\S]*?\\*/")
    val numberRegex = Regex("\\b\\d+\\.?\\d*\\b")
    val keywordRegex = Regex("\\b(${JS_KEYWORDS.joinToString("|")})\\b")
    return buildAnnotated(text, listOf(
        commentRegex to COLOR_COMMENT,
        stringRegex to COLOR_STRING,
        numberRegex to COLOR_NUMBER,
        keywordRegex to COLOR_KEYWORD
    ))
}

private fun highlightHtml(text: String): AnnotatedString {
    val tagRegex = Regex("</?[a-zA-Z][a-zA-Z0-9-]*")
    val attrRegex = Regex("[a-zA-Z-]+(?==)")
    val stringRegex = Regex("\"[^\"]*\"|'[^']*'")
    val commentRegex = Regex("<!--[\\s\\S]*?-->")
    return buildAnnotated(text, listOf(
        commentRegex to COLOR_COMMENT,
        tagRegex to COLOR_TAG,
        stringRegex to COLOR_STRING,
        attrRegex to COLOR_ATTR
    ))
}

/**
 * Menggabungkan beberapa aturan regex jadi satu AnnotatedString.
 * Aturan yang lebih dulu di list punya prioritas lebih tinggi kalau tumpang tindih.
 */
private fun buildAnnotated(text: String, rules: List<Pair<Regex, Color>>): AnnotatedString {
    // index -> warna, dipakai untuk menghindari overlap antar rule
    val colorAt = arrayOfNulls<Color>(text.length)

    for ((regex, color) in rules) {
        for (match in regex.findAll(text)) {
            for (i in match.range) {
                if (i < colorAt.size && colorAt[i] == null) {
                    colorAt[i] = color
                }
            }
        }
    }

    return AnnotatedString.Builder(text).apply {
        var i = 0
        while (i < text.length) {
            val color = colorAt[i]
            if (color == null) {
                i++
                continue
            }
            var j = i
            while (j < text.length && colorAt[j] == color) j++
            addStyle(SpanStyle(color = color), i, j)
            i = j
        }
    }.toAnnotatedString()
}
