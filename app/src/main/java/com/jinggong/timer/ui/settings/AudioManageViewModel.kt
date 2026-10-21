package com.jinggong.timer.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jinggong.timer.data.db.AudioEntity
import com.jinggong.timer.data.prefs.SettingsDataStore
import com.jinggong.timer.data.repo.AudioRepository
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.AudioScope
import com.jinggong.timer.service.PlaybackController
import com.jinggong.timer.service.PlaybackState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AudioManageUiState(
    val audio: List<AudioEntity> = emptyList(),
    val defaultAudioId: Long? = null,
    val importing: Boolean = false,
    val previewId: Long? = null,
    val playback: PlaybackState = PlaybackState(),
)

sealed interface AudioManageEvent {
    data object Imported : AudioManageEvent
    data object ImportFailed : AudioManageEvent
    data object Deleted : AudioManageEvent
    data object Renamed : AudioManageEvent
    data object NameRequired : AudioManageEvent
}

class AudioManageViewModel(
    private val audioRepository: AudioRepository,
    private val settingsDataStore: SettingsDataStore,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _importing = MutableStateFlow(false)
    private val _previewId = MutableStateFlow<Long?>(null)
    private val _events = MutableSharedFlow<AudioManageEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    val uiState = combine(
        audioRepository.observeAll(),
        settingsDataStore.settings,
        playbackController.state,
        _importing,
        _previewId,
    ) { audio, settings, playback, importing, previewId ->
        AudioManageUiState(
            audio = audio,
            defaultAudioId = settings.defaultAudioId,
            importing = importing,
            previewId = previewId,
            playback = playback,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AudioManageUiState(),
    )

    fun importAudio(uri: Uri, displayName: String, scope: AudioScope) {
        viewModelScope.launch {
            _importing.value = true
            audioRepository.import(uri, displayName, scope)
                .onSuccess { _events.emit(AudioManageEvent.Imported) }
                .onFailure { _events.emit(AudioManageEvent.ImportFailed) }
            _importing.value = false
        }
    }

    fun renameAudio(audio: AudioEntity, displayName: String) {
        if (displayName.isBlank()) {
            viewModelScope.launch { _events.emit(AudioManageEvent.NameRequired) }
            return
        }
        viewModelScope.launch {
            audioRepository.rename(audio, displayName)
            _events.emit(AudioManageEvent.Renamed)
        }
    }

    fun deleteAudio(audio: AudioEntity) {
        viewModelScope.launch {
            audioRepository.delete(audio)
            if (settingsDataStore.current().defaultAudioId == audio.id) {
                settingsDataStore.setDefaultAudio(null)
            }
            playbackController.clearCurrent(audio.id)
            if (_previewId.value == audio.id) _previewId.value = null
            _events.emit(AudioManageEvent.Deleted)
        }
    }

    fun togglePreview(audio: AudioEntity) {
        if (_previewId.value == audio.id && playbackController.state.value.isPlaying) {
            playbackController.pause()
            _previewId.value = null
        } else {
            playbackController.play(audio)
            _previewId.value = audio.id
        }
    }

    fun stopPreview() {
        if (_previewId.value != null) {
            playbackController.pause()
            _previewId.value = null
        }
    }

    override fun onCleared() {
        stopPreview()
        super.onCleared()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AudioManageViewModel(
                    audioRepository = container.audioRepository,
                    settingsDataStore = container.settingsDataStore,
                    playbackController = container.playbackController,
                )
            }
        }
    }
}
