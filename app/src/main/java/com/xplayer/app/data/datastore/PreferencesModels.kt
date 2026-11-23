package com.xplayer.app.data.datastore

enum class DarkModeTheme {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

enum class EnginePolicy {
    MEDIA3_FIRST,
    FORCE_MEDIA3,
    FORCE_MPV
}

enum class MpvHwdecMode(val value: String) {
    MEDIACODEC("mediacodec"),
    MEDIACODEC_COPY("mediacodec-copy"),
    NO("no")
}

enum class GestureAction {
    BRIGHTNESS,
    VOLUME,
    NONE
}

enum class DoubleTapAction {
    SEEK_BACKWARD,
    SEEK_FORWARD,
    PLAY_PAUSE,
    PREV_VIDEO,
    NEXT_VIDEO,
    NONE
}

enum class CenterTapAction {
    PLAY_PAUSE,
    TOGGLE_ASPECT,
    MUTE,
    SCREENSHOT,
    NONE
}

enum class BackgroundBehavior {
    PAUSE,
    PIP,
    BACKGROUND_AUDIO
}

data class AppearancePrefs(
    val dynamicColor: Boolean = true,
    val seedColorIndex: Int = 0,
    val darkMode: DarkModeTheme = DarkModeTheme.SYSTEM
)

data class EnginePrefs(
    val enginePolicy: EnginePolicy = EnginePolicy.MEDIA3_FIRST,
    val autoSwitchAss: Boolean = true,
    val frameRateMatching: Boolean = true,
    val mpvHwdec: MpvHwdecMode = MpvHwdecMode.MEDIACODEC,
    val customMpvConfEnabled: Boolean = false,
    val customMpvConf: String = "# mpv.conf\nvo=gpu-next\ngpu-context=android\nhwdec=mediacodec\n"
)

data class GesturePrefs(
    val leftVerticalAction: GestureAction = GestureAction.BRIGHTNESS,
    val rightVerticalAction: GestureAction = GestureAction.VOLUME,
    val verticalSensitivity: Float = 1.0f,
    val volumeBoostEnabled: Boolean = true,
    val horizontalSeekEnabled: Boolean = true,
    val seekSpanSeconds: Int = 60,
    val seekExact: Boolean = false,
    val doubleTapLeftAction: DoubleTapAction = DoubleTapAction.SEEK_BACKWARD,
    val doubleTapRightAction: DoubleTapAction = DoubleTapAction.SEEK_FORWARD,
    val doubleTapCenterAction: CenterTapAction = CenterTapAction.PLAY_PAUSE,
    val doubleTapStepSeconds: Int = 10,
    val longPressSpeedEnabled: Boolean = true,
    val longPressSpeed: Float = 2.0f,
    val longPressSlideSpeedEnabled: Boolean = true,
    val longPressDelayMs: Long = 300L,
    val pinchZoomEnabled: Boolean = true,
    val edgeDeadzoneDp: Int = 24
)

data class SubtitlePrefs(
    val fontSizeScale: Float = 1.0f,
    val fontColor: Int = 0xFFFFFFFF.toInt(),
    val outlineWidth: Float = 2.0f,
    val shadowEnabled: Boolean = true,
    val backgroundEnabled: Boolean = false,
    val bottomPaddingDp: Int = 24,
    val overrideAssStyle: Boolean = false
)

data class PlaybackPrefs(
    val defaultSpeed: Float = 1.0f,
    val autoPlayNext: Boolean = true,
    val backgroundBehavior: BackgroundBehavior = BackgroundBehavior.PAUSE,
    val screenshotFormat: String = "PNG"
)
