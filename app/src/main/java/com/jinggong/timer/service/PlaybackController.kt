package com.jinggong.timer.service

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jinggong.timer.data.db.AudioEntity
import com.jinggong.timer.data.prefs.SettingsDataStore
import com.jinggong.timer.data.repo.AudioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 音频播放的 UI 状态。 */
data class PlaybackState(
    val currentAudio: AudioEntity? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val loop: Boolean = true,
    val hasError: Boolean = false,
    /** 用户希望播放（用于前台 Service 保活，暂停后为 false）。 */
    val wantsPlay: Boolean = false,
)

/**
 * 应用级 ExoPlayer 封装。
 *
 * 播放器使用 application context，不持有任何 Activity；前台 Service 负责在后台保持
 * 进程存活，并在 Service 停止时调用 [release] 释放播放器。
 */
class PlaybackController(
    private val context: Context,
    private val audioRepository: AudioRepository,
    private val settingsDataStore: SettingsDataStore,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null

    init {
        scope.launch {
            settingsDataStore.settings.collectLatest { settings ->
                onMain {
                    _state.update { it.copy(loop = settings.loopAudio) }
                    player?.repeatMode = if (settings.loopAudio) {
                        Player.REPEAT_MODE_ONE
                    } else {
                        Player.REPEAT_MODE_OFF
                    }
                }
            }
        }
    }

    /** 播放或切换到指定音频。文件缺失时只更新错误状态。 */
    fun play(audio: AudioEntity) {
        onMain {
            if (!audioRepository.fileExists(audio)) {
                _state.update {
                    it.copy(isPlaying = false, hasError = true, wantsPlay = false)
                }
                return@onMain
            }
            val current = _state.value
            val exo = ensurePlayer()
            val same = current.currentAudio?.id == audio.id && exo.mediaItemCount > 0
            if (!same) {
                exo.setMediaItem(MediaItem.fromUri(Uri.fromFile(audioRepository.fileFor(audio))))
                exo.prepare()
            } else if (exo.playbackState == Player.STATE_ENDED) {
                exo.seekTo(0L)
            }
            _state.update {
                it.copy(currentAudio = audio, hasError = false, wantsPlay = true)
            }
            exo.playWhenReady = true
        }
    }

    fun pause() {
        _state.update { it.copy(wantsPlay = false) }
        onMain {
            player?.pause()
        }
    }

    /** 计时结束/放弃时真正停止音频，并把播放位置归零。 */
    fun stop() {
        _state.update {
            it.copy(wantsPlay = false, isPlaying = false, isBuffering = false)
        }
        onMain {
            player?.pause()
            player?.seekTo(0L)
        }
    }

    fun toggle(audio: AudioEntity) {
        onMain {
            val current = _state.value
            val exo = player
            if (current.currentAudio?.id == audio.id && exo?.isPlaying == true) {
                exo.pause()
            } else {
                play(audio)
            }
        }
    }

    fun setLoop(enabled: Boolean) {
        _state.update { it.copy(loop = enabled) }
        onMain {
            player?.repeatMode = if (enabled) {
                Player.REPEAT_MODE_ONE
            } else {
                Player.REPEAT_MODE_OFF
            }
        }
        scope.launch { settingsDataStore.setLoopAudio(enabled) }
    }

    /** 只切换当前音频，不自动播放（用于暂停状态下改选音频）。 */
    fun select(audio: AudioEntity) {
        onMain {
            if (_state.value.currentAudio?.id != audio.id) {
                player?.stop()
                player?.clearMediaItems()
                _state.update {
                    it.copy(
                        currentAudio = audio,
                        isPlaying = false,
                        isBuffering = false,
                        wantsPlay = false,
                        hasError = false,
                    )
                }
            }
        }
    }

    /** 清空当前音频选择并停止播放。 */
    fun clear() {
        _state.update {
            it.copy(
                currentAudio = null,
                isPlaying = false,
                isBuffering = false,
                wantsPlay = false,
            )
        }
        onMain {
            player?.stop()
            player?.clearMediaItems()
        }
    }

    /** 删除音频后清除当前选择。 */
    fun clearCurrent(audioId: Long) {
        onMain {
            if (_state.value.currentAudio?.id == audioId) {
                player?.stop()
                player?.clearMediaItems()
                _state.update {
                    it.copy(
                        currentAudio = null,
                        isPlaying = false,
                        isBuffering = false,
                        wantsPlay = false,
                    )
                }
            }
        }
    }

    /** Service 不再需要保活时释放播放器。 */
    fun release() {
        onMain {
            player?.release()
            player = null
            _state.update {
                it.copy(isPlaying = false, isBuffering = false, wantsPlay = false)
            }
        }
    }

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        val exo = ExoPlayer.Builder(context.applicationContext).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
            repeatMode = if (_state.value.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            val owner = this
            addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _state.update {
                            it.copy(
                                isPlaying = isPlaying,
                                isBuffering = false,
                                hasError = false,
                            )
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _state.update {
                            it.copy(
                                isBuffering = playbackState == Player.STATE_BUFFERING,
                                isPlaying = owner.isPlaying && playbackState != Player.STATE_ENDED,
                                wantsPlay = if (playbackState == Player.STATE_ENDED) {
                                    false
                                } else {
                                    it.wantsPlay
                                },
                            )
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        _state.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                hasError = true,
                                wantsPlay = false,
                            )
                        }
                    }
                },
            )
        }
        player = exo
        return exo
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
