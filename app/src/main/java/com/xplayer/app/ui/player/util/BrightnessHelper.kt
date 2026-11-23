package com.xplayer.app.ui.player.util

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager

object BrightnessHelper {
    fun setScreenBrightness(activity: Activity, brightness: Float) {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = brightness.coerceIn(0.01f, 1.0f)
        activity.window.attributes = layoutParams
    }

    fun getScreenBrightness(activity: Activity): Float {
        val current = activity.window.attributes.screenBrightness
        return if (current < 0) 0.5f else current
    }

    fun restoreScreenBrightness(activity: Activity) {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = layoutParams
    }
}

object VolumeHelper {
    fun getVolume(context: Context): Pair<Int, Int> {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return Pair(current, max)
    }

    fun setVolume(context: Context, volume: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, volume.coerceIn(0, max), 0)
    }
}
