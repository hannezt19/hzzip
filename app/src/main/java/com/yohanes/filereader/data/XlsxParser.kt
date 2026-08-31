package com.yohanes.filereader.data

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class XlsxCell(
    var value: String = "",
    var formula: String? = null,
    var isNumeric: Boolean = false
)

data class XlsxSheet(
    val rows: MutableList<MutableList<XlsxCell>> = mutableListOf()
)

object XlsxParser {

    fun parse(context: Context, uri: Uri): XlsxSheet? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                parseZip(input)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseZip(input: InputStream): XlsxSheet {
        val sharedStrings = mutableListOf<String>()
        var sheetXmlBytes: ByteArray? = null

        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "xl/sharedStrings.xml" -> sharedStrings.addAll(parseSharedStrings(zip.readBytes()))
                    "xl/worksheets/sheet1.xml" -> sheetXmlBytes = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }

        val sheet = XlsxSheet()
        val bytes = sheetXmlBytes ?: return sheet
        parseSheetXml(bytes, sharedStrings, sheet)
        return sheet
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var eventType = parser.eventType
        var currentText = StringBuilder()
        var insideSi = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") {
                        insideSi = true
                        currentText = StringBuilder()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideSi) currentText.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "si") {
                        strings.add(currentText.toString())
                        insideSi = false
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseSheetXml(bytes: ByteArray, sharedStrings: List<String>, sheet: XlsxSheet) {
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var eventType = parser.eventType
        var currentRow: MutableList<XlsxCell>? = null
        var currentCell: XlsxCell? = null
        var currentCellType: String? = null
        var currentColIndex = -1
        var readingValue = false
        var readingFormula = false
        var textBuffer = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> currentRow = mutableListOf()
                        "c" -> {
                            currentCellType = parser.getAttributeValue(null, "t")
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            currentColIndex = XlsxParser.colIndexFromRef(ref)
                            currentCell = XlsxCell()
                        }
                        "v" -> {
                            readingValue = true
                            textBuffer = StringBuilder()
                        }
                        "f" -> {
                            readingFormula = true
                            textBuffer = StringBuilder()
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (readingValue || readingFormula) textBuffer.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> {
                            readingValue = false
                            val raw = textBuffer.toString()
                            currentCell?.let { cell ->
                                if (currentCellType == "s") {
                                    val idx = raw.toIntOrNull()
                                    cell.value = if (idx != null && idx < sharedStrings.size) sharedStrings[idx] else ""
                                    cell.isNumeric = false
                                } else {
                                    cell.value = raw
                                    cell.isNumeric = raw.toDoubleOrNull() != null
                                }
                            }
                        }
                        "f" -> {
                            readingFormula = false
                            currentCell?.formula = textBuffer.toString()
                        }
                        "c" -> {
                            currentRow?.let { row ->
                                while (row.size < currentColIndex) row.add(XlsxCell())
                                if (currentColIndex >= 0) {
                                    if (row.size == currentColIndex) row.add(currentCell ?: XlsxCell())
                                    else if (currentColIndex < row.size) row[currentColIndex] = currentCell ?: XlsxCell()
                                }
                            }
                            currentCell = null
                        }
                        "row" -> {
                            currentRow?.let { sheet.rows.add(it) }
                            currentRow = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    fun colIndexFromRef(ref: String): Int {
        var col = 0
        for (c in ref) {
            if (c.isLetter()) {
                col = col * 26 + (c.uppercaseChar() - 'A' + 1)
            } else break
        }
        return col - 1
    }
}
