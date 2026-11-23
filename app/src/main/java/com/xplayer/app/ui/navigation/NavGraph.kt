package com.xplayer.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.xplayer.app.ui.folders.FoldersScreen
import com.xplayer.app.ui.home.HomeScreen
import com.xplayer.app.ui.player.PlayerScreen
import com.xplayer.app.ui.settings.CodecInspectorScreen
import com.xplayer.app.ui.settings.GestureSettingsScreen
import com.xplayer.app.ui.settings.MpvLabScreen
import com.xplayer.app.ui.settings.SettingsScreen
import com.xplayer.app.ui.videos.VideosScreen

@Composable
fun MainNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isPlayerScreen = currentRoute?.startsWith("player/") == true

    Scaffold(
        bottomBar = {
            if (!isPlayerScreen) {
                FlClashBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(if (isPlayerScreen) androidx.compose.foundation.layout.PaddingValues() else innerPadding)
        ) {
            // Tab 1: Home
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToPlayer = { path ->
                        navController.navigate(Screen.Player.createRoute(path))
                    },
                    onNavigateToCodecInspector = {
                        navController.navigate(Screen.CodecInspector.route)
                    }
                )
            }

            // Tab 2: Videos
            composable(Screen.Videos.route) {
                VideosScreen(
                    onNavigateToPlayer = { path ->
                        navController.navigate(Screen.Player.createRoute(path))
                    }
                )
            }

            // Tab 3: Folders
            composable(Screen.Folders.route) {
                FoldersScreen(
                    onNavigateToPlayer = { path ->
                        navController.navigate(Screen.Player.createRoute(path))
                    }
                )
            }

            // Tab 4: Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToMpvLab = {
                        navController.navigate(Screen.MpvLab.route)
                    },
                    onNavigateToCodecInspector = {
                        navController.navigate(Screen.CodecInspector.route)
                    },
                    onNavigateToGestures = {
                        navController.navigate("gestures")
                    }
                )
            }

            // Subpage: Gestures
            composable("gestures") {
                GestureSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Subpage: mpv Lab
            composable(Screen.MpvLab.route) {
                MpvLabScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Subpage: Codec Inspector
            composable(Screen.CodecInspector.route) {
                CodecInspectorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Subpage: Player
            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("mediaPath") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("mediaPath") ?: ""
                val mediaPath = Uri.decode(encodedPath)

                PlayerScreen(
                    mediaPath = mediaPath,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
