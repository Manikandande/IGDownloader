package com.igdownloader.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = InstagramPink,
    secondary = InstagramPurple,
    tertiary = InstagramYellow,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = TextOnDark,
    onSecondary = TextOnDark,
    onBackground = TextOnDark,
    onSurface = TextOnDark,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextOnDarkSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = InstagramPink,
    secondary = InstagramPurple,
    tertiary = InstagramOrange,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = TextOnDark,
    onSecondary = TextOnDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextSecondary
)

@Composable
fun IGDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
