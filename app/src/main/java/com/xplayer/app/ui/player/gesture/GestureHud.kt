package com.xplayer.app.ui.player.gesture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xplayer.app.ui.home.formatDuration

sealed interface HudState {
    data object None : HudState
    data class Volume(val volumePercent: Int) : HudState
    data class Brightness(val brightnessPercent: Int) : HudState
    data class Seek(val targetPositionMs: Long, val deltaMs: Long, val totalDurationMs: Long) : HudState
    data class Speed(val speed: Float) : HudState
}

@Composable
fun GestureHudOverlay(
    hudState: HudState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = hudState !is HudState.None,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.75f),
                contentColor = Color.White
            ) {
                when (hudState) {
                    is HudState.Volume -> {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val icon = when {
                                hudState.volumePercent == 0 -> Icons.Filled.VolumeMute
                                hudState.volumePercent <= 50 -> Icons.Filled.VolumeDown
                                else -> Icons.Filled.VolumeUp
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp))
                            Text(
                                text = "${hudState.volumePercent}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { (hudState.volumePercent.toFloat() / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (hudState.volumePercent > 100) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }

                    is HudState.Brightness -> {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.BrightnessHigh, contentDescription = null, modifier = Modifier.size(36.dp))
                            Text(
                                text = "${hudState.brightnessPercent}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { (hudState.brightnessPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }

                    is HudState.Seek -> {
                        Column(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.FastForward, contentDescription = null, modifier = Modifier.size(36.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatDuration(hudState.targetPositionMs),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = " / ${formatDuration(hudState.totalDurationMs)}",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            val sign = if (hudState.deltaMs >= 0) "+" else ""
                            Text(
                                text = "[$sign${hudState.deltaMs / 1000}s]",
                                fontSize = 13.sp,
                                color = if (hudState.deltaMs >= 0) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    is HudState.Speed -> {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(24.dp))
                            Text(
                                text = "${hudState.speed}x 倍速播放中",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HudState.None -> {}
                }
            }
        }
    }
}
