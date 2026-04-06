package com.igdownloader.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val postUrl: String,
    val shortcode: String,
    val thumbnailUrl: String?,
    val caption: String?,
    val localVideoPath: String?,
    val localThumbnailPath: String?,
    val status: DownloadStatus,
    val mediaType: String,      // "VIDEO" or "IMAGE"
    val downloadedAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}
