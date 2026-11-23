package com.xplayer.app.ui.player.gesture

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.xplayer.app.data.datastore.GestureAction
import com.xplayer.app.data.datastore.GesturePrefs
import kotlin.math.abs

@Composable
fun PlayerGestureDetector(
    gesturePrefs: GesturePrefs,
    isLocked: Boolean,
    onSingleTap: () -> Unit,
    onDoubleTapSeek: (offsetMs: Long) -> Unit,
    onPlayPause: () -> Unit,
    onSeekScrub: (deltaMs: Long, isFinished: Boolean) -> Unit,
    onBrightnessChange: (delta: Float) -> Unit,
    onVolumeChange: (delta: Float) -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressSlide: (speedOffset: Float) -> Unit,
    onLongPressEnd: () -> Unit,
    onZoomAndPan: (scale: Float, pan: Offset) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val edgeDeadzonePx = with(density) { gesturePrefs.edgeDeadzoneDp.dp.toPx() }

    var isTransforming by remember { mutableStateOf(false) }
    var currentScale by remember { mutableFloatStateOf(1f) }
    var currentOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 1. Two-finger pinch to zoom & pan
            .pointerInput(isLocked, gesturePrefs.pinchZoomEnabled) {
                if (!isLocked && gesturePrefs.pinchZoomEnabled) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        isTransforming = true
                        currentScale = (currentScale * zoom).coerceIn(0.5f, 4.0f)
                        currentOffset += pan
                        onZoomAndPan(currentScale, currentOffset)
                    }
                }
            }
            // 2. Drag & Tap & Long-press detection
            .pointerInput(isLocked, gesturePrefs) {
                detectTapGestures(
                    onTap = { offset ->
                        onSingleTap()
                    },
                    onDoubleTap = { offset ->
                        if (isLocked) return@detectTapGestures
                        val screenWidth = size.width
                        val stepMs = gesturePrefs.doubleTapStepSeconds * 1000L

                        when {
                            offset.x < screenWidth * 0.35f -> onDoubleTapSeek(-stepMs)
                            offset.x > screenWidth * 0.65f -> onDoubleTapSeek(stepMs)
                            else -> onPlayPause()
                        }
                    },
                    onLongPress = {
                        if (isLocked || !gesturePrefs.longPressSpeedEnabled) return@detectTapGestures
                        onLongPressStart()
                    },
                    onPress = {
                        tryAwaitRelease()
                        onLongPressEnd()
                        isTransforming = false
                    }
                )
            }
            .pointerInput(isLocked, gesturePrefs) {
                if (isLocked) return@pointerInput

                var startOffset = Offset.Zero
                var isHorizontalSeek = false
                var isVerticalDrag = false
                var isLeftSide = false
                var accumulatedDeltaX = 0f
                var accumulatedDeltaY = 0f

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes

                        if (changes.isEmpty()) continue
                        val change = changes[0]

                        // Ignore deadzone edges
                        val posX = change.position.x
                        val posY = change.position.y
                        if (posX < edgeDeadzonePx || posX > size.width - edgeDeadzonePx ||
                            posY < edgeDeadzonePx || posY > size.height - edgeDeadzonePx
                        ) {
                            continue
                        }

                        if (change.pressed && !change.previousPressed) {
                            // Touch down
                            startOffset = change.position
                            isLeftSide = startOffset.x < size.width * 0.5f
                            accumulatedDeltaX = 0f
                            accumulatedDeltaY = 0f
                            isHorizontalSeek = false
                            isVerticalDrag = false
                        } else if (change.pressed && change.previousPressed) {
                            val dragAmount = change.position - change.previousPosition
                            accumulatedDeltaX += dragAmount.x
                            accumulatedDeltaY += dragAmount.y

                            if (!isHorizontalSeek && !isVerticalDrag && changes.size == 1) {
                                if (abs(accumulatedDeltaX) > 40f && abs(accumulatedDeltaX) > abs(accumulatedDeltaY) * 1.5f) {
                                    isHorizontalSeek = true
                                } else if (abs(accumulatedDeltaY) > 40f && abs(accumulatedDeltaY) > abs(accumulatedDeltaX) * 1.5f) {
                                    isVerticalDrag = true
                                }
                            }

                            if (isHorizontalSeek && gesturePrefs.horizontalSeekEnabled) {
                                change.consume()
                                val seekSpanMs = gesturePrefs.seekSpanSeconds * 1000L
                                val deltaRatio = dragAmount.x / size.width
                                val deltaMs = (deltaRatio * seekSpanMs).toLong()
                                onSeekScrub(deltaMs, false)
                            } else if (isVerticalDrag) {
                                change.consume()
                                val delta = -dragAmount.y / size.height * gesturePrefs.verticalSensitivity
                                if (isLeftSide) {
                                    when (gesturePrefs.leftVerticalAction) {
                                        GestureAction.BRIGHTNESS -> onBrightnessChange(delta)
                                        GestureAction.VOLUME -> onVolumeChange(delta)
                                        GestureAction.NONE -> {}
                                    }
                                } else {
                                    when (gesturePrefs.rightVerticalAction) {
                                        GestureAction.VOLUME -> onVolumeChange(delta)
                                        GestureAction.BRIGHTNESS -> onBrightnessChange(delta)
                                        GestureAction.NONE -> {}
                                    }
                                }
                            }
                        } else if (!change.pressed && change.previousPressed) {
                            // Touch up
                            if (isHorizontalSeek) {
                                onSeekScrub(0L, true)
                            }
                        }
                    }
                }
            }
    ) {
        content()
    }
}
