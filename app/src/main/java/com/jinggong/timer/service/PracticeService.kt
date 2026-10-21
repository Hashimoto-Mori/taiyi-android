package com.jinggong.timer.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import com.jinggong.timer.JingGongApp
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.util.TargetChime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 统一的前台 Service：计时与音频共用同一个常驻通知。
 *
 * Service 不持有自己的计时状态，所有状态来自 [AppContainer] 中的 TimerManager 与
 * PlaybackController；它只负责前台生命周期、通知更新和目标时长提醒。
 */
class PracticeService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var container: AppContainer

    private var foregroundStarted = false
    private var stopJob: Job? = null
    private var targetAlertKey: String? = null

    override fun onCreate() {
        super.onCreate()
        container = (application as JingGongApp).container
        PracticeNotification.createChannel(this)

        scope.launch {
            container.timerManager.state.collect { syncNotification() }
        }
        scope.launch {
            container.playbackController.state.collect { syncNotification() }
        }
        scope.launch {
            while (isActive) {
                delay(1_000L)
                val state = container.timerManager.state.value
                if (state.isRunning) {
                    checkTarget(state)
                    refreshNotification()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForeground()
        when (intent?.action) {
            ServiceActions.ACTION_PAUSE -> scope.launch {
                container.timerManager.pause()
            }

            ServiceActions.ACTION_RESUME -> scope.launch {
                container.timerManager.resume()
            }

            ServiceActions.ACTION_TOGGLE_AUDIO -> {
                val audio = container.playbackController.state.value.currentAudio
                if (audio != null) container.playbackController.toggle(audio)
            }
        }
        syncNotification()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopJob?.cancel()
        scope.cancel()
        container.playbackController.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        super.onDestroy()
    }

    private fun ensureForeground() {
        if (foregroundStarted) return
        val notification = PracticeNotification.build(
            this,
            container.timerManager.state.value,
            container.playbackController.state.value,
        )
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            PracticeNotification.NOTIFICATION_ID,
            notification,
            type,
        )
        foregroundStarted = true
    }

    private fun refreshNotification() {
        if (!foregroundStarted) return
        PracticeNotification.notify(
            this,
            PracticeNotification.build(
                this,
                container.timerManager.state.value,
                container.playbackController.state.value,
            ),
        )
    }

    private fun syncNotification() {
        if (!foregroundStarted) return
        refreshNotification()

        val timer = container.timerManager.state.value
        val playback = container.playbackController.state.value
        val shouldStay = timer.isActive || playback.wantsPlay ||
            playback.isPlaying || playback.isBuffering

        if (shouldStay) {
            stopJob?.cancel()
            stopJob = null
            return
        }
        if (stopJob == null) {
            stopJob = scope.launch {
                delay(1_200L)
                val latestTimer = container.timerManager.state.value
                val latestPlayback = container.playbackController.state.value
                if (!latestTimer.isActive && !latestPlayback.wantsPlay &&
                    !latestPlayback.isPlaying && !latestPlayback.isBuffering
                ) {
                    stopForegroundSafely()
                    stopSelf()
                }
            }
        }
    }

    private fun checkTarget(state: com.jinggong.timer.timer.TimerState) {
        if (state.targetMinutes <= 0) return
        val elapsedSec = state.elapsedMs(SystemClock.elapsedRealtime()) / 1000L
        val targetSec = state.targetMinutes * 60L
        val key = "${state.startWallMs}:${state.targetMinutes}"
        if (elapsedSec >= targetSec && targetAlertKey != key) {
            targetAlertKey = key
            TargetChime.play(this)
            refreshNotification()
        }
    }

    private fun stopForegroundSafely() {
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
    }
}
