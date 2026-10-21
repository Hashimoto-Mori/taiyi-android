package com.jinggong.timer.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jinggong.timer.data.db.AudioEntity
import com.jinggong.timer.data.prefs.Settings
import com.jinggong.timer.data.prefs.SettingsDataStore
import com.jinggong.timer.data.repo.AudioRepository
import com.jinggong.timer.data.repo.SessionRepository
import com.jinggong.timer.di.AppContainer
import com.jinggong.timer.model.PracticeMode
import com.jinggong.timer.model.ThemeMode
import com.jinggong.timer.timer.TimerManager
import com.jinggong.timer.util.CsvExporter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: Settings = Settings(),
    val audio: List<AudioEntity> = emptyList(),
)

sealed interface SettingsEvent {
    data object Exported : SettingsEvent
    data object ExportFailed : SettingsEvent
    data object NoData : SettingsEvent
    data object Cleared : SettingsEvent
    data object ClearBlocked : SettingsEvent
    data object ClearFailed : SettingsEvent
    data object Saved : SettingsEvent
}

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    audioRepository: AudioRepository,
    private val sessionRepository: SessionRepository,
    private val csvExporter: CsvExporter,
    private val timerManager: TimerManager,
) : ViewModel() {

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    val uiState = combine(
        settingsDataStore.settings,
        audioRepository.observeAll(),
    ) { settings, audio ->
        SettingsUiState(settings = settings, audio = audio)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SettingsUiState(),
    )

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch { settingsDataStore.setDailyGoal(minutes) }
    }

    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAutoPlay(enabled) }
    }

    fun setDefaultAudio(id: Long?) {
        viewModelScope.launch { settingsDataStore.setDefaultAudio(id) }
    }

    fun setLoop(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setLoopAudio(enabled) }
    }

    fun setTarget(mode: PracticeMode, minutes: Int) {
        viewModelScope.launch { settingsDataStore.setTargetMinutes(mode, minutes) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            val sessions = sessionRepository.getAllOnce()
            if (sessions.isEmpty()) {
                _events.emit(SettingsEvent.NoData)
                return@launch
            }
            csvExporter.export(uri)
                .onSuccess { _events.emit(SettingsEvent.Exported) }
                .onFailure { _events.emit(SettingsEvent.ExportFailed) }
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            if (timerManager.state.value.isActive) {
                _events.emit(SettingsEvent.ClearBlocked)
                return@launch
            }
            runCatching { sessionRepository.clearAll() }
                .onSuccess { _events.emit(SettingsEvent.Cleared) }
                .onFailure { _events.emit(SettingsEvent.ClearFailed) }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsDataStore = container.settingsDataStore,
                    audioRepository = container.audioRepository,
                    sessionRepository = container.sessionRepository,
                    csvExporter = container.csvExporter,
                    timerManager = container.timerManager,
                )
            }
        }
    }
}
