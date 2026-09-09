package com.yohanes.filereader.data

data class LyricLine(val timeMs: Long, val text: String)

private val LRC_TIME_REGEX = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\]""")

object LyricsStore {

    data class LyricsResult(val lines: List<LyricLine>, val synced: Boolean)

    fun loadForSong(songPath: String): LyricsResult? {
        val usltText = Id3UsltReader.readUslt(songPath)
        if (usltText.isNullOrBlank()) return null

        val synced = parseLrc(usltText)
        if (synced.isNotEmpty()) return LyricsResult(synced, true)

        val plainLines = usltText.lines().filter { it.isNotBlank() }.map { LyricLine(0L, it.trim()) }
        if (plainLines.isEmpty()) return null
        return LyricsResult(plainLines, false)
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
}
