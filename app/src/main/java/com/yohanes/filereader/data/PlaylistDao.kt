package com.yohanes.filereader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist_items ORDER BY position ASC")
    fun getAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT path FROM playlist_items")
    fun getAllPaths(): Flow<List<String>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_items")
    suspend fun getMaxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: PlaylistEntity)

    @Transaction
    suspend fun addToEnd(path: String) {
        val nextPosition = getMaxPosition() + 1
        insert(PlaylistEntity(path = path, position = nextPosition))
    }

    @Query("DELETE FROM playlist_items WHERE path = :path")
    suspend fun removeByPath(path: String)

    @Query("DELETE FROM playlist_items")
    suspend fun clearAll()
}
