package com.yohanes.filereader.data

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object XlsxWriter {

    fun save(context: Context, uri: Uri, sheet: XlsxSheet): Boolean {
        return try {
            val resolver = context.contentResolver
            val originalBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return false

            val sharedStrings = mutableListOf<String>()
            val stringIndex = HashMap<String, Int>()
            fun indexOf(value: String): Int {
                return stringIndex.getOrPut(value) {
                    sharedStrings.add(value)
                    sharedStrings.size - 1
                }
            }

            val sheetXml = buildSheetXml(sheet, ::indexOf)
            val sharedStringsXml = buildSharedStringsXml(sharedStrings)

            val outBytes = ByteArrayOutputStream()
            ZipOutputStream(outBytes).use { zos ->
                ZipInputStream(originalBytes.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        zos.putNextEntry(ZipEntry(name))
                        when (name) {
                            "xl/worksheets/sheet1.xml" -> zos.write(sheetXml.toByteArray(Charsets.UTF_8))
                            "xl/sharedStrings.xml" -> zos.write(sharedStringsXml.toByteArray(Charsets.UTF_8))
                            else -> zis.copyTo(zos)
                        }
                        zos.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            resolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(outBytes.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun colLetter(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.insert(0, ('A' + rem))
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun buildSheetXml(sheet: XlsxSheet, indexOf: (String) -> Int): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        sb.append("<sheetData>")
        sheet.rows.forEachIndexed { rowIdx, row ->
            sb.append("<row r=\"${rowIdx + 1}\">")
            row.forEachIndexed { colIdx, cell ->
                val ref = "${colLetter(colIdx)}${rowIdx + 1}"
                when {
                    cell.formula != null -> {
                        sb.append("<c r=\"$ref\">")
                        sb.append("<f>${escapeXml(cell.formula!!)}</f>")
                        if (cell.value.isNotEmpty()) sb.append("<v>${escapeXml(cell.value)}</v>")
                        sb.append("</c>")
                    }
                    cell.isNumeric && cell.value.isNotEmpty() -> {
                        sb.append("<c r=\"$ref\"><v>${escapeXml(cell.value)}</v></c>")
                    }
                    cell.value.isNotEmpty() -> {
                        val idx = indexOf(cell.value)
                        sb.append("<c r=\"$ref\" t=\"s\"><v>$idx</v></c>")
                    }
                    else -> {
                        sb.append("<c r=\"$ref\"/>")
                    }
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun buildSharedStringsXml(strings: List<String>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"${strings.size}\" uniqueCount=\"${strings.size}\">")
        strings.forEach { s ->
            sb.append("<si><t xml:space=\"preserve\">${escapeXml(s)}</t></si>")
        }
        sb.append("</sst>")
        return sb.toString()
    }
}
