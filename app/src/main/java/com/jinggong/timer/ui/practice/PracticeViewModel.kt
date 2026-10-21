package com.jinggong.timer.ui.practice

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jinggong.timer.data.db.AudioEntity
import com.jinggong.timer.data.db.SessionEntity
import com.jinggong.timer.data.prefs.Settings
import com.jinggong.timer.data.prefs.SettingsDataStore
import com.jinggong.timer.data.repo.AudioRepository
import com.jinggong.timer.data.repo.SessionRepository
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.service.PlaybackController
import com.jinggong.timer.service.PlaybackState
import com.jinggong.timer.service.ServiceActions
import com.jinggong.timer.timer.TimerManager
import com.jinggong.timer.timer.TimerResult
import com.jinggong.timer.timer.TimerState
import com.jinggong.timer.ui.nav.NavigationEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PracticeUiState(
    val mode: PracticeMode,
    val timer: TimerState = TimerState(),
    val targetMinutes: Int = 0,
    val audioOptions: List<AudioEntity> = emptyList(),
    val selectedAudio: AudioEntity? = null,
    val playback: PlaybackState = PlaybackState(),
    val autoPlay: Boolean = false,
    val summary: TimerResult? = null,
    val isOtherModeActive: Boolean = false,
)

sealed interface PracticeEvent {
    data object Saved : PracticeEvent
    data object Discarded : PracticeEvent
    data object SaveFailed : PracticeEvent
}

private data class PracticeBase(
    val timer: TimerState,
    val settings: Settings,
    val audioOptions: List<AudioEntity>,
    val playback: PlaybackState,
)

class PracticeViewModel(
    private val mode: PracticeMode,
    @SuppressLint("StaticFieldLeak") private val context: Context,
    private val sessionRepository: SessionRepository,
    private val audioRepository: AudioRepository,
    private val settingsDataStore: SettingsDataStore,
    private val timerManager: TimerManager,
    private val playbackController: PlaybackController,
    private val navigationEvents: NavigationEvents,
) : ViewModel() {

    private val selectedAudioId = MutableStateFlow<Long?>(null)
    private val summary = MutableStateFlow<TimerResult?>(null)
    private var playedAudioTitle: String? = null

    private val _events = MutableSharedFlow<PracticeEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    private val base = combine(
        timerManager.state,
        settingsDataStore.settings,
        audioRepository.observeForMode(mode),
        playbackController.state,
    ) { timer, settings, audios, playback ->
        PracticeBase(timer, settings, audios, playback)
    }

    val uiState = combine(base, selectedAudioId, summary) { current, explicitId, pendingSummary ->
        val selected = resolveSelectedAudio(current, explicitId)
        PracticeUiState(
            mode = mode,
            timer = current.timer,
            targetMinutes = if (current.timer.isActive && current.timer.mode == mode) {
                current.timer.targetMinutes
            } else {
                current.settings.targetMinutesFor(mode)
            },
            audioOptions = current.audioOptions,
            selectedAudio = selected,
            playback = current.playback,
            autoPlay = current.settings.autoPlayAudio,
            summary = pendingSummary,
            isOtherModeActive = current.timer.isActive && current.timer.mode != mode,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = PracticeUiState(mode = mode),
    )

    init {
        viewModelScope.launch {
            navigationEvents.pendingFinish.collect { pending ->
                if (!pending) return@collect
                val timer = timerManager.state.value
                if (timer.isActive && timer.mode == mode) {
                    playbackController.stop()
                    val result = timerManager.finish()
                    if (result != null) summary.value = result
                    navigationEvents.consumePendingFinish()
                } else if (!timer.isActive) {
                    navigationEvents.consumePendingFinish()
                }
            }
        }
    }

    fun start() {
        if (!uiState.value.timer.ready || uiState.value.isOtherModeActive) return
        viewModelScope.launch {
            val settings = settingsDataStore.current()
            playedAudioTitle = null
            timerManager.start(mode, settings.targetMinutesFor(mode))
            ServiceActions.start(context)
            val currentPlayback = playbackController.state.value
            if (currentPlayback.isPlaying && currentPlayback.currentAudio != null) {
                playedAudioTitle = currentPlayback.currentAudio.displayName
            }
            if (settings.autoPlayAudio && timerManager.state.value.isRunning) {
                val audio = resolveAudioForStart(settings)
                if (audio != null) {
                    playedAudioTitle = audio.displayName
                    playbackController.play(audio)
                    ServiceActions.start(context)
                }
            }
        }
    }

    /** 开始计时时优先使用当前选择，其次默认音频，最后当前模式下的第一个音频。 */
    private suspend fun resolveAudioForStart(settings: Settings): AudioEntity? {
        if (selectedAudioId.value == 0L) return null
        val options = audioRepository.getForModeOnce(mode)
        val explicit = selectedAudioId.value
        return if (explicit != null) {
            options.firstOrNull { it.id == explicit }
        } else {
            options.firstOrNull { it.id == settings.defaultAudioId } ?: options.firstOrNull()
        }
    }

    fun pause() {
        viewModelScope.launch { timerManager.pause() }
    }

    fun resume() {
        viewModelScope.launch { timerManager.resume() }
    }

    fun end() {
        viewModelScope.launch {
            playbackController.stop()
            val result = timerManager.finish()
            if (result != null) summary.value = result
        }
    }

    fun save(note: String) {
        val result = summary.value ?: return
        viewModelScope.launch {
            if (result.durationSec <= 0) {
                summary.value = null
                playedAudioTitle = null
                timerManager.reset()
                _events.emit(PracticeEvent.Discarded)
                return@launch
            }
            val audioTitle = playedAudioTitle
            val session = SessionEntity(
                mode = result.mode.dbValue,
                startTime = result.startWallMs,
                endTime = result.endWallMs,
                durationSec = result.durationSec,
                audioTitle = audioTitle,
                note = note.trim().takeIf { it.isNotEmpty() },
            )
            runCatching { sessionRepository.insert(session) }
                .onSuccess {
                    summary.value = null
                    playedAudioTitle = null
                    timerManager.reset()
                    _events.emit(PracticeEvent.Saved)
                }
                .onFailure {
                    _events.emit(PracticeEvent.SaveFailed)
                }
        }
    }

    fun discard() {
        viewModelScope.launch {
            summary.value = null
            playedAudioTitle = null
            timerManager.reset()
            _events.emit(PracticeEvent.Discarded)
        }
    }

    fun selectAudio(audioId: Long) {
        selectedAudioId.value = if (audioId <= 0L) 0L else audioId
        if (audioId <= 0L) {
            playbackController.clear()
            return
        }
        val audio = uiState.value.audioOptions.firstOrNull { it.id == audioId } ?: return
        val wasPlaying = playbackController.state.value.wantsPlay ||
            playbackController.state.value.isPlaying
        if (wasPlaying) {
            playedAudioTitle = audio.displayName
            playbackController.play(audio)
            ServiceActions.start(context)
        } else {
            playedAudioTitle = audio.displayName
            playbackController.select(audio)
        }
    }

    fun toggleAudio() {
        val audio = uiState.value.selectedAudio ?: return
        val playback = playbackController.state.value
        if (playback.currentAudio?.id == audio.id && playback.isPlaying) {
            playbackController.pause()
        } else {
            playedAudioTitle = audio.displayName
            playbackController.play(audio)
            ServiceActions.start(context)
        }
    }

    fun toggleLoop() {
        playbackController.setLoop(!playbackController.state.value.loop)
    }

    fun setTarget(minutes: Int) {
        viewModelScope.launch {
            val safe = minutes.coerceIn(0, 240)
            if (uiState.value.timer.isActive && uiState.value.timer.mode == mode) {
                timerManager.updateTarget(safe)
            } else {
                settingsDataStore.setTargetMinutes(mode, safe)
            }
        }
    }

    private fun resolveSelectedAudio(base: PracticeBase, explicitId: Long?): AudioEntity? {
        if (explicitId == 0L) return null
        if (explicitId != null) {
            base.audioOptions.firstOrNull { it.id == explicitId }?.let { return it }
        }
        val defaultId = base.settings.defaultAudioId
        return base.audioOptions.firstOrNull { it.id == defaultId }
            ?: base.audioOptions.firstOrNull()
    }

    companion object {
        fun factory(container: AppContainer, mode: PracticeMode): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    PracticeViewModel(
                        mode = mode,
                        context = container.appContext,
                        sessionRepository = container.sessionRepository,
                        audioRepository = container.audioRepository,
                        settingsDataStore = container.settingsDataStore,
                        timerManager = container.timerManager,
                        playbackController = container.playbackController,
                        navigationEvents = container.navigationEvents,
                    )
                }
            }
    }
}
