package com.igdownloader.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.igdownloader.app.data.repository.DownloadState
import com.igdownloader.app.data.repository.FetchState
import com.igdownloader.app.ui.components.GradientButton
import com.igdownloader.app.ui.components.MediaPreviewCard
import com.igdownloader.app.ui.theme.InstagramOrange
import com.igdownloader.app.ui.theme.InstagramPink
import com.igdownloader.app.ui.theme.InstagramPurple
import com.igdownloader.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToHistory: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(InstagramPurple, InstagramPink, InstagramOrange)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "IG Downloader",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            text = "Save Instagram videos & photos",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Download history",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // URL Input
            OutlinedTextField(
                value = uiState.inputUrl,
                onValueChange = viewModel::onUrlChanged,
                placeholder = { Text("Paste Instagram link here...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        val text = clipboard.getText()?.text
                        if (!text.isNullOrBlank()) viewModel.onUrlChanged(text)
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste from clipboard"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    keyboard?.hide()
                    viewModel.fetchMedia()
                })
            )

            // Fetch button
            GradientButton(
                text = "Fetch Media",
                onClick = {
                    keyboard?.hide()
                    viewModel.fetchMedia()
                },
                modifier = Modifier.fillMaxWidth(),
                isLoading = uiState.fetchState is FetchState.Loading
            )

            // Error message
            if (uiState.fetchState is FetchState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = (uiState.fetchState as FetchState.Error).message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Media preview + download
            if (uiState.fetchState is FetchState.Success) {
                val media = (uiState.fetchState as FetchState.Success).media
                MediaPreviewCard(
                    media = media,
                    downloadState = uiState.downloadState,
                    onDownloadVideo = { viewModel.downloadVideo(media) },
                    onDownloadThumbnail = { viewModel.downloadThumbnail(media) }
                )
            }

            // Instructions when nothing is fetched yet
            if (uiState.fetchState == null) {
                InstructionsCard()
            }
        }
    }
}

@Composable
private fun InstructionsCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "How to use",
                style = MaterialTheme.typography.titleLarge
            )
            InstructionStep("1", "Open Instagram and find a video or reel")
            InstructionStep("2", "Tap Share → Copy Link")
            InstructionStep("3", "Paste the link above and tap Fetch Media")
            InstructionStep("4", "Tap Download Video or Download Thumbnail")
            Spacer(Modifier.height(4.dp))
            Text(
                text = "You can also share the link directly from Instagram to this app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    Brush.radialGradient(
                        listOf(InstagramPink, InstagramPurple)
                    ),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
