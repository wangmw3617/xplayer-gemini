package com.xplayer.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xplayer_settings")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        // Appearance
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SEED_COLOR_INDEX = intPreferencesKey("seed_color_index")
        val DARK_MODE = stringPreferencesKey("dark_mode")

        // Engine
        val ENGINE_POLICY = stringPreferencesKey("engine_policy")
        val AUTO_SWITCH_ASS = booleanPreferencesKey("auto_switch_ass")
        val FRAME_RATE_MATCHING = booleanPreferencesKey("frame_rate_matching")
        val MPV_HWDEC = stringPreferencesKey("mpv_hwdec")
        val CUSTOM_MPV_CONF_ENABLED = booleanPreferencesKey("custom_mpv_conf_enabled")
        val CUSTOM_MPV_CONF = stringPreferencesKey("custom_mpv_conf")

        // Gestures
        val LEFT_VERT_ACTION = stringPreferencesKey("left_vert_action")
        val RIGHT_VERT_ACTION = stringPreferencesKey("right_vert_action")
        val VERT_SENSITIVITY = floatPreferencesKey("vert_sensitivity")
        val VOLUME_BOOST = booleanPreferencesKey("volume_boost")
        val HORIZONTAL_SEEK = booleanPreferencesKey("horizontal_seek")
        val SEEK_SPAN_SECONDS = intPreferencesKey("seek_span_seconds")
        val SEEK_EXACT = booleanPreferencesKey("seek_exact")
        val DOUBLE_TAP_LEFT = stringPreferencesKey("double_tap_left")
        val DOUBLE_TAP_RIGHT = stringPreferencesKey("double_tap_right")
        val DOUBLE_TAP_CENTER = stringPreferencesKey("double_tap_center")
        val DOUBLE_TAP_STEP = intPreferencesKey("double_tap_step")
        val LONG_PRESS_SPEED_ENABLED = booleanPreferencesKey("long_press_speed_enabled")
        val LONG_PRESS_SPEED = floatPreferencesKey("long_press_speed")
        val LONG_PRESS_SLIDE = booleanPreferencesKey("long_press_slide")
        val LONG_PRESS_DELAY = longPreferencesKey("long_press_delay")
        val PINCH_ZOOM = booleanPreferencesKey("pinch_zoom")
        val EDGE_DEADZONE_DP = intPreferencesKey("edge_deadzone_dp")

        // Subtitles
        val SUB_FONT_SIZE_SCALE = floatPreferencesKey("sub_font_size_scale")
        val SUB_FONT_COLOR = intPreferencesKey("sub_font_color")
        val SUB_OUTLINE_WIDTH = floatPreferencesKey("sub_outline_width")
        val SUB_SHADOW_ENABLED = booleanPreferencesKey("sub_shadow_enabled")
        val SUB_BG_ENABLED = booleanPreferencesKey("sub_bg_enabled")
        val SUB_BOTTOM_PADDING = intPreferencesKey("sub_bottom_padding")
        val SUB_OVERRIDE_ASS = booleanPreferencesKey("sub_override_ass")

        // Playback
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val BG_BEHAVIOR = stringPreferencesKey("bg_behavior")
        val SCREENSHOT_FORMAT = stringPreferencesKey("screenshot_format")
    }

    val appearancePrefsFlow: Flow<AppearancePrefs> = context.dataStore.data.map { pref ->
        AppearancePrefs(
            dynamicColor = pref[Keys.DYNAMIC_COLOR] ?: true,
            seedColorIndex = pref[Keys.SEED_COLOR_INDEX] ?: 0,
            darkMode = pref[Keys.DARK_MODE]?.let {
                runCatching { DarkModeTheme.valueOf(it) }.getOrNull()
            } ?: DarkModeTheme.SYSTEM
        )
    }

    val enginePrefsFlow: Flow<EnginePrefs> = context.dataStore.data.map { pref ->
        EnginePrefs(
            enginePolicy = pref[Keys.ENGINE_POLICY]?.let {
                runCatching { EnginePolicy.valueOf(it) }.getOrNull()
            } ?: EnginePolicy.MEDIA3_FIRST,
            autoSwitchAss = pref[Keys.AUTO_SWITCH_ASS] ?: true,
            frameRateMatching = pref[Keys.FRAME_RATE_MATCHING] ?: true,
            mpvHwdec = pref[Keys.MPV_HWDEC]?.let {
                runCatching { MpvHwdecMode.valueOf(it) }.getOrNull()
            } ?: MpvHwdecMode.MEDIACODEC,
            customMpvConfEnabled = pref[Keys.CUSTOM_MPV_CONF_ENABLED] ?: false,
            customMpvConf = pref[Keys.CUSTOM_MPV_CONF] ?: "# mpv.conf\nvo=gpu-next\ngpu-context=android\nhwdec=mediacodec\n"
        )
    }

    val gesturePrefsFlow: Flow<GesturePrefs> = context.dataStore.data.map { pref ->
        GesturePrefs(
            leftVerticalAction = pref[Keys.LEFT_VERT_ACTION]?.let {
                runCatching { GestureAction.valueOf(it) }.getOrNull()
            } ?: GestureAction.BRIGHTNESS,
            rightVerticalAction = pref[Keys.RIGHT_VERT_ACTION]?.let {
                runCatching { GestureAction.valueOf(it) }.getOrNull()
            } ?: GestureAction.VOLUME,
            verticalSensitivity = pref[Keys.VERT_SENSITIVITY] ?: 1.0f,
            volumeBoostEnabled = pref[Keys.VOLUME_BOOST] ?: true,
            horizontalSeekEnabled = pref[Keys.HORIZONTAL_SEEK] ?: true,
            seekSpanSeconds = pref[Keys.SEEK_SPAN_SECONDS] ?: 60,
            seekExact = pref[Keys.SEEK_EXACT] ?: false,
            doubleTapLeftAction = pref[Keys.DOUBLE_TAP_LEFT]?.let {
                runCatching { DoubleTapAction.valueOf(it) }.getOrNull()
            } ?: DoubleTapAction.SEEK_BACKWARD,
            doubleTapRightAction = pref[Keys.DOUBLE_TAP_RIGHT]?.let {
                runCatching { DoubleTapAction.valueOf(it) }.getOrNull()
            } ?: DoubleTapAction.SEEK_FORWARD,
            doubleTapCenterAction = pref[Keys.DOUBLE_TAP_CENTER]?.let {
                runCatching { CenterTapAction.valueOf(it) }.getOrNull()
            } ?: CenterTapAction.PLAY_PAUSE,
            doubleTapStepSeconds = pref[Keys.DOUBLE_TAP_STEP] ?: 10,
            longPressSpeedEnabled = pref[Keys.LONG_PRESS_SPEED_ENABLED] ?: true,
            longPressSpeed = pref[Keys.LONG_PRESS_SPEED] ?: 2.0f,
            longPressSlideSpeedEnabled = pref[Keys.LONG_PRESS_SLIDE] ?: true,
            longPressDelayMs = pref[Keys.LONG_PRESS_DELAY] ?: 300L,
            pinchZoomEnabled = pref[Keys.PINCH_ZOOM] ?: true,
            edgeDeadzoneDp = pref[Keys.EDGE_DEADZONE_DP] ?: 24
        )
    }

    val subtitlePrefsFlow: Flow<SubtitlePrefs> = context.dataStore.data.map { pref ->
        SubtitlePrefs(
            fontSizeScale = pref[Keys.SUB_FONT_SIZE_SCALE] ?: 1.0f,
            fontColor = pref[Keys.SUB_FONT_COLOR] ?: 0xFFFFFFFF.toInt(),
            outlineWidth = pref[Keys.SUB_OUTLINE_WIDTH] ?: 2.0f,
            shadowEnabled = pref[Keys.SUB_SHADOW_ENABLED] ?: true,
            backgroundEnabled = pref[Keys.SUB_BG_ENABLED] ?: false,
            bottomPaddingDp = pref[Keys.SUB_BOTTOM_PADDING] ?: 24,
            overrideAssStyle = pref[Keys.SUB_OVERRIDE_ASS] ?: false
        )
    }

    val playbackPrefsFlow: Flow<PlaybackPrefs> = context.dataStore.data.map { pref ->
        PlaybackPrefs(
            defaultSpeed = pref[Keys.DEFAULT_SPEED] ?: 1.0f,
            autoPlayNext = pref[Keys.AUTO_PLAY_NEXT] ?: true,
            backgroundBehavior = pref[Keys.BG_BEHAVIOR]?.let {
                runCatching { BackgroundBehavior.valueOf(it) }.getOrNull()
            } ?: BackgroundBehavior.PAUSE,
            screenshotFormat = pref[Keys.SCREENSHOT_FORMAT] ?: "PNG"
        )
    }

    // Updaters
    suspend fun updateAppearance(update: (AppearancePrefs) -> AppearancePrefs) {
        context.dataStore.edit { pref ->
            val current = AppearancePrefs(
                dynamicColor = pref[Keys.DYNAMIC_COLOR] ?: true,
                seedColorIndex = pref[Keys.SEED_COLOR_INDEX] ?: 0,
                darkMode = pref[Keys.DARK_MODE]?.let { runCatching { DarkModeTheme.valueOf(it) }.getOrNull() } ?: DarkModeTheme.SYSTEM
            )
            val updated = update(current)
            pref[Keys.DYNAMIC_COLOR] = updated.dynamicColor
            pref[Keys.SEED_COLOR_INDEX] = updated.seedColorIndex
            pref[Keys.DARK_MODE] = updated.darkMode.name
        }
    }

    suspend fun updateEngine(update: (EnginePrefs) -> EnginePrefs) {
        context.dataStore.edit { pref ->
            val current = EnginePrefs(
                enginePolicy = pref[Keys.ENGINE_POLICY]?.let { runCatching { EnginePolicy.valueOf(it) }.getOrNull() } ?: EnginePolicy.MEDIA3_FIRST,
                autoSwitchAss = pref[Keys.AUTO_SWITCH_ASS] ?: true,
                frameRateMatching = pref[Keys.FRAME_RATE_MATCHING] ?: true,
                mpvHwdec = pref[Keys.MPV_HWDEC]?.let { runCatching { MpvHwdecMode.valueOf(it) }.getOrNull() } ?: MpvHwdecMode.MEDIACODEC,
                customMpvConfEnabled = pref[Keys.CUSTOM_MPV_CONF_ENABLED] ?: false,
                customMpvConf = pref[Keys.CUSTOM_MPV_CONF] ?: "# mpv.conf\nvo=gpu-next\ngpu-context=android\nhwdec=mediacodec\n"
            )
            val updated = update(current)
            pref[Keys.ENGINE_POLICY] = updated.enginePolicy.name
            pref[Keys.AUTO_SWITCH_ASS] = updated.autoSwitchAss
            pref[Keys.FRAME_RATE_MATCHING] = updated.frameRateMatching
            pref[Keys.MPV_HWDEC] = updated.mpvHwdec.name
            pref[Keys.CUSTOM_MPV_CONF_ENABLED] = updated.customMpvConfEnabled
            pref[Keys.CUSTOM_MPV_CONF] = updated.customMpvConf
        }
    }

    suspend fun updateGestures(update: (GesturePrefs) -> GesturePrefs) {
        context.dataStore.edit { pref ->
            val current = GesturePrefs(
                leftVerticalAction = pref[Keys.LEFT_VERT_ACTION]?.let { runCatching { GestureAction.valueOf(it) }.getOrNull() } ?: GestureAction.BRIGHTNESS,
                rightVerticalAction = pref[Keys.RIGHT_VERT_ACTION]?.let { runCatching { GestureAction.valueOf(it) }.getOrNull() } ?: GestureAction.VOLUME,
                verticalSensitivity = pref[Keys.VERT_SENSITIVITY] ?: 1.0f,
                volumeBoostEnabled = pref[Keys.VOLUME_BOOST] ?: true,
                horizontalSeekEnabled = pref[Keys.HORIZONTAL_SEEK] ?: true,
                seekSpanSeconds = pref[Keys.SEEK_SPAN_SECONDS] ?: 60,
                seekExact = pref[Keys.SEEK_EXACT] ?: false,
                doubleTapLeftAction = pref[Keys.DOUBLE_TAP_LEFT]?.let { runCatching { DoubleTapAction.valueOf(it) }.getOrNull() } ?: DoubleTapAction.SEEK_BACKWARD,
                doubleTapRightAction = pref[Keys.DOUBLE_TAP_RIGHT]?.let { runCatching { DoubleTapAction.valueOf(it) }.getOrNull() } ?: DoubleTapAction.SEEK_FORWARD,
                doubleTapCenterAction = pref[Keys.DOUBLE_TAP_CENTER]?.let { runCatching { CenterTapAction.valueOf(it) }.getOrNull() } ?: CenterTapAction.PLAY_PAUSE,
                doubleTapStepSeconds = pref[Keys.DOUBLE_TAP_STEP] ?: 10,
                longPressSpeedEnabled = pref[Keys.LONG_PRESS_SPEED_ENABLED] ?: true,
                longPressSpeed = pref[Keys.LONG_PRESS_SPEED] ?: 2.0f,
                longPressSlideSpeedEnabled = pref[Keys.LONG_PRESS_SLIDE] ?: true,
                longPressDelayMs = pref[Keys.LONG_PRESS_DELAY] ?: 300L,
                pinchZoomEnabled = pref[Keys.PINCH_ZOOM] ?: true,
                edgeDeadzoneDp = pref[Keys.EDGE_DEADZONE_DP] ?: 24
            )
            val updated = update(current)
            pref[Keys.LEFT_VERT_ACTION] = updated.leftVerticalAction.name
            pref[Keys.RIGHT_VERT_ACTION] = updated.rightVerticalAction.name
            pref[Keys.VERT_SENSITIVITY] = updated.verticalSensitivity
            pref[Keys.VOLUME_BOOST] = updated.volumeBoostEnabled
            pref[Keys.HORIZONTAL_SEEK] = updated.horizontalSeekEnabled
            pref[Keys.SEEK_SPAN_SECONDS] = updated.seekSpanSeconds
            pref[Keys.SEEK_EXACT] = updated.seekExact
            pref[Keys.DOUBLE_TAP_LEFT] = updated.doubleTapLeftAction.name
            pref[Keys.DOUBLE_TAP_RIGHT] = updated.doubleTapRightAction.name
            pref[Keys.DOUBLE_TAP_CENTER] = updated.doubleTapCenterAction.name
            pref[Keys.DOUBLE_TAP_STEP] = updated.doubleTapStepSeconds
            pref[Keys.LONG_PRESS_SPEED_ENABLED] = updated.longPressSpeedEnabled
            pref[Keys.LONG_PRESS_SPEED] = updated.longPressSpeed
            pref[Keys.LONG_PRESS_SLIDE] = updated.longPressSlideSpeedEnabled
            pref[Keys.LONG_PRESS_DELAY] = updated.longPressDelayMs
            pref[Keys.PINCH_ZOOM] = updated.pinchZoomEnabled
            pref[Keys.EDGE_DEADZONE_DP] = updated.edgeDeadzoneDp
        }
    }

    suspend fun resetGesturesToDefault() {
        updateGestures { GesturePrefs() }
    }

    suspend fun updateSubtitles(update: (SubtitlePrefs) -> SubtitlePrefs) {
        context.dataStore.edit { pref ->
            val current = SubtitlePrefs(
                fontSizeScale = pref[Keys.SUB_FONT_SIZE_SCALE] ?: 1.0f,
                fontColor = pref[Keys.SUB_FONT_COLOR] ?: 0xFFFFFFFF.toInt(),
                outlineWidth = pref[Keys.SUB_OUTLINE_WIDTH] ?: 2.0f,
                shadowEnabled = pref[Keys.SUB_SHADOW_ENABLED] ?: true,
                backgroundEnabled = pref[Keys.SUB_BG_ENABLED] ?: false,
                bottomPaddingDp = pref[Keys.SUB_BOTTOM_PADDING] ?: 24,
                overrideAssStyle = pref[Keys.SUB_OVERRIDE_ASS] ?: false
            )
            val updated = update(current)
            pref[Keys.SUB_FONT_SIZE_SCALE] = updated.fontSizeScale
            pref[Keys.SUB_FONT_COLOR] = updated.fontColor
            pref[Keys.SUB_OUTLINE_WIDTH] = updated.outlineWidth
            pref[Keys.SUB_SHADOW_ENABLED] = updated.shadowEnabled
            pref[Keys.SUB_BG_ENABLED] = updated.backgroundEnabled
            pref[Keys.SUB_BOTTOM_PADDING] = updated.bottomPaddingDp
            pref[Keys.SUB_OVERRIDE_ASS] = updated.overrideAssStyle
        }
    }

    suspend fun updatePlayback(update: (PlaybackPrefs) -> PlaybackPrefs) {
        context.dataStore.edit { pref ->
            val current = PlaybackPrefs(
                defaultSpeed = pref[Keys.DEFAULT_SPEED] ?: 1.0f,
                autoPlayNext = pref[Keys.AUTO_PLAY_NEXT] ?: true,
                backgroundBehavior = pref[Keys.BG_BEHAVIOR]?.let { runCatching { BackgroundBehavior.valueOf(it) }.getOrNull() } ?: BackgroundBehavior.PAUSE,
                screenshotFormat = pref[Keys.SCREENSHOT_FORMAT] ?: "PNG"
            )
            val updated = update(current)
            pref[Keys.DEFAULT_SPEED] = updated.defaultSpeed
            pref[Keys.AUTO_PLAY_NEXT] = updated.autoPlayNext
            pref[Keys.BG_BEHAVIOR] = updated.backgroundBehavior.name
            pref[Keys.SCREENSHOT_FORMAT] = updated.screenshotFormat
        }
    }
}
