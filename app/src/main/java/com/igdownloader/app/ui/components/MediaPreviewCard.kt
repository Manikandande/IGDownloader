package com.igdownloader.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.igdownloader.app.data.repository.DownloadState
import com.igdownloader.app.domain.model.InstagramMedia

@Composable
fun MediaPreviewCard(
    media: InstagramMedia,
    downloadState: DownloadState,
    onDownloadVideo: () -> Unit,
    onDownloadThumbnail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (media.thumbnailUrl != null) {
                    AsyncImage(
                        model = media.thumbnailUrl,
                        contentDescription = "Post thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Video badge overlay
                if (media.isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Video",
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Caption
            if (!media.caption.isNullOrBlank()) {
                Text(
                    text = media.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
            }

            // Download progress
            if (downloadState is DownloadState.Downloading) {
                Column {
                    LinearProgressIndicator(
                        progress = { downloadState.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${(downloadState.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            val isDownloading = downloadState is DownloadState.Downloading
            val isCompleted = downloadState is DownloadState.Completed

            // Action buttons
            if (media.isVideo) {
                GradientButton(
                    text = when {
                        isCompleted -> "Downloaded!"
                        isDownloading -> "Downloading..."
                        else -> "Download Video"
                    },
                    onClick = onDownloadVideo,
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isDownloading,
                    enabled = !isDownloading && !isCompleted
                )
                Spacer(Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = onDownloadThumbnail,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDownloading,
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Download Thumbnail")
            }
        }
    }
}
