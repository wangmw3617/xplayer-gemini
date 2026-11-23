package com.xplayer.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xplayer.app.R
import com.xplayer.app.data.datastore.GestureAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val gestures by viewModel.gesturePrefs.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    var activeDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_cat_gestures),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vertical Swipes
            item {
                SettingsGroup(title = "垂直滑动与音量增强") {
                    val leftLabel = when (gestures.leftVerticalAction) {
                        GestureAction.BRIGHTNESS -> stringResource(R.string.settings_gesture_action_brightness)
                        GestureAction.VOLUME -> stringResource(R.string.settings_gesture_action_volume)
                        GestureAction.NONE -> stringResource(R.string.settings_gesture_action_none)
                    }
                    SettingsClickableItem(
                        icon = Icons.Filled.BrightnessMedium,
                        title = stringResource(R.string.settings_gesture_left_vert),
                        subtitle = leftLabel,
                        onClick = { activeDialog = "LEFT_VERT" }
                    )

                    val rightLabel = when (gestures.rightVerticalAction) {
                        GestureAction.BRIGHTNESS -> stringResource(R.string.settings_gesture_action_brightness)
                        GestureAction.VOLUME -> stringResource(R.string.settings_gesture_action_volume)
                        GestureAction.NONE -> stringResource(R.string.settings_gesture_action_none)
                    }
                    SettingsClickableItem(
                        icon = Icons.Filled.VolumeUp,
                        title = stringResource(R.string.settings_gesture_right_vert),
                        subtitle = rightLabel,
                        onClick = { activeDialog = "RIGHT_VERT" }
                    )

                    SettingsSwitchItem(
                        icon = Icons.Filled.VolumeUp,
                        title = stringResource(R.string.settings_volume_boost),
                        subtitle = stringResource(R.string.settings_volume_boost_desc),
                        checked = gestures.volumeBoostEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateGestures { it.copy(volumeBoostEnabled = checked) }
                        }
                    )
                }
            }

            // Horizontal Swipe & Double Tap Seek
            item {
                SettingsGroup(title = "滑动 Seek 与双击分区") {
                    SettingsSwitchItem(
                        icon = Icons.Filled.FastForward,
                        title = stringResource(R.string.settings_horizontal_swipe),
                        subtitle = "满屏滑动跳转 ${gestures.seekSpanSeconds}s",
                        checked = gestures.horizontalSeekEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateGestures { it.copy(horizontalSeekEnabled = checked) }
                        }
                    )

                    SettingsSwitchItem(
                        icon = Icons.Filled.Gesture,
                        title = stringResource(R.string.settings_seek_exact),
                        subtitle = "精准定位到目标帧（关闭则跳至最近关键帧）",
                        checked = gestures.seekExact,
                        onCheckedChange = { checked ->
                            viewModel.updateGestures { it.copy(seekExact = checked) }
                        }
                    )

                    SettingsClickableItem(
                        icon = Icons.Filled.TouchApp,
                        title = stringResource(R.string.settings_double_tap_seek),
                        subtitle = "${gestures.doubleTapStepSeconds} 秒 (连续点击可叠加)",
                        onClick = { activeDialog = "DOUBLE_TAP_STEP" }
                    )
                }
            }

            // Long Press & Pinch Zoom
            item {
                SettingsGroup(title = "长按调速与缩放") {
                    SettingsSwitchItem(
                        icon = Icons.Filled.Speed,
                        title = stringResource(R.string.settings_long_press_speed),
                        subtitle = "长按触发 ${gestures.longPressSpeed}x 倍速",
                        checked = gestures.longPressSpeedEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateGestures { it.copy(longPressSpeedEnabled = checked) }
                        }
                    )

                    SettingsSwitchItem(
                        icon = Icons.Filled.Speed,
                        title = stringResource(R.string.settings_long_press_slide),
                        subtitle = "长按后左右滑动无级调速 (0.5x - 4.0x)",
                        checked = gestures.longPressSlideSpeedEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateGestures { it.copy(longPressSlideSpeedEnabled = checked) }
                        }
                    )

                    SettingsSwitchItem(
                        icon = Icons.Filled.ZoomIn,
                        title = stringResource(R.string.settings_pinch_zoom),
                        subtitle = "双指自由缩放画面 (50% - 400%) 并拖拽平移",
                        checked = gestures.pinchZoomEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateGestures { it.copy(pinchZoomEnabled = checked) }
                        }
                    )
                }
            }

            // Reset Gestures Button
            item {
                Button(
                    onClick = { viewModel.resetGestures() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(stringResource(R.string.settings_reset_gestures))
                }
            }
        }
    }

    // Bottom Sheet for Gesture Step Selection
    if (activeDialog != null) {
        ModalBottomSheet(
            onDismissRequest = { activeDialog = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeDialog == "DOUBLE_TAP_STEP") {
                    Text(
                        text = stringResource(R.string.settings_double_tap_seek),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    listOf(5, 10, 15, 30).forEach { seconds ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateGestures { it.copy(doubleTapStepSeconds = seconds) }
                                    scope.launch { sheetState.hide() }.invokeOnCompletion { activeDialog = null }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = gestures.doubleTapStepSeconds == seconds,
                                onClick = null
                            )
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Text("$seconds 秒", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else if (activeDialog == "LEFT_VERT" || activeDialog == "RIGHT_VERT") {
                    val isLeft = activeDialog == "LEFT_VERT"
                    Text(
                        text = if (isLeft) stringResource(R.string.settings_gesture_left_vert) else stringResource(R.string.settings_gesture_right_vert),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    GestureAction.values().forEach { action ->
                        val label = when (action) {
                            GestureAction.BRIGHTNESS -> stringResource(R.string.settings_gesture_action_brightness)
                            GestureAction.VOLUME -> stringResource(R.string.settings_gesture_action_volume)
                            GestureAction.NONE -> stringResource(R.string.settings_gesture_action_none)
                        }
                        val currentAction = if (isLeft) gestures.leftVerticalAction else gestures.rightVerticalAction
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateGestures {
                                        if (isLeft) it.copy(leftVerticalAction = action) else it.copy(rightVerticalAction = action)
                                    }
                                    scope.launch { sheetState.hide() }.invokeOnCompletion { activeDialog = null }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentAction == action, onClick = null)
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
