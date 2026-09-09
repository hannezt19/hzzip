package com.yohanes.filereader.data

import android.content.Context
import java.io.File
import java.security.MessageDigest

data class LyricLine(val timeMs: Long, val text: String)

private val LRC_TIME_REGEX = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\]""")

/**
 * Penyimpanan lirik PRIVAT khusus app (bukan folder publik) - tidak ikut
 * ter-scan FileScanner/Beranda, tidak berisiko terhapus app pembersih.
 * Dikunci pakai hash dari path lagu (bukan nama file) supaya tidak ketuker
 * walau ada nama lagu kembar di folder berbeda.
 */
object LyricsStore {

    private fun hashPath(songPath: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(songPath.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun lyricsDir(context: Context): File {
        val dir = File(context.filesDir, "lyrics")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun lyricsFile(context: Context, songPath: String): File {
        return File(lyricsDir(context), hashPath(songPath) + ".lrc")
    }

    fun hasLyrics(context: Context, songPath: String): Boolean {
        return lyricsFile(context, songPath).exists()
    }

    fun readLyrics(context: Context, songPath: String): List<LyricLine>? {
        val file = lyricsFile(context, songPath)
        if (!file.exists()) return null
        return try {
            parseLrc(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun saveLyrics(context: Context, songPath: String, lines: List<LyricLine>) {
        val file = lyricsFile(context, songPath)
        file.writeText(formatLrc(lines))
    }

    fun deleteLyrics(context: Context, songPath: String) {
        val file = lyricsFile(context, songPath)
        if (file.exists()) file.delete()
    }

    /** Dipanggil dari siklus sync scan Beranda - bersihkan lirik lagu yang sudah hilang dari HP. */
    fun cleanupRemoved(context: Context, removedPaths: Collection<String>) {
        removedPaths.forEach { deleteLyrics(context, it) }
    }

    fun parseLrc(content: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        content.lines().forEach { rawLine ->
            val matches = LRC_TIME_REGEX.findAll(rawLine).toList()
            if (matches.isEmpty()) return@forEach
            val text = rawLine.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val fracStr = m.groupValues[3]
                val frac = if (fracStr.length == 2) fracStr.toLong() * 10 else fracStr.toLong()
                val timeMs = min * 60_000 + sec * 1000 + frac
                lines.add(LyricLine(timeMs, text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    data class LyricsResult(val lines: List<LyricLine>, val synced: Boolean)

    /** Cari lirik: coba file .lrc privat dulu (tersinkron), baru fallback ke tag USLT (polos, tanpa highlight). */
    fun loadForSong(context: Context, songPath: String): LyricsResult? {
        val synced = readLyrics(context, songPath)
        if (!synced.isNullOrEmpty()) return LyricsResult(synced, true)

        val usltText = Id3UsltReader.readUslt(songPath)
        if (!usltText.isNullOrBlank()) {
            val lines = usltText.lines().filter { it.isNotBlank() }.map { LyricLine(0L, it.trim()) }
            if (lines.isNotEmpty()) return LyricsResult(lines, false)
        }
        return null
    }

    fun formatLrc(lines: List<LyricLine>): String {
        return lines.sortedBy { it.timeMs }.joinToString("\n") { line ->
            val totalMs = line.timeMs
            val minutes = totalMs / 60000
            val seconds = (totalMs % 60000) / 1000
            val centis = (totalMs % 1000) / 10
            "[%02d:%02d.%02d] %s".format(minutes, seconds, centis, line.text)
        }
    }
}
