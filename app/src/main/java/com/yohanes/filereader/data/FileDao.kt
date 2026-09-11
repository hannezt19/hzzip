package com.yohanes.filereader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<FileEntity>)

    @Query("DELETE FROM files")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(files: List<FileEntity>) {
        clearAll()
        insertAll(files)
    }

    @Query("SELECT * FROM files")
    suspend fun getAllOnce(): List<FileEntity>

    @Query("DELETE FROM files WHERE path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("DELETE FROM files WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("UPDATE files SET path = :newPath, name = :newName WHERE path = :oldPath")
    suspend fun renamePath(oldPath: String, newPath: String, newName: String)

    @Transaction
    suspend fun syncAll(newFiles: List<FileEntity>): List<String> {
        val existing = getAllOnce().associateBy { it.path }
        val newMap = newFiles.associateBy { it.path }
        val toUpsert = newFiles.filter { nf -> existing[nf.path] != nf }
        val toDelete = existing.keys - newMap.keys
        if (toUpsert.isNotEmpty()) insertAll(toUpsert)
        if (toDelete.isNotEmpty()) deleteByPaths(toDelete.toList())
        return toDelete.toList()
    }

    @Query("SELECT * FROM files ORDER BY lastModified DESC")
    fun getAll(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    fun search(query: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE extension = :ext ORDER BY lastModified DESC")
    fun getByExtension(ext: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE extension IN ('jpg','jpeg','png','webp','gif') ORDER BY lastModified DESC")
    fun getImagesPaged(): PagingSource<Int, FileEntity>

    @Query("SELECT * FROM files WHERE extension IN ('jpg','jpeg','png','webp','gif') ORDER BY lastModified DESC")
    fun getImages(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE extension IN ('mp4','mkv','webm','3gp','avi','mov') ORDER BY lastModified DESC")
    fun getVideos(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE extension IN ('mp3','wav','m4a','ogg','flac','aac') ORDER BY lastModified DESC")
    fun getAudios(): Flow<List<FileEntity>>

    // === Ditambahkan untuk accordion galeri Gambar (Tahun -> Bulan -> Tanggal) ===

    @Query("""
        SELECT strftime('%d', lastModified/1000, 'unixepoch', 'localtime') AS day, COUNT(*) AS count
        FROM files
        WHERE extension IN ('jpg','jpeg','png','webp','gif')
          AND strftime('%Y-%m', lastModified/1000, 'unixepoch', 'localtime') = :yearMonth
        GROUP BY day
        ORDER BY day DESC
    """)
    suspend fun countPhotosPerDayInMonth(yearMonth: String): List<DayCount>

    @Query("""
        SELECT strftime('%Y-%m', lastModified/1000, 'unixepoch', 'localtime') AS yearMonth, COUNT(*) AS count
        FROM files
        WHERE extension IN ('jpg','jpeg','png','webp','gif')
          AND strftime('%Y', lastModified/1000, 'unixepoch', 'localtime') = :year
          AND strftime('%Y-%m', lastModified/1000, 'unixepoch', 'localtime') != :currentYearMonth
        GROUP BY yearMonth
        ORDER BY yearMonth DESC
    """)
    suspend fun countPhotosPerMonthInYear(year: String, currentYearMonth: String): List<MonthCount>

    @Query("""
        SELECT strftime('%Y', lastModified/1000, 'unixepoch', 'localtime') AS year, COUNT(*) AS count
        FROM files
        WHERE extension IN ('jpg','jpeg','png','webp','gif')
          AND strftime('%Y', lastModified/1000, 'unixepoch', 'localtime') != :currentYear
        GROUP BY year
        ORDER BY year DESC
    """)
    suspend fun countPhotosPerYear(currentYear: String): List<YearCount>

    @Query("""
        SELECT * FROM files
        WHERE extension IN ('jpg','jpeg','png','webp','gif')
          AND strftime('%Y-%m', lastModified/1000, 'unixepoch', 'localtime') = :yearMonth
        ORDER BY lastModified DESC
    """)
    fun getImagesPagedForMonth(yearMonth: String): PagingSource<Int, FileEntity>

    @Query("""
        SELECT * FROM files
        WHERE extension IN ('jpg','jpeg','png','webp','gif')
          AND strftime('%Y-%m-%d', lastModified/1000, 'unixepoch', 'localtime') = :date
        ORDER BY lastModified DESC
    """)
    suspend fun getImagesForDate(date: String): List<FileEntity>
}

data class DayCount(val day: String, val count: Int)
data class MonthCount(val yearMonth: String, val count: Int)
data class YearCount(val year: String, val count: Int)
