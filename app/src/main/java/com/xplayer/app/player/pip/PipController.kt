package com.xplayer.app.player.pip

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import com.xplayer.app.R

class PipController(
    private val activity: Activity,
    private val onPlayPause: () -> Unit,
    private val onSeek: (offsetMs: Long) -> Unit
) {

    companion object {
        const val ACTION_PIP_PLAY_PAUSE = "com.xplayer.app.PIP_PLAY_PAUSE"
        const val ACTION_PIP_REWIND = "com.xplayer.app.PIP_REWIND"
        const val ACTION_PIP_FORWARD = "com.xplayer.app.PIP_FORWARD"
    }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PIP_PLAY_PAUSE -> onPlayPause()
                ACTION_PIP_REWIND -> onSeek(-10000L)
                ACTION_PIP_FORWARD -> onSeek(10000L)
            }
        }
    }

    private var isReceiverRegistered = false

    fun register() {
        if (!isReceiverRegistered && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val filter = IntentFilter().apply {
                addAction(ACTION_PIP_PLAY_PAUSE)
                addAction(ACTION_PIP_REWIND)
                addAction(ACTION_PIP_FORWARD)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(pipReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                activity.registerReceiver(pipReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    fun unregister() {
        if (isReceiverRegistered) {
            runCatching { activity.unregisterReceiver(pipReceiver) }
            isReceiverRegistered = false
        }
    }

    fun enterPip(videoWidth: Int, videoHeight: Int, isPlaying: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = buildPipParams(videoWidth, videoHeight, isPlaying)
            activity.enterPictureInPictureMode(params)
        }
    }

    fun updatePipParams(videoWidth: Int, videoHeight: Int, isPlaying: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity.isInPictureInPictureMode) {
            val params = buildPipParams(videoWidth, videoHeight, isPlaying)
            activity.setPictureInPictureParams(params)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(width: Int, height: Int, isPlaying: Boolean): PictureInPictureParams {
        val aspect = if (width > 0 && height > 0) {
            val ratio = width.toFloat() / height.toFloat()
            if (ratio in 0.42f..2.38f) {
                Rational(width, height)
            } else {
                Rational(16, 9)
            }
        } else {
            Rational(16, 9)
        }

        val actions = mutableListOf<RemoteAction>()

        // 1. Rewind 10s
        val rewindIntent = PendingIntent.getBroadcast(
            activity,
            1,
            Intent(ACTION_PIP_REWIND).setPackage(activity.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        actions.add(
            RemoteAction(
                Icon.createWithResource(activity, android.R.drawable.ic_media_rew),
                "-10s",
                "-10s",
                rewindIntent
            )
        )

        // 2. Play/Pause
        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseIntent = PendingIntent.getBroadcast(
            activity,
            2,
            Intent(ACTION_PIP_PLAY_PAUSE).setPackage(activity.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        actions.add(
            RemoteAction(
                Icon.createWithResource(activity, playPauseIcon),
                "Play/Pause",
                "Play/Pause",
                playPauseIntent
            )
        )

        // 3. Forward 10s
        val forwardIntent = PendingIntent.getBroadcast(
            activity,
            3,
            Intent(ACTION_PIP_FORWARD).setPackage(activity.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        actions.add(
            RemoteAction(
                Icon.createWithResource(activity, android.R.drawable.ic_media_ff),
                "+10s",
                "+10s",
                forwardIntent
            )
        )

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspect)
            .setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
            builder.setSeamlessResizeEnabled(true)
        }

        return builder.build()
    }
}
