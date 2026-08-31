package com.yohanes.filereader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<FileEntity>)

    @Query("DELETE FROM files")
    suspend fun clearAll()

    @Query("SELECT * FROM files ORDER BY lastModified DESC")
    fun getAll(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    fun search(query: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE extension = :ext ORDER BY lastModified DESC")
    fun getByExtension(ext: String): Flow<List<FileEntity>>
}
