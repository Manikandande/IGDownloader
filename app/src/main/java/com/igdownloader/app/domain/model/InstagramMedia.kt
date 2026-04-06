package com.igdownloader.app.domain.model

data class InstagramMedia(
    val shortcode: String,
    val videoUrl: String?,
    val thumbnailUrl: String?,
    val caption: String?,
    val isVideo: Boolean,
    val postUrl: String
)

enum class MediaType {
    VIDEO, IMAGE
}

data class DownloadRequest(
    val url: String,
    val media: InstagramMedia,
    val type: MediaType
)
