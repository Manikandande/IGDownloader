package com.igdownloader.app.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DownloadProgress(
    val downloadId: Long,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val status: Int
) {
    val progress: Float
        get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f

    val isComplete: Boolean
        get() = status == DownloadManager.STATUS_SUCCESSFUL

    val isFailed: Boolean
        get() = status == DownloadManager.STATUS_FAILED
}

class MediaDownloadManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun downloadFile(
        url: String,
        fileName: String,
        subPath: String = "IGDownloader",
        mimeType: String = "video/mp4",
        title: String = "Instagram Media"
    ): Long {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(title)
            setDescription("Downloading $fileName")
            setMimeType(mimeType)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$subPath/$fileName")
            addRequestHeader("User-Agent",
                "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            addRequestHeader("Referer", "https://www.instagram.com/")
        }
        return downloadManager.enqueue(request)
    }

    // Observe download progress via a Flow
    fun observeDownload(downloadId: Long): Flow<DownloadProgress> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    val progress = queryProgress(downloadId)
                    trySend(progress)
                    if (progress.isComplete || progress.isFailed) {
                        close()
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Send initial state
        trySend(queryProgress(downloadId))

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    fun queryProgress(downloadId: Long): DownloadProgress {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        return if (cursor.moveToFirst()) {
            val bytesDownloaded = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            )
            val totalBytes = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            )
            val status = cursor.getInt(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            )
            cursor.close()
            DownloadProgress(downloadId, bytesDownloaded, totalBytes, status)
        } else {
            cursor.close()
            DownloadProgress(downloadId, 0, 0, DownloadManager.STATUS_FAILED)
        }
    }

    fun getDownloadedFilePath(downloadId: Long): String? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        return if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val path = if (status == DownloadManager.STATUS_SUCCESSFUL) {
                cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            } else null
            cursor.close()
            path
        } else {
            cursor.close()
            null
        }
    }

    fun cancelDownload(downloadId: Long) {
        downloadManager.remove(downloadId)
    }
}
