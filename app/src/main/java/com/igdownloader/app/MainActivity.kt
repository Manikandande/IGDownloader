package com.igdownloader.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.igdownloader.app.ui.screens.HistoryScreen
import com.igdownloader.app.ui.screens.MainScreen
import com.igdownloader.app.ui.theme.IGDownloaderTheme
import com.igdownloader.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

object Destinations {
    const val MAIN = "main"
    const val HISTORY = "history"
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle share intent (from Instagram "Share to..." flow)
        handleShareIntent(intent)

        setContent {
            IGDownloaderTheme {
                IGDownloaderApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND &&
            intent.type == "text/plain"
        ) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            // Extract URL from the shared text (Instagram share text may have extra copy)
            val urlRegex = Regex("https://www\\.instagram\\.com/[^\\s]+")
            val extractedUrl = urlRegex.find(sharedText ?: "")?.value ?: sharedText
            viewModel.handleSharedUrl(extractedUrl)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IGDownloaderApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Show snackbar messages from the ViewModel
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearSnackbar()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.MAIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destinations.MAIN) {
                MainScreen(
                    viewModel = viewModel,
                    onNavigateToHistory = { navController.navigate(Destinations.HISTORY) },
                    snackbarHostState = snackbarHostState
                )
            }
            composable(Destinations.HISTORY) {
                HistoryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
