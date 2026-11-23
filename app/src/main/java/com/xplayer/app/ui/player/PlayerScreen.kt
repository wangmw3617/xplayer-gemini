package com.xplayer.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xplayer.app.player.pip.PipController
import com.xplayer.app.ui.player.gesture.GestureHudOverlay
import com.xplayer.app.ui.player.gesture.HudState
import com.xplayer.app.ui.player.gesture.PlayerGestureDetector
import com.xplayer.app.ui.player.overlay.PlayerBottomBar
import com.xplayer.app.ui.player.overlay.PlayerSideControls
import com.xplayer.app.ui.player.overlay.PlayerTopBar
import com.xplayer.app.ui.player.overlay.TracksBottomSheet
import com.xplayer.app.ui.player.util.BrightnessHelper
import com.xplayer.app.ui.player.util.VolumeHelper
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    mediaPath: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as Activity

    val uiState by viewModel.uiState.collectAsState()
    val hudState by viewModel.hudState.collectAsState()
    val gesturePrefs by viewModel.gesturePrefs.collectAsState()

    var areControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showTracksSheet by remember { mutableStateOf(false) }

    // Zoom & pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Orientation toggle
    var isLandscape by remember { mutableStateOf(false) }

    val pipController = remember {
        PipController(
            activity = activity,
            onPlayPause = {
                if (uiState.isPlaying) viewModel.pause() else viewModel.play()
            },
            onSeek = { offsetMs ->
                viewModel.seekTo(uiState.currentPositionMs + offsetMs)
            }
        )
    }

    // Subtitle File Picker
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val name = it.lastPathSegment ?: "External Subtitle"
            viewModel.addExternalSubtitle(it, name)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(areControlsVisible, uiState.isPlaying) {
        if (areControlsVisible && uiState.isPlaying && !isLocked) {
            delay(4000L)
            areControlsVisible = false
        }
    }

    // Load media on start
    LaunchedEffect(mediaPath) {
        viewModel.loadMedia(mediaPath)
    }

    // Toast listener
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Manage system bars immersive mode & PiP registration
    DisposableEffect(Unit) {
        pipController.register()
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            pipController.unregister()
            BrightnessHelper.restoreScreenBrightness(activity)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Gesture Detector & SurfaceView container
        PlayerGestureDetector(
            gesturePrefs = gesturePrefs,
            isLocked = isLocked,
            onSingleTap = {
                areControlsVisible = !areControlsVisible
            },
            onDoubleTapSeek = { offsetMs ->
                val newPos = (uiState.currentPositionMs + offsetMs).coerceIn(0L, uiState.durationMs)
                viewModel.seekTo(newPos)
                viewModel.showHud(HudState.Seek(newPos, offsetMs, uiState.durationMs))
            },
            onPlayPause = {
                if (uiState.isPlaying) viewModel.pause() else viewModel.play()
            },
            onSeekScrub = { deltaMs, isFinished ->
                val target = (uiState.currentPositionMs + deltaMs).coerceIn(0L, uiState.durationMs)
                if (isFinished) {
                    viewModel.seekTo(target)
                } else {
                    viewModel.showHud(HudState.Seek(target, deltaMs, uiState.durationMs), autoDismissMs = 0L)
                }
            },
            onBrightnessChange = { delta ->
                val current = BrightnessHelper.getScreenBrightness(activity)
                val newBrightness = (current + delta).coerceIn(0.01f, 1.0f)
                BrightnessHelper.setScreenBrightness(activity, newBrightness)
                viewModel.showHud(HudState.Brightness((newBrightness * 100).toInt()))
            },
            onVolumeChange = { delta ->
                val (vol, maxVol) = VolumeHelper.getVolume(context)
                val step = if (delta > 0) 1 else -1
                val newVol = (vol + step).coerceIn(0, maxVol)
                VolumeHelper.setVolume(context, newVol)
                val percent = (newVol.toFloat() / maxVol.toFloat() * 100).toInt()
                viewModel.showHud(HudState.Volume(percent))
            },
            onLongPressStart = {
                viewModel.setSpeed(gesturePrefs.longPressSpeed)
                viewModel.showHud(HudState.Speed(gesturePrefs.longPressSpeed), autoDismissMs = 0L)
            },
            onLongPressSlide = { _ -> },
            onLongPressEnd = {
                viewModel.setSpeed(1.0f)
                viewModel.showHud(HudState.None, autoDismissMs = 0L)
            },
            onZoomAndPan = { newScale, newPan ->
                scale = newScale
                panX = newPan.x
                panY = newPan.y
            }
        ) {
            // Video SurfaceView
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        viewModel.attachSurface(this)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = panX
                        translationY = panY
                    }
            )
        }

        // 2. Center HUD Overlay (Brightness, Volume, Seek, Speed)
        GestureHudOverlay(hudState = hudState)

        // 3. Immersive Overlay Controls
        AnimatedVisibility(
            visible = areControlsVisible || isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!isLocked) {
                    // Top Bar
                    PlayerTopBar(
                        title = mediaPath.substringAfterLast('/'),
                        activeEngine = uiState.activeEngine,
                        aspectRatioMode = uiState.aspectRatioMode,
                        onBack = onBack,
                        onSwitchEngine = { viewModel.switchKernel() },
                        onCycleAspectRatio = { viewModel.cycleAspectRatio() },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    // Bottom Bar
                    PlayerBottomBar(
                        isPlaying = uiState.isPlaying,
                        currentPositionMs = uiState.currentPositionMs,
                        durationMs = uiState.durationMs,
                        bufferedPositionMs = uiState.bufferedPositionMs,
                        currentSpeed = uiState.speed,
                        onPlayPause = {
                            if (uiState.isPlaying) viewModel.pause() else viewModel.play()
                        },
                        onSeek = { pos -> viewModel.seekTo(pos) },
                        onSpeedChange = { spd -> viewModel.setSpeed(spd) },
                        onOpenTracks = { showTracksSheet = true },
                        onToggleOrientation = {
                            isLandscape = !isLandscape
                            activity.requestedOrientation = if (isLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        },
                        onEnterPip = {
                            pipController.enterPip(uiState.videoWidth, uiState.videoHeight, uiState.isPlaying)
                        },
                        onPrevVideo = {},
                        onNextVideo = {},
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                // Side Controls (Lock, Screenshot, Background audio)
                PlayerSideControls(
                    isLocked = isLocked,
                    onToggleLock = { isLocked = !isLocked },
                    onCaptureScreenshot = { viewModel.captureScreenshot(activity) },
                    onEnableBackgroundAudio = {
                        Toast.makeText(context, "后台播放已开启", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 4. Tracks & Subtitles ModalBottomSheet
        if (showTracksSheet) {
            TracksBottomSheet(
                audioTracks = uiState.audioTracks,
                selectedAudioId = uiState.selectedAudioTrackId,
                subtitleTracks = uiState.subtitleTracks,
                selectedSubtitleId = uiState.selectedSubtitleTrackId,
                subtitleDelayMs = uiState.subtitleDelayMs,
                onSelectAudio = { viewModel.selectAudioTrack(it) },
                onSelectSubtitle = { viewModel.selectSubtitleTrack(it) },
                onImportSubtitle = { subtitlePickerLauncher.launch("*/*") },
                onAdjustSubtitleDelay = { delta -> viewModel.adjustSubtitleDelay(delta) },
                onResetSubtitleDelay = { viewModel.resetSubtitleDelay() },
                onDismiss = { showTracksSheet = false }
            )
        }
    }
}
