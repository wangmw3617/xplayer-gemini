package com.xplayer.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xplayer.app.R
import com.xplayer.app.data.datastore.DarkModeTheme
import com.xplayer.app.data.datastore.EnginePolicy
import com.xplayer.app.data.datastore.MpvHwdecMode
import com.xplayer.app.ui.theme.SeedColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToMpvLab: () -> Unit,
    onNavigateToCodecInspector: () -> Unit,
    onNavigateToGestures: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val appearance by viewModel.appearancePrefs.collectAsState()
    val engine by viewModel.enginePrefs.collectAsState()
    val playback by viewModel.playbackPrefs.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    var activeSheet by remember { mutableStateOf<SettingsSheetType?>(null) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group 1: Appearance & Theme
            item {
                SettingsGroup(title = stringResource(R.string.settings_cat_appearance)) {
                    SettingsSwitchItem(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.settings_dynamic_color),
                        subtitle = stringResource(R.string.settings_dynamic_color_desc),
                        checked = appearance.dynamicColor,
                        onCheckedChange = { viewModel.updateDynamicColor(it) }
                    )

                    if (!appearance.dynamicColor) {
                        SettingsClickableItem(
                            icon = Icons.Filled.ColorLens,
                            title = stringResource(R.string.settings_theme_seed),
                            subtitle = "Color #${appearance.seedColorIndex + 1}",
                            onClick = { activeSheet = SettingsSheetType.SEED_COLOR }
                        )
                    }

                    val darkModeSubtitle = when (appearance.darkMode) {
                        DarkModeTheme.SYSTEM -> stringResource(R.string.settings_dark_mode_system)
                        DarkModeTheme.LIGHT -> stringResource(R.string.settings_dark_mode_light)
                        DarkModeTheme.DARK -> stringResource(R.string.settings_dark_mode_dark)
                        DarkModeTheme.AMOLED -> stringResource(R.string.settings_dark_mode_amoled)
                    }
                    SettingsClickableItem(
                        icon = Icons.Filled.DarkMode,
                        title = stringResource(R.string.settings_dark_mode),
                        subtitle = darkModeSubtitle,
                        onClick = { activeSheet = SettingsSheetType.DARK_MODE }
                    )
                }
            }

            // Group 2: Engine & Decoding
            item {
                SettingsGroup(title = stringResource(R.string.settings_cat_engine)) {
                    val policySubtitle = when (engine.enginePolicy) {
                        EnginePolicy.MEDIA3_FIRST -> stringResource(R.string.settings_engine_policy_media3_first)
                        EnginePolicy.FORCE_MEDIA3 -> stringResource(R.string.settings_engine_policy_force_media3)
                        EnginePolicy.FORCE_MPV -> stringResource(R.string.settings_engine_policy_force_mpv)
                    }
                    SettingsClickableItem(
                        icon = Icons.Filled.Tune,
                        title = stringResource(R.string.settings_engine_policy),
                        subtitle = policySubtitle,
                        onClick = { activeSheet = SettingsSheetType.ENGINE_POLICY }
                    )

                    SettingsSwitchItem(
                        icon = Icons.Filled.Subtitles,
                        title = stringResource(R.string.settings_auto_switch_ass),
                        subtitle = stringResource(R.string.settings_auto_switch_ass_desc),
                        checked = engine.autoSwitchAss,
                        onCheckedChange = { viewModel.updateAutoSwitchAss(it) }
                    )

                    SettingsSwitchItem(
                        icon = Icons.Filled.Speed,
                        title = stringResource(R.string.settings_frame_rate_matching),
                        subtitle = stringResource(R.string.settings_frame_rate_matching_desc),
                        checked = engine.frameRateMatching,
                        onCheckedChange = { viewModel.updateFrameRateMatching(it) }
                    )

                    val hwdecSubtitle = when (engine.mpvHwdec) {
                        MpvHwdecMode.MEDIACODEC -> stringResource(R.string.settings_mpv_hwdec_mediacodec)
                        MpvHwdecMode.MEDIACODEC_COPY -> stringResource(R.string.settings_mpv_hwdec_copy)
                        MpvHwdecMode.NO -> stringResource(R.string.settings_mpv_hwdec_no)
                    }
                    SettingsClickableItem(
                        icon = Icons.Filled.Memory,
                        title = stringResource(R.string.settings_mpv_hwdec),
                        subtitle = hwdecSubtitle,
                        onClick = { activeSheet = SettingsSheetType.MPV_HWDEC }
                    )
                }
            }

            // Group: Gestures & Interaction
            item {
                SettingsGroup(title = stringResource(R.string.settings_cat_gestures)) {
                    SettingsClickableItem(
                        icon = Icons.Filled.Gesture,
                        title = stringResource(R.string.settings_cat_gestures),
                        subtitle = "滑动调光/音量、双击步长、长按倍速调控",
                        onClick = { onNavigateToGestures() }
                    )
                }
            }

            // Group 3: mpv Laboratory (Geek Zone)
            item {
                SettingsGroup(title = stringResource(R.string.settings_cat_mpv_lab)) {
                    SettingsClickableItem(
                        icon = Icons.Filled.Science,
                        title = stringResource(R.string.mpv_lab_title),
                        subtitle = stringResource(R.string.mpv_conf_editor_desc),
                        onClick = { onNavigateToMpvLab() }
                    )
                }
            }

            // Group 4: Hardware Codec Diagnostics
            item {
                SettingsGroup(title = stringResource(R.string.settings_cat_scanner)) {
                    SettingsClickableItem(
                        icon = Icons.Filled.Memory,
                        title = stringResource(R.string.codec_inspector_title),
                        subtitle = stringResource(R.string.home_codec_check_desc),
                        onClick = { onNavigateToCodecInspector() }
                    )

                    SettingsClickableItem(
                        icon = Icons.Filled.Refresh,
                        title = stringResource(R.string.videos_scan_now),
                        subtitle = stringResource(R.string.videos_refreshing),
                        onClick = { viewModel.triggerRescan() }
                    )
                }
            }

            // Group 5: About
            item {
                SettingsGroup(title = stringResource(R.string.settings_cat_about)) {
                    SettingsClickableItem(
                        icon = Icons.Filled.Tune,
                        title = stringResource(R.string.app_name),
                        subtitle = "v1.0.0 • Media3 + libmpv Dual Core",
                        onClick = {}
                    )
                }
            }
        }
    }

    // Modal Bottom Sheets for Option Selection
    activeSheet?.let { sheetType ->
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (sheetType) {
                    SettingsSheetType.DARK_MODE -> {
                        Text(
                            text = stringResource(R.string.settings_dark_mode),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        DarkModeTheme.values().forEach { mode ->
                            val label = when (mode) {
                                DarkModeTheme.SYSTEM -> stringResource(R.string.settings_dark_mode_system)
                                DarkModeTheme.LIGHT -> stringResource(R.string.settings_dark_mode_light)
                                DarkModeTheme.DARK -> stringResource(R.string.settings_dark_mode_dark)
                                DarkModeTheme.AMOLED -> stringResource(R.string.settings_dark_mode_amoled)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateDarkMode(mode)
                                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                                            activeSheet = null
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = appearance.darkMode == mode,
                                    onClick = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    SettingsSheetType.SEED_COLOR -> {
                        Text(
                            text = stringResource(R.string.settings_theme_seed),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            SeedColors.forEachIndexed { index, color ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (appearance.seedColorIndex == index) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.updateSeedColorIndex(index)
                                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                activeSheet = null
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (appearance.seedColorIndex == index) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingsSheetType.ENGINE_POLICY -> {
                        Text(
                            text = stringResource(R.string.settings_engine_policy),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        EnginePolicy.values().forEach { policy ->
                            val label = when (policy) {
                                EnginePolicy.MEDIA3_FIRST -> stringResource(R.string.settings_engine_policy_media3_first)
                                EnginePolicy.FORCE_MEDIA3 -> stringResource(R.string.settings_engine_policy_force_media3)
                                EnginePolicy.FORCE_MPV -> stringResource(R.string.settings_engine_policy_force_mpv)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateEnginePolicy(policy)
                                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                                            activeSheet = null
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = engine.enginePolicy == policy,
                                    onClick = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    SettingsSheetType.MPV_HWDEC -> {
                        Text(
                            text = stringResource(R.string.settings_mpv_hwdec),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        MpvHwdecMode.values().forEach { mode ->
                            val label = when (mode) {
                                MpvHwdecMode.MEDIACODEC -> stringResource(R.string.settings_mpv_hwdec_mediacodec)
                                MpvHwdecMode.MEDIACODEC_COPY -> stringResource(R.string.settings_mpv_hwdec_copy)
                                MpvHwdecMode.NO -> stringResource(R.string.settings_mpv_hwdec_no)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateMpvHwdec(mode)
                                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                                            activeSheet = null
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = engine.mpvHwdec == mode,
                                    onClick = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

enum class SettingsSheetType {
    DARK_MODE,
    SEED_COLOR,
    ENGINE_POLICY,
    MPV_HWDEC
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else null
        )
    }
}
