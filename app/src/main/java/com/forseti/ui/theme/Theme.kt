package com.forseti.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = ForsetiColors.RuneGold,
    onPrimary = ForsetiColors.SplashBlack,
    primaryContainer = ForsetiColors.RuneGoldDim,
    onPrimaryContainer = ForsetiColors.AshWhite,
    secondary = ForsetiColors.RavenBlue,
    onSecondary = ForsetiColors.SplashBlack,
    tertiary = ForsetiColors.MeadAmber,
    onTertiary = ForsetiColors.SplashBlack,
    background = ForsetiColors.Background,
    onBackground = ForsetiColors.AshWhite,
    surface = ForsetiColors.Surface,
    onSurface = ForsetiColors.AshWhite,
    surfaceVariant = ForsetiColors.SurfaceVariant,
    onSurfaceVariant = ForsetiColors.AshGrey,
    error = ForsetiColors.DeadlineRed,
    onError = ForsetiColors.AshWhite,
    outline = ForsetiColors.Stone,
    outlineVariant = ForsetiColors.SidebarSelected,
    scrim = Color(0xCC000000)
)

// A muted "daylight" variant that still leans dark-warm to keep brand consistency.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF6E5320),
    onPrimary = Color.White,
    secondary = Color(0xFF315A72),
    background = Color(0xFFF5EFE3),
    onBackground = Color(0xFF1A1C20),
    surface = Color(0xFFEDE6D6),
    onSurface = Color(0xFF1A1C20),
    error = Color(0xFF8C3A29)
)

@Composable
fun ForsetiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    forceDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = forceDark || darkTheme
    val colorScheme = if (isDark) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Edge-to-edge bar *colors* are owned by MainActivity.enableEdgeToEdge.
            // Here we only flip the appearance (light vs dark icons/text) so it
            // tracks theme changes from the Settings screen at runtime.
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ForsetiTypography,
        content = content
    )
}
