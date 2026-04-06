package com.igdownloader.app.data.repository

import android.app.DownloadManager
import android.content.Context
import com.igdownloader.app.data.local.DownloadDao
import com.igdownloader.app.data.local.DownloadDatabase
import com.igdownloader.app.data.local.DownloadEntity
import com.igdownloader.app.data.local.DownloadStatus
import com.igdownloader.app.domain.model.InstagramMedia
import com.igdownloader.app.domain.model.MediaType
import com.igdownloader.app.download.MediaDownloadManager
import com.igdownloader.app.network.InstagramScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

sealed class FetchState {
    object Loading : FetchState()
    data class Success(val media: InstagramMedia) : FetchState()
    data class Error(val message: String) : FetchState()
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float, val downloadId: Long) : DownloadState()
    data class Completed(val filePath: String?) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}

class DownloadRepository(context: Context) {

    private val scraper = InstagramScraper()
    private val downloadManager = MediaDownloadManager(context)
    private val dao: DownloadDao = DownloadDatabase.getInstance(context).downloadDao()

    fun fetchInstagramMedia(url: String): Flow<FetchState> = flow {
        emit(FetchState.Loading)
        val result = scraper.fetchMediaInfo(url)
        result.fold(
            onSuccess = { emit(FetchState.Success(it)) },
            onFailure = { emit(FetchState.Error(it.message ?: "Unknown error")) }
        )
    }.flowOn(Dispatchers.IO)

    fun startDownload(media: InstagramMedia, type: MediaType): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)

        val (downloadUrl, mimeType, extension) = when (type) {
            MediaType.VIDEO -> Triple(media.videoUrl, "video/mp4", "mp4")
            MediaType.IMAGE -> Triple(media.thumbnailUrl, "image/jpeg", "jpg")
        }

        if (downloadUrl == null) {
            emit(DownloadState.Failed("No ${type.name.lowercase()} URL available for this post."))
            return@flow
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "ig_${media.shortcode}_$timestamp.$extension"

        // Insert pending record in DB
        val entity = DownloadEntity(
            postUrl = media.postUrl,
            shortcode = media.shortcode,
            thumbnailUrl = media.thumbnailUrl,
            caption = media.caption,
            localVideoPath = null,
            localThumbnailPath = null,
            status = DownloadStatus.PENDING,
            mediaType = type.name
        )
        val dbId = dao.insertDownload(entity)

        val downloadId = downloadManager.downloadFile(
            url = downloadUrl,
            fileName = fileName,
            mimeType = mimeType,
            title = "Instagram ${type.name.lowercase().replaceFirstChar { it.uppercase() }}"
        )

        dao.updateDownloadStatus(dbId, DownloadStatus.DOWNLOADING, null)

        // Poll progress
        var lastProgress = 0f
        var isFinished = false
        while (!isFinished) {
            kotlinx.coroutines.delay(500)
            val progress = downloadManager.queryProgress(downloadId)
            if (progress.progress != lastProgress) {
                lastProgress = progress.progress
                emit(DownloadState.Downloading(progress.progress, downloadId))
            }
            when {
                progress.isComplete -> {
                    val filePath = downloadManager.getDownloadedFilePath(downloadId)
                    dao.updateDownloadStatus(dbId, DownloadStatus.COMPLETED, filePath)
                    emit(DownloadState.Completed(filePath))
                    isFinished = true
                }
                progress.isFailed -> {
                    dao.updateDownloadStatus(dbId, DownloadStatus.FAILED, null)
                    emit(DownloadState.Failed("Download failed. Please try again."))
                    isFinished = true
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getAllDownloads(): Flow<List<DownloadEntity>> = dao.getAllDownloads()

    suspend fun deleteDownload(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteDownloadById(id)
    }
}
