package com.yohanes.filereader.data

import java.io.RandomAccessFile

/**
 * Pembaca frame USLT (lirik tertanam) dari header ID3v2 mentah - Android
 * tidak punya API bawaan untuk ini, jadi diparse manual dari byte file.
 * Cuma teks polos, TANPA info waktu (beda dari file .lrc privat kita).
 */
object Id3UsltReader {

    fun readUslt(path: String): String? {
        return try {
            RandomAccessFile(path, "r").use { raf ->
                val header = ByteArray(10)
                if (raf.read(header) < 10) return null
                if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
                    return null
                }
                val majorVersion = header[3].toInt() and 0xFF
                val tagSize = synchsafeToInt(header[6], header[7], header[8], header[9])
                var bytesRead = 0

                while (bytesRead + 10 <= tagSize) {
                    val frameHeader = ByteArray(10)
                    val readCount = raf.read(frameHeader)
                    if (readCount < 10) break
                    bytesRead += 10

                    if (frameHeader[0] == 0.toByte()) break // sudah masuk area padding

                    val frameId = String(frameHeader, 0, 4, Charsets.ISO_8859_1)
                    val frameSize = if (majorVersion >= 4) {
                        synchsafeToInt(frameHeader[4], frameHeader[5], frameHeader[6], frameHeader[7])
                    } else {
                        ((frameHeader[4].toInt() and 0xFF) shl 24) or
                            ((frameHeader[5].toInt() and 0xFF) shl 16) or
                            ((frameHeader[6].toInt() and 0xFF) shl 8) or
                            (frameHeader[7].toInt() and 0xFF)
                    }
                    if (frameSize <= 0 || frameSize > tagSize) break

                    if (frameId == "USLT") {
                        val frameData = ByteArray(frameSize)
                        raf.readFully(frameData)
                        return parseUsltFrame(frameData)
                    } else {
                        raf.seek(raf.filePointer + frameSize)
                        bytesRead += frameSize
                    }
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun synchsafeToInt(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int {
        return ((b0.toInt() and 0x7F) shl 21) or
            ((b1.toInt() and 0x7F) shl 14) or
            ((b2.toInt() and 0x7F) shl 7) or
            (b3.toInt() and 0x7F)
    }

    private fun parseUsltFrame(data: ByteArray): String? {
        if (data.size < 5) return null
        val encodingByte = data[0].toInt() and 0xFF
        val offset = 4 // 1 byte encoding + 3 byte kode bahasa
        val isWide = encodingByte == 1 || encodingByte == 2
        val charset = when (encodingByte) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }

        var descEnd = offset
        if (isWide) {
            while (descEnd + 1 < data.size && !(data[descEnd] == 0.toByte() && data[descEnd + 1] == 0.toByte())) {
                descEnd += 2
            }
            descEnd += 2
        } else {
            while (descEnd < data.size && data[descEnd] != 0.toByte()) {
                descEnd++
            }
            descEnd += 1
        }
        if (descEnd >= data.size) return null

        val lyricsBytes = data.copyOfRange(descEnd, data.size)
        return String(lyricsBytes, charset).trim().takeIf { it.isNotBlank() }
    }
}
