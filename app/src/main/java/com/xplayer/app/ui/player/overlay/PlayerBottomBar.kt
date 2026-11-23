package com.xplayer.app.ui.player.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xplayer.app.ui.home.formatDuration

@Composable
fun PlayerBottomBar(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    currentSpeed: Float,
    onPlayPause: () -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onSpeedChange: (speed: Float) -> Unit,
    onOpenTracks: () -> Unit,
    onToggleOrientation: () -> Unit,
    onEnterPip: () -> Unit,
    onPrevVideo: () -> Unit,
    onNextVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    var showRemainingTime by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    val effectivePosition = if (isScrubbing) scrubPosition.toLong() else currentPositionMs

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // 1. Time & Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(effectivePosition),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Slider(
                    value = effectivePosition.toFloat(),
                    onValueChange = {
                        isScrubbing = true
                        scrubPosition = it
                    },
                    onValueChangeFinished = {
                        onSeek(scrubPosition.toLong())
                        isScrubbing = false
                    },
                    valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                val displayDurationText = if (showRemainingTime) {
                    val remaining = (durationMs - effectivePosition).coerceAtLeast(0L)
                    "-${formatDuration(remaining)}"
                } else {
                    formatDuration(durationMs)
                }

                Text(
                    text = displayDurationText,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { showRemainingTime = !showRemainingTime }
                )
            }

            // 2. Playback Control Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left controls: Prev, Play/Pause, Next
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onPrevVideo) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev", tint = Color.White)
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(46.dp)
                    ) {
                        IconButton(onClick = onPlayPause) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    IconButton(onClick = onNextVideo) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White)
                    }
                }

                // Right controls: Speed, Tracks, Orientation, PiP
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Speed Selector
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { showSpeedMenu = true }
                        ) {
                            Text(
                                text = "${currentSpeed}x",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f).forEach { spd ->
                                DropdownMenuItem(
                                    text = { Text("${spd}x") },
                                    onClick = {
                                        onSpeedChange(spd)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = onOpenTracks) {
                        Icon(Icons.Filled.Audiotrack, contentDescription = "Tracks", tint = Color.White)
                    }

                    IconButton(onClick = onToggleOrientation) {
                        Icon(Icons.Filled.ScreenRotation, contentDescription = "Orientation", tint = Color.White)
                    }

                    IconButton(onClick = onEnterPip) {
                        Icon(Icons.Filled.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                    }
                }
            }
        }
    }
}
