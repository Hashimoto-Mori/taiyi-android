package com.jinggong.timer.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jinggong.timer.MainActivity
import com.jinggong.timer.R
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.timer.TimerState
import com.jinggong.timer.util.TimeFormatters

/** 构造计时/音频前台通知，并创建固定通知渠道。 */
object PracticeNotification {

    const val CHANNEL_ID = "jinggong_practice"
    const val NOTIFICATION_ID = 1201

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        manager.createNotificationChannel(channel)
    }

    fun build(
        context: Context,
        timer: TimerState,
        playback: PlaybackState,
    ): Notification {
        val modeLabel = timer.mode?.let { context.getString(it.labelRes()) }
            ?: context.getString(R.string.app_name)
        val elapsed = timer.elapsedMs(SystemClock.elapsedRealtime())
        val contentIntent = contentIntent(context, timer.isActive)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        when {
            timer.isRunning -> {
                builder.setContentTitle(
                    context.getString(
                        R.string.notification_running,
                        modeLabel,
                        TimeFormatters.formatTimer(elapsed),
                    ),
                )
                builder.setContentText(targetText(context, timer, elapsed))
                builder.addAction(
                    android.R.drawable.ic_media_pause,
                    context.getString(R.string.notification_action_pause),
                    servicePendingIntent(context, ServiceActions.ACTION_PAUSE, 201),
                )
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.notification_action_end),
                    finishPendingIntent(context),
                )
            }

            timer.isPaused -> {
                builder.setContentTitle(
                    context.getString(
                        R.string.notification_paused,
                        modeLabel,
                        TimeFormatters.formatTimer(elapsed),
                    ),
                )
                builder.setContentText(targetText(context, timer, elapsed))
                builder.addAction(
                    android.R.drawable.ic_media_play,
                    context.getString(R.string.notification_action_resume),
                    servicePendingIntent(context, ServiceActions.ACTION_RESUME, 202),
                )
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.notification_action_end),
                    finishPendingIntent(context),
                )
            }

            playback.currentAudio != null -> {
                val audio = playback.currentAudio
                val titleRes = if (playback.isPlaying) {
                    R.string.notification_audio_playing
                } else {
                    R.string.notification_audio_paused
                }
                builder.setContentTitle(context.getString(titleRes, audio.displayName))
                builder.setContentText(context.getString(R.string.app_name))
                builder.addAction(
                    if (playback.isPlaying) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play,
                    context.getString(
                        if (playback.isPlaying) R.string.notification_action_audio_pause
                        else R.string.notification_action_audio_play,
                    ),
                    servicePendingIntent(context, ServiceActions.ACTION_TOGGLE_AUDIO, 203),
                )
            }

            else -> {
                builder.setContentTitle(context.getString(R.string.app_name))
                builder.setContentText(context.getString(R.string.practice_back_hint))
            }
        }
        return builder.build()
    }

    fun notify(context: Context, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun targetText(context: Context, timer: TimerState, elapsedMs: Long): String {
        if (timer.targetMinutes <= 0) return context.getString(R.string.app_name)
        val targetSec = timer.targetMinutes * 60L
        return if (elapsedMs / 1000L >= targetSec) {
            context.getString(R.string.notification_target_reached)
        } else {
            context.getString(R.string.practice_target, timer.targetMinutes)
        }
    }

    private fun contentIntent(context: Context, openPractice: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (openPractice) putExtra(ServiceActions.EXTRA_OPEN_PRACTICE, true)
        }
        return PendingIntent.getActivity(
            context,
            210,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun finishPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(ServiceActions.EXTRA_FINISH_SESSION, true)
            putExtra(ServiceActions.EXTRA_OPEN_PRACTICE, true)
        }
        return PendingIntent.getActivity(
            context,
            211,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun servicePendingIntent(
        context: Context,
        action: String,
        requestCode: Int,
    ): PendingIntent {
        return PendingIntent.getService(
            context,
            requestCode,
            ServiceActions.intent(context, action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
