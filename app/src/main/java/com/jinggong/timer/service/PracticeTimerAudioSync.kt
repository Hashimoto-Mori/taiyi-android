package com.jinggong.timer.service

import com.jinggong.timer.model.TimerPhase
import com.jinggong.timer.timer.TimerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 让音频跟随练习计时状态：
 * - 计时暂停：音频暂停，并记住暂停前是否正在播放；
 * - 计时继续：如果暂停前音频在播放，则从原位置继续；
 * - 计时结束回到 IDLE：停止音频并归零位置。
 *
 * 计时 IDLE 时仍允许单独播放音频，保持「只播放不计时」的能力。
 */
class PracticeTimerAudioSync(
    private val timerManager: TimerManager,
    private val playbackController: PlaybackController,
    scope: CoroutineScope,
) {
    private var previousPhase = timerManager.state.value.phase
    private var resumeAudioOnResume = false

    init {
        scope.launch {
            timerManager.state.collect { timer ->
                val phase = timer.phase
                when {
                    phase == TimerPhase.PAUSED -> {
                        val playback = playbackController.state.value
                        if (playback.wantsPlay || playback.isPlaying) {
                            resumeAudioOnResume = true
                            playbackController.pause()
                        }
                    }

                    phase == TimerPhase.RUNNING && previousPhase == TimerPhase.PAUSED -> {
                        if (resumeAudioOnResume) {
                            playbackController.state.value.currentAudio?.let {
                                playbackController.play(it)
                            }
                            resumeAudioOnResume = false
                        }
                    }

                    phase == TimerPhase.IDLE && previousPhase != TimerPhase.IDLE -> {
                        resumeAudioOnResume = false
                        playbackController.stop()
                    }
                }
                previousPhase = phase
            }
        }
    }
}
