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

    @Transaction
    suspend fun syncAll(newFiles: List<FileEntity>) {
        val existing = getAllOnce().associateBy { it.path }
        val newMap = newFiles.associateBy { it.path }

        // hanya file yang benar-benar baru atau berubah (beda size/lastModified) yang ditulis ulang
        val toUpsert = newFiles.filter { nf -> existing[nf.path] != nf }
        // file yang sudah tidak ada lagi di storage dihapus dari tabel
        val toDelete = existing.keys - newMap.keys

        if (toUpsert.isNotEmpty()) insertAll(toUpsert)
        if (toDelete.isNotEmpty()) deleteByPaths(toDelete.toList())
    }

    @Query("SELECT * FROM files ORDER BY lastModified DESC")
    fun getAll(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    fun search(query: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE extension = :ext ORDER BY lastModified DESC")
    fun getByExtension(ext: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE extension IN ('jpg','jpeg','png','webp','gif') ORDER BY lastModified DESC")
    fun getImagesPaged(): PagingSource<Int, FileEntity>

    @Query("SELECT * FROM files WHERE extension IN ('mp4','mkv','webm','3gp','avi','mov') ORDER BY lastModified DESC")
    fun getVideos(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE extension IN ('mp3','wav','m4a','ogg','flac','aac') ORDER BY lastModified DESC")
    fun getAudios(): Flow<List<FileEntity>>
}
