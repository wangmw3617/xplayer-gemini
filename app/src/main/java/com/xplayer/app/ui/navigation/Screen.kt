package com.xplayer.app.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Videos : Screen("videos")
    data object Folders : Screen("folders")
    data object Settings : Screen("settings")

    data object MpvLab : Screen("mpv_lab")
    data object CodecInspector : Screen("codec_inspector")

    data object Player : Screen("player/{mediaPath}") {
        fun createRoute(mediaPath: String): String {
            return "player/${Uri.encode(mediaPath)}"
        }
    }
}
