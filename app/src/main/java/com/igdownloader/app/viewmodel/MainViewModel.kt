package com.igdownloader.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.igdownloader.app.data.local.DownloadEntity
import com.igdownloader.app.data.repository.DownloadRepository
import com.igdownloader.app.data.repository.DownloadState
import com.igdownloader.app.data.repository.FetchState
import com.igdownloader.app.domain.model.InstagramMedia
import com.igdownloader.app.domain.model.MediaType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val inputUrl: String = "",
    val fetchState: FetchState? = null,
    val downloadState: DownloadState = DownloadState.Idle,
    val snackbarMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val downloadHistory: StateFlow<List<DownloadEntity>> = repository
        .getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(inputUrl = url, fetchState = null) }
    }

    fun fetchMedia() {
        val url = _uiState.value.inputUrl.trim()
        if (url.isBlank()) {
            showSnackbar("Please paste an Instagram URL first.")
            return
        }
        viewModelScope.launch {
            repository.fetchInstagramMedia(url)
                .collect { state ->
                    _uiState.update { it.copy(fetchState = state) }
                }
        }
    }

    fun downloadVideo(media: InstagramMedia) = startDownload(media, MediaType.VIDEO)

    fun downloadThumbnail(media: InstagramMedia) = startDownload(media, MediaType.IMAGE)

    private fun startDownload(media: InstagramMedia, type: MediaType) {
        viewModelScope.launch {
            repository.startDownload(media, type)
                .collect { state ->
                    _uiState.update { it.copy(downloadState = state) }
                    if (state is DownloadState.Completed) {
                        showSnackbar("Download complete! Saved to Downloads/IGDownloader")
                    } else if (state is DownloadState.Failed) {
                        showSnackbar(state.message)
                    }
                }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteDownload(id)
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun resetDownloadState() {
        _uiState.update { it.copy(downloadState = DownloadState.Idle) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    // Called when the app is opened via a share intent from Instagram
    fun handleSharedUrl(url: String?) {
        if (!url.isNullOrBlank()) {
            _uiState.update { it.copy(inputUrl = url) }
            fetchMedia()
        }
    }
}
