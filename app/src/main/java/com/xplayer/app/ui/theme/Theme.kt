package com.xplayer.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.xplayer.app.data.datastore.DarkModeTheme

@Composable
fun XPlayerTheme(
    darkMode: DarkModeTheme = DarkModeTheme.SYSTEM,
    dynamicColor: Boolean = true,
    seedColorIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val isDark = when (darkMode) {
        DarkModeTheme.SYSTEM -> systemInDark
        DarkModeTheme.LIGHT -> false
        DarkModeTheme.DARK, DarkModeTheme.AMOLED -> true
    }

    val isAmoled = darkMode == DarkModeTheme.AMOLED

    val primarySeed = SeedColors.getOrElse(seedColorIndex) { SeedColors[0] }

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme(
            primary = primarySeed,
            secondary = primarySeed.copy(alpha = 0.8f),
            tertiary = Color(0xFF00D2B4)
        )
        else -> lightColorScheme(
            primary = primarySeed,
            secondary = primarySeed.copy(alpha = 0.8f),
            tertiary = Color(0xFF00D2B4)
        )
    }

    val colorScheme = if (isAmoled) {
        baseColorScheme.copy(
            background = AmoledBackground,
            surface = AmoledSurface,
            surfaceContainer = AmoledSurfaceContainer,
            surfaceContainerHigh = AmoledCard,
            surfaceContainerLowest = AmoledBackground,
            outline = AmoledBorder
        )
    } else {
        baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
