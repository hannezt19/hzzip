package com.yohanes.filereader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey val path: String,
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    val lastModified: Long
)
